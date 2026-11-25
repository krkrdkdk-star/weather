package com.example.weather.feature.weather.data.remote

import com.example.weather.feature.weather.domain.error.WeatherFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal interface WeatherRemoteDataSource {
    suspend fun getCurrentWeather(city: String): AmapCurrentWeatherDto
    suspend fun getForecast(city: String): AmapForecastDto
}

/** 高德天气协议的唯一实现边界。 */
internal class AmapWeatherRemoteDataSource(
    private val apiKey: String,
    private val httpClient: HttpClient,
) : WeatherRemoteDataSource {

    init {
        require(apiKey.isNotBlank()) {
            "Missing weather API key. Configure it in the app before loading weather."
        }
    }

    override suspend fun getCurrentWeather(city: String): AmapCurrentWeatherDto =
        requestWeather(city, WeatherQuery.CURRENT).lives.firstOrNull()
            ?: throw WeatherFailure.MissingData("current weather")

    override suspend fun getForecast(city: String): AmapForecastDto =
        requestWeather(city, WeatherQuery.FORECAST).forecasts.firstOrNull()
            ?: throw WeatherFailure.MissingData("forecast")

    private suspend fun requestWeather(
        city: String,
        query: WeatherQuery,
    ): AmapWeatherResponseDto {
        val normalizedCity = city.trim()
        if (normalizedCity.isEmpty()) throw WeatherFailure.InvalidCity

        val response = executeRequest(normalizedCity, query)
        if (response.status != SUCCESS_STATUS || response.infocode != SUCCESS_INFO_CODE) {
            throw WeatherFailure.ServiceRejected(
                code = response.infocode,
                serviceMessage = response.info.ifBlank { "Weather service rejected the request" },
            )
        }
        return response
    }

    private suspend fun executeRequest(
        city: String,
        query: WeatherQuery,
    ): AmapWeatherResponseDto = try {
        httpClient.get(WEATHER_ENDPOINT) {
            parameter("key", apiKey)
            parameter("city", city)
            parameter("extensions", query.extension)
            parameter("output", "JSON")
        }.body()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (httpError: ResponseException) {
        throw WeatherFailure.Http(httpError.response.status.value, httpError)
    } catch (conversionError: JsonConvertException) {
        throw WeatherFailure.InvalidResponse(conversionError)
    } catch (serializationError: SerializationException) {
        throw WeatherFailure.InvalidResponse(serializationError)
    } catch (transformationError: NoTransformationFoundException) {
        throw WeatherFailure.InvalidResponse(transformationError)
    } catch (knownFailure: WeatherFailure) {
        throw knownFailure
    } catch (networkError: Exception) {
        throw WeatherFailure.Network(networkError)
    }

    private enum class WeatherQuery(val extension: String) {
        CURRENT("base"),
        FORECAST("all"),
    }

    private companion object {
        const val WEATHER_ENDPOINT = "https://restapi.amap.com/v3/weather/weatherInfo"
        const val SUCCESS_STATUS = "1"
        const val SUCCESS_INFO_CODE = "10000"
    }
}
