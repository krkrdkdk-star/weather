package com.example.weather.feature.weather.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.feature.location.data.store.LocationStore
import com.example.weather.feature.location.domain.error.LocationFailure
import com.example.weather.feature.location.domain.model.AdministrativeRegion
import com.example.weather.feature.location.domain.model.SelectedLocation
import com.example.weather.feature.location.domain.repository.LocationRepository
import com.example.weather.feature.location.domain.search.locationSearchTokens
import com.example.weather.feature.weather.domain.error.WeatherFailure
import com.example.weather.feature.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 管理天气、刷新状态，以及带搜索/最近/收藏能力的地点选择器。 */
class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val locationStore: LocationStore,
) : ViewModel() {

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _location = MutableStateFlow(DEFAULT_LOCATION)
    val location: StateFlow<SelectedLocation> = _location.asStateFlow()

    private val _pickerState = MutableStateFlow(LocationPickerState())
    val pickerState: StateFlow<LocationPickerState> = _pickerState.asStateFlow()

    private var provinceTree: List<AdministrativeRegion> = emptyList()
    private var searchIndex: List<LocationSearchEntry> = emptyList()
    private var pendingPathCodes: List<String> = emptyList()

    private var inFlightWeather: Job? = null
    private var weatherSequence = 0

    init {
        locationStore.load()?.let { _location.value = it }
        val recent = locationStore.recordRecent(_location.value)
        _pickerState.value = _pickerState.value.copy(
            recentLocations = recent,
            favoriteLocations = locationStore.loadFavorites(),
        )
        loadWeather(preserveContent = false)
    }

    /** 正常刷新仍允许命中当天缓存；forceRefresh=true 明确消耗远程 API 配额。 */
    fun refreshWeather(forceRefresh: Boolean = false) {
        loadWeather(forceRefresh = forceRefresh, preserveContent = true)
    }

    fun retry() = loadWeather(preserveContent = false)

    private fun loadWeather(
        forceRefresh: Boolean = false,
        preserveContent: Boolean,
    ) {
        val city = _location.value.adcode
        inFlightWeather?.cancel()
        val requestId = ++weatherSequence
        val hadContent = _weatherUiState.value is WeatherUiState.Content

        inFlightWeather = viewModelScope.launch {
            if (preserveContent && hadContent) {
                _isRefreshing.value = true
            } else {
                _weatherUiState.value = WeatherUiState.Loading
            }

            val result = loadWeatherState(city, forceRefresh)
            if (requestId == weatherSequence) {
                _weatherUiState.value = result
                _isRefreshing.value = false
            }
        }
    }

    // ---- 地点选择器 ----

    fun openPicker(
        restoredPathCodes: List<String> = emptyList(),
        restoredSearchQuery: String = "",
    ) {
        pendingPathCodes = restoredPathCodes
        val restoredPath = pathForCodes(restoredPathCodes)
        _pickerState.value = LocationPickerState(
            visible = true,
            path = restoredPath,
            items = itemsForPath(restoredPath),
            searchQuery = restoredSearchQuery,
            searchResults = search(restoredSearchQuery),
            recentLocations = locationStore.loadRecent(),
            favoriteLocations = locationStore.loadFavorites(),
        )
        if (provinceTree.isEmpty()) loadDistricts()
    }

    fun closePicker() {
        _pickerState.value = _pickerState.value.copy(visible = false)
    }

    fun onRegionClick(region: AdministrativeRegion) {
        if (region.children.isNotEmpty()) {
            val newPath = _pickerState.value.path + region
            pendingPathCodes = newPath.map(AdministrativeRegion::adcode)
            _pickerState.value = _pickerState.value.copy(path = newPath, items = region.children)
        } else {
            val fullName = (_pickerState.value.path.map(AdministrativeRegion::name) + region.name)
                .joinToString(" ")
            selectLocation(SelectedLocation(region.adcode, region.name, fullName))
        }
    }

    fun onBack() {
        val path = _pickerState.value.path
        if (path.isEmpty()) {
            closePicker()
            return
        }
        val newPath = path.dropLast(1)
        pendingPathCodes = newPath.map(AdministrativeRegion::adcode)
        _pickerState.value = _pickerState.value.copy(
            path = newPath,
            items = itemsForPath(newPath),
        )
    }

    fun onSearchQueryChange(query: String) {
        _pickerState.value = _pickerState.value.copy(
            searchQuery = query,
            searchResults = search(query),
        )
    }

    fun selectLocation(location: SelectedLocation) {
        _location.value = location
        locationStore.save(location)
        val recent = locationStore.recordRecent(location)
        _pickerState.value = _pickerState.value.copy(recentLocations = recent, visible = false)
        loadWeather(preserveContent = false)
    }

    fun toggleFavorite(location: SelectedLocation = _location.value) {
        val favorites = locationStore.toggleFavorite(location)
        _pickerState.value = _pickerState.value.copy(favoriteLocations = favorites)
    }

    fun refreshDistricts() = loadDistricts(forceRefresh = true)

    private fun loadDistricts(forceRefresh: Boolean = false) {
        _pickerState.value = _pickerState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val error = try {
                provinceTree = locationRepository.getProvinces(forceRefresh)
                searchIndex = buildSearchIndex(provinceTree)
                null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: LocationFailure) {
                failure.toUserMessage()
            } catch (_: Exception) {
                "地点列表加载失败，请稍后重试"
            }

            _pickerState.value = if (error == null) {
                val path = pathForCodes(pendingPathCodes)
                _pickerState.value.copy(
                    loading = false,
                    error = null,
                    path = path,
                    items = itemsForPath(path),
                    searchResults = search(_pickerState.value.searchQuery),
                )
            } else {
                _pickerState.value.copy(loading = false, error = error)
            }
        }
    }

    private fun pathForCodes(codes: List<String>): List<AdministrativeRegion> {
        var items = provinceTree
        val path = mutableListOf<AdministrativeRegion>()
        for (code in codes) {
            val node = items.firstOrNull { it.adcode == code } ?: break
            path += node
            items = node.children
        }
        return path
    }

    private fun itemsForPath(path: List<AdministrativeRegion>): List<AdministrativeRegion> =
        path.lastOrNull()?.children ?: provinceTree

    private fun search(query: String): List<SelectedLocation> {
        val normalizedQuery = query.trim().lowercase().replace(" ", "")
        if (normalizedQuery.isEmpty()) return emptyList()
        return searchIndex.asSequence()
            .filter { entry -> entry.tokens.any { normalizedQuery in it } }
            .map(LocationSearchEntry::location)
            .distinctBy(SelectedLocation::adcode)
            .take(MAX_SEARCH_RESULTS)
            .toList()
    }

    private fun buildSearchIndex(
        regions: List<AdministrativeRegion>,
        parentNames: List<String> = emptyList(),
    ): List<LocationSearchEntry> = buildList {
        regions.forEach { region ->
            val names = parentNames + region.name
            val location = SelectedLocation(
                adcode = region.adcode,
                name = region.name,
                fullName = names.joinToString(" "),
            )
            add(
                LocationSearchEntry(
                    location = location,
                    tokens = locationSearchTokens(location.name) + locationSearchTokens(location.fullName),
                ),
            )
            addAll(buildSearchIndex(region.children, names))
        }
    }

    private suspend fun loadWeatherState(
        city: String,
        forceRefresh: Boolean,
    ): WeatherUiState = try {
        val result = weatherRepository.getWeather(city, forceRefresh)
        WeatherUiState.Content(
            weather = result.weather,
            source = result.source,
            cacheDateKey = result.cacheDateKey,
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: WeatherFailure) {
        WeatherUiState.Error(failure.toUserMessage())
    } catch (_: Exception) {
        WeatherUiState.Error("天气数据加载失败，请稍后重试")
    }

    private fun WeatherFailure.toUserMessage(): String = when (this) {
        WeatherFailure.InvalidCity -> "城市不能为空"
        is WeatherFailure.ServiceRejected -> "天气服务请求失败（错误码：$code）"
        is WeatherFailure.Http -> "服务暂时不可用（HTTP $statusCode）"
        is WeatherFailure.MissingData -> "天气服务没有返回${dataName}数据"
        is WeatherFailure.Network -> "网络连接失败，请检查网络后重试"
        is WeatherFailure.InvalidResponse -> "天气数据格式异常"
    }

    private fun LocationFailure.toUserMessage(): String = when (this) {
        is LocationFailure.ServiceRejected -> "地点服务请求失败（错误码：$code）"
        is LocationFailure.Http -> "地点服务暂时不可用（HTTP $statusCode）"
        is LocationFailure.Network -> "网络连接失败，请检查网络后重试"
        is LocationFailure.InvalidResponse -> "地点数据格式异常"
        is LocationFailure.MissingData -> "地点服务没有返回${dataName}数据"
    }

    private companion object {
        const val MAX_SEARCH_RESULTS = 100
        val DEFAULT_LOCATION = SelectedLocation(
            adcode = "110101",
            name = "东城区",
            fullName = "北京市 东城区",
        )
    }
}

private data class LocationSearchEntry(
    val location: SelectedLocation,
    val tokens: Set<String>,
)
