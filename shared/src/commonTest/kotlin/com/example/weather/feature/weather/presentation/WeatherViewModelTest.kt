package com.example.weather.feature.weather.presentation

import com.example.weather.feature.location.data.store.LocationStore
import com.example.weather.feature.location.domain.error.LocationFailure
import com.example.weather.feature.location.domain.model.AdministrativeRegion
import com.example.weather.feature.location.domain.model.RegionLevel
import com.example.weather.feature.location.domain.model.SelectedLocation
import com.example.weather.feature.location.domain.repository.LocationRepository
import com.example.weather.feature.weather.domain.error.WeatherFailure
import com.example.weather.feature.weather.domain.model.CurrentWeather
import com.example.weather.feature.weather.domain.model.Weather
import com.example.weather.feature.weather.domain.model.WeatherDataSource
import com.example.weather.feature.weather.domain.model.WeatherForecast
import com.example.weather.feature.weather.domain.model.WeatherResult
import com.example.weather.feature.weather.domain.repository.WeatherRepository
import com.example.weather.testutil.InMemoryKeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(mainDispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsWeatherIntoContentState() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val content = assertIs<WeatherUiState.Content>(viewModel.weatherUiState.value)
        assertEquals("110101", content.weather.current.city)
        assertEquals(WeatherDataSource.NETWORK, content.source)
    }

    @Test
    fun forcedRefreshIsForwardedToRepository() = runTest(mainDispatcher) {
        val requests = mutableListOf<Boolean>()
        val viewModel = viewModel(
            weatherRepository = FakeWeatherRepository { city, force ->
                requests += force
                weatherResult(city)
            },
        )
        advanceUntilIdle()

        viewModel.refreshWeather(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(listOf(false, true), requests)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun loadsPersistedLocationAndRecordsItAsRecent() = runTest(mainDispatcher) {
        val store = InMemoryKeyValueStore()
        val locationStore = LocationStore(store)
        locationStore.save(SelectedLocation("440300", "深圳市", "广东省 深圳市"))
        val viewModel = viewModel(store = store)
        advanceUntilIdle()

        assertEquals("440300", viewModel.location.value.adcode)
        assertEquals("440300", locationStore.loadRecent().first().adcode)
    }

    @Test
    fun mapsDomainFailureToUserMessage() = runTest(mainDispatcher) {
        val viewModel = viewModel(FakeWeatherRepository { _, _ ->
            throw WeatherFailure.Network(IllegalStateException("offline"))
        })
        advanceUntilIdle()
        assertEquals(
            WeatherUiState.Error("网络连接失败，请检查网络后重试"),
            viewModel.weatherUiState.value,
        )
    }

    @Test
    fun selectingLeafPersistsReloadsAndAddsRecent() = runTest(mainDispatcher) {
        val store = InMemoryKeyValueStore()
        val viewModel = viewModel(locationRepository = FakeLocationRepository { listOf(tree()) }, store = store)
        advanceUntilIdle()

        viewModel.openPicker()
        advanceUntilIdle()
        viewModel.onRegionClick(viewModel.pickerState.value.items.single())
        viewModel.onRegionClick(viewModel.pickerState.value.items.single())
        viewModel.onRegionClick(viewModel.pickerState.value.items.first { it.adcode == "110101" })
        advanceUntilIdle()

        assertEquals("110101", viewModel.location.value.adcode)
        assertEquals("北京市 北京市 东城区", viewModel.location.value.fullName)
        assertEquals("110101", LocationStore(store).loadRecent().first().adcode)
    }

    @Test
    fun searchesByChineseFullPinyinAndInitials() = runTest(mainDispatcher) {
        val viewModel = viewModel(locationRepository = FakeLocationRepository { listOf(tree()) })
        advanceUntilIdle()
        viewModel.openPicker()
        advanceUntilIdle()

        listOf("东城", "dongcheng", "dc").forEach { query ->
            viewModel.onSearchQueryChange(query)
            assertEquals("110101", viewModel.pickerState.value.searchResults.first().adcode)
        }
    }

    @Test
    fun restoresPickerPathCodesAfterDistrictLoad() = runTest(mainDispatcher) {
        val viewModel = viewModel(locationRepository = FakeLocationRepository { listOf(tree()) })
        advanceUntilIdle()

        viewModel.openPicker(restoredPathCodes = listOf("110000", "110100"))
        advanceUntilIdle()

        assertEquals(listOf("110000", "110100"), viewModel.pickerState.value.path.map { it.adcode })
        assertTrue(viewModel.pickerState.value.items.any { it.adcode == "110101" })
    }

    @Test
    fun togglesFavoriteAndQuickSelectsLocation() = runTest(mainDispatcher) {
        val store = InMemoryKeyValueStore()
        val viewModel = viewModel(store = store)
        advanceUntilIdle()
        val shenzhen = SelectedLocation("440300", "深圳市", "广东省 深圳市")

        viewModel.toggleFavorite(shenzhen)
        assertEquals(listOf(shenzhen), viewModel.pickerState.value.favoriteLocations)
        viewModel.selectLocation(shenzhen)
        advanceUntilIdle()

        assertEquals("440300", viewModel.location.value.adcode)
        assertEquals("440300", LocationStore(store).loadRecent().first().adcode)
    }

    @Test
    fun pickerShowsErrorWhenDistrictsFail() = runTest(mainDispatcher) {
        val viewModel = viewModel(
            locationRepository = FakeLocationRepository {
                throw LocationFailure.Network(IllegalStateException("offline"))
            },
        )
        advanceUntilIdle()
        viewModel.openPicker()
        advanceUntilIdle()

        assertEquals("网络连接失败，请检查网络后重试", viewModel.pickerState.value.error)
        assertNull(viewModel.pickerState.value.items.firstOrNull())
    }

    private fun viewModel(
        weatherRepository: WeatherRepository = FakeWeatherRepository { city, _ -> weatherResult(city) },
        locationRepository: LocationRepository = FakeLocationRepository { emptyList() },
        store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
    ) = WeatherViewModel(weatherRepository, locationRepository, LocationStore(store))

    private class FakeWeatherRepository(
        private val result: suspend (String, Boolean) -> WeatherResult,
    ) : WeatherRepository {
        override suspend fun getWeather(city: String, forceRefresh: Boolean) = result(city, forceRefresh)
    }

    private class FakeLocationRepository(
        private val result: suspend (Boolean) -> List<AdministrativeRegion>,
    ) : LocationRepository {
        override suspend fun getProvinces(forceRefresh: Boolean) = result(forceRefresh)
    }

    private fun tree() = AdministrativeRegion(
        adcode = "110000", name = "北京市", level = RegionLevel.PROVINCE,
        children = listOf(
            AdministrativeRegion(
                adcode = "110100", name = "北京市", level = RegionLevel.CITY,
                children = listOf(
                    AdministrativeRegion("110101", "东城区", RegionLevel.DISTRICT),
                    AdministrativeRegion("110108", "海淀区", RegionLevel.DISTRICT),
                ),
            ),
        ),
    )

    private fun weatherResult(city: String) = WeatherResult(
        weather = Weather(
            current = CurrentWeather(
                province = "北京", city = city, condition = "晴", temperatureCelsius = "20",
                humidityPercent = "40", windDirection = "北", windPower = "2",
                observedAt = "2026-01-01 08:00:00",
            ),
            forecast = WeatherForecast("北京", city, "2026-01-01 08:00:00", emptyList()),
        ),
        source = WeatherDataSource.NETWORK,
    )
}
