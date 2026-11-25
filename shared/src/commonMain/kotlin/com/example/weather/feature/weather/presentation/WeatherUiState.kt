package com.example.weather.feature.weather.presentation

import com.example.weather.feature.location.domain.model.AdministrativeRegion
import com.example.weather.feature.location.domain.model.SelectedLocation
import com.example.weather.feature.weather.domain.model.Weather
import com.example.weather.feature.weather.domain.model.WeatherDataSource

/** 天气页面所有可渲染状态。 */
sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Content(
        val weather: Weather,
        val source: WeatherDataSource,
        val cacheDateKey: String? = null,
    ) : WeatherUiState

    data class Error(val message: String) : WeatherUiState
}

/** 地点选择器的可渲染状态。 */
data class LocationPickerState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val path: List<AdministrativeRegion> = emptyList(),
    val items: List<AdministrativeRegion> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SelectedLocation> = emptyList(),
    val recentLocations: List<SelectedLocation> = emptyList(),
    val favoriteLocations: List<SelectedLocation> = emptyList(),
)
