package com.example.weather.feature.weather.data.repository

import com.example.weather.feature.weather.data.cache.WeatherCache
import com.example.weather.feature.weather.data.remote.AmapCurrentWeatherDto
import com.example.weather.feature.weather.data.remote.AmapDailyForecastDto
import com.example.weather.feature.weather.data.remote.AmapForecastDto
import com.example.weather.feature.weather.data.remote.WeatherRemoteDataSource
import com.example.weather.feature.weather.domain.error.WeatherFailure
import com.example.weather.feature.weather.domain.model.CurrentWeather
import com.example.weather.feature.weather.domain.model.DailyForecast
import com.example.weather.feature.weather.domain.model.Weather
import com.example.weather.feature.weather.domain.model.WeatherDataSource
import com.example.weather.feature.weather.domain.model.WeatherForecast
import com.example.weather.feature.weather.domain.model.WeatherResult
import com.example.weather.feature.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 协调远程数据，将供应商 DTO 转换为领域模型，并在「城市 + 当天」粒度上做本地缓存，
 * 保证同一天内对同一地点只请求一次远程接口。
 */
internal class DefaultWeatherRepository(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val cache: WeatherCache,
    private val dateProvider: () -> String,
) : WeatherRepository {

    override suspend fun getWeather(city: String, forceRefresh: Boolean): WeatherResult {
        val dateKey = dateProvider()

        if (!forceRefresh) {
            cache.get(city, dateKey)?.let { cached ->
                return WeatherResult(
                    weather = cached,
                    source = WeatherDataSource.TODAY_CACHE,
                    cacheDateKey = dateKey,
                )
            }
        }

        return try {
            val weather = fetchRemoteWeather(city)
            cache.put(city, dateKey, weather)
            WeatherResult(weather = weather, source = WeatherDataSource.NETWORK)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (remoteFailure: WeatherFailure) {
            if (remoteFailure !is WeatherFailure.Network && remoteFailure !is WeatherFailure.Http) {
                throw remoteFailure
            }
            val fallback = cache.getLatest(city) ?: throw remoteFailure
            WeatherResult(
                weather = fallback.weather,
                source = WeatherDataSource.STALE_CACHE,
                cacheDateKey = fallback.dateKey,
            )
        }
    }

    private suspend fun fetchRemoteWeather(city: String): Weather = coroutineScope {
        val current = async { remoteDataSource.getCurrentWeather(city) }
        val forecast = async { remoteDataSource.getForecast(city) }

        Weather(
            current = current.await().toDomain(),
            forecast = forecast.await().toDomain(),
        )
    }
}

private fun AmapCurrentWeatherDto.toDomain() = CurrentWeather(
    province = province.required("current.province"),
    city = city.required("current.city"),
    condition = weather.required("current.weather"),
    temperatureCelsius = temperature.required("current.temperature"),
    humidityPercent = humidity.required("current.humidity"),
    windDirection = windDirection,
    windPower = windPower,
    observedAt = reportTime.required("current.reporttime"),
)

private fun AmapForecastDto.toDomain(): WeatherForecast {
    if (days.isEmpty()) throw invalidRemoteData("forecast.casts")

    return WeatherForecast(
        province = province.required("forecast.province"),
        city = city.required("forecast.city"),
        reportedAt = reportTime.required("forecast.reporttime"),
        days = days.map(AmapDailyForecastDto::toDomain),
    )
}

private fun AmapDailyForecastDto.toDomain() = DailyForecast(
    date = date.required("forecast.date"),
    week = week,
    dayCondition = dayCondition.required("forecast.dayweather"),
    nightCondition = nightCondition.required("forecast.nightweather"),
    dayTemperatureCelsius = dayTemperature.required("forecast.daytemp"),
    nightTemperatureCelsius = nightTemperature.required("forecast.nighttemp"),
    dayWindDirection = dayWindDirection,
    nightWindDirection = nightWindDirection,
    dayWindPower = dayWindPower,
    nightWindPower = nightWindPower,
)

private fun String.required(fieldName: String): String =
    takeIf(String::isNotBlank) ?: throw invalidRemoteData(fieldName)

private fun invalidRemoteData(fieldName: String) =
    com.example.weather.feature.weather.domain.error.WeatherFailure.InvalidResponse(
        IllegalArgumentException("Missing required remote field: $fieldName"),
    )
