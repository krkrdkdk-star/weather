package com.example.weather.feature.weather.data.repository

import com.example.weather.feature.weather.data.cache.WeatherCache
import com.example.weather.feature.weather.data.remote.AmapCurrentWeatherDto
import com.example.weather.feature.weather.data.remote.AmapDailyForecastDto
import com.example.weather.feature.weather.data.remote.AmapForecastDto
import com.example.weather.feature.weather.data.remote.WeatherRemoteDataSource
import com.example.weather.feature.weather.domain.error.WeatherFailure
import com.example.weather.feature.weather.domain.model.WeatherDataSource
import com.example.weather.testutil.InMemoryKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultWeatherRepositoryTest {

    @Test
    fun mapsRemoteDtosToDomainModel() = runTest {
        val result = repository(FakeWeatherRemoteDataSource()).getWeather("110101")

        assertEquals("东城区", result.weather.current.city)
        assertEquals("雾", result.weather.current.condition)
        assertEquals("晴", result.weather.forecast.days.single().dayCondition)
        assertEquals(WeatherDataSource.NETWORK, result.source)
    }

    @Test
    fun rejectsIncompleteRemoteDataWithoutFallback() = runTest {
        val source = object : WeatherRemoteDataSource {
            override suspend fun getCurrentWeather(city: String) = AmapCurrentWeatherDto()
            override suspend fun getForecast(city: String) = FakeWeatherRemoteDataSource().getForecast(city)
        }

        assertFailsWith<WeatherFailure.InvalidResponse> {
            repository(source).getWeather("110101")
        }
    }

    @Test
    fun secondCallOnSameDayServesTodayCacheWithoutRemote() = runTest {
        val source = ControllableWeatherRemoteDataSource()
        val repository = repository(source, dateProvider = { "20260101" })

        val first = repository.getWeather("110101")
        val second = repository.getWeather("110101")

        assertEquals(WeatherDataSource.NETWORK, first.source)
        assertEquals(WeatherDataSource.TODAY_CACHE, second.source)
        assertEquals("20260101", second.cacheDateKey)
        assertEquals(2, source.calls)
    }

    @Test
    fun forceRefreshBypassesTodayCacheAndConsumesRemoteCalls() = runTest {
        val source = ControllableWeatherRemoteDataSource()
        val repository = repository(source)

        repository.getWeather("110101")
        val refreshed = repository.getWeather("110101", forceRefresh = true)

        assertEquals(WeatherDataSource.NETWORK, refreshed.source)
        assertEquals(4, source.calls)
    }

    @Test
    fun newDayNetworkFailureFallsBackToLatestSuccessfulCache() = runTest {
        var currentDay = "20260101"
        val source = ControllableWeatherRemoteDataSource()
        val repository = repository(source, dateProvider = { currentDay })
        val original = repository.getWeather("110101")

        currentDay = "20260102"
        source.fail = true
        val fallback = repository.getWeather("110101")

        assertEquals(WeatherDataSource.STALE_CACHE, fallback.source)
        assertEquals("20260101", fallback.cacheDateKey)
        assertEquals(original.weather, fallback.weather)
    }

    @Test
    fun forcedRefreshFailureAlsoFallsBackToCache() = runTest {
        val source = ControllableWeatherRemoteDataSource()
        val repository = repository(source)
        repository.getWeather("110101")
        source.fail = true

        val fallback = repository.getWeather("110101", forceRefresh = true)

        assertEquals(WeatherDataSource.STALE_CACHE, fallback.source)
        assertEquals("20260101", fallback.cacheDateKey)
    }

    private fun repository(
        source: WeatherRemoteDataSource,
        dateProvider: () -> String = { "20260101" },
    ) = DefaultWeatherRepository(
        remoteDataSource = source,
        cache = WeatherCache(InMemoryKeyValueStore()),
        dateProvider = dateProvider,
    )

    private class ControllableWeatherRemoteDataSource : WeatherRemoteDataSource {
        var calls = 0
        var fail = false

        override suspend fun getCurrentWeather(city: String): AmapCurrentWeatherDto {
            calls++
            if (fail) throw WeatherFailure.Network(IllegalStateException("offline"))
            return FakeWeatherRemoteDataSource().getCurrentWeather(city)
        }

        override suspend fun getForecast(city: String): AmapForecastDto {
            calls++
            if (fail) throw WeatherFailure.Network(IllegalStateException("offline"))
            return FakeWeatherRemoteDataSource().getForecast(city)
        }
    }

    private class FakeWeatherRemoteDataSource : WeatherRemoteDataSource {
        override suspend fun getCurrentWeather(city: String) = AmapCurrentWeatherDto(
            province = "北京", city = "东城区", weather = "雾", temperature = "4",
            windDirection = "西南", windPower = "≤3", humidity = "80",
            reportTime = "2026-01-01 08:00:00",
        )

        override suspend fun getForecast(city: String) = AmapForecastDto(
            province = "北京", city = "东城区", reportTime = "2026-01-01 08:00:00",
            days = listOf(
                AmapDailyForecastDto(
                    date = "2026-01-01", week = "4", dayCondition = "晴",
                    nightCondition = "晴", dayTemperature = "8", nightTemperature = "2",
                    dayWindDirection = "北", nightWindDirection = "北",
                    dayWindPower = "≤3", nightWindPower = "≤3",
                ),
            ),
        )
    }
}
