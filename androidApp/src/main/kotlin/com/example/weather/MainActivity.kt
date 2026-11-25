package com.example.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.weather.app.WeatherApp
import com.example.weather.feature.location.domain.model.SelectedLocation
import com.example.weather.feature.weather.presentation.LocationPickerState
import com.example.weather.feature.weather.presentation.WeatherScreen
import com.example.weather.feature.weather.presentation.WeatherUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as WeatherApplication).container
        setContent {
            WeatherApp(container = container)
        }
    }
}

@Preview
@Composable
private fun WeatherLoadingPreview() {
    WeatherScreen(
        weatherState = WeatherUiState.Loading,
        isRefreshing = false,
        location = SelectedLocation(
            adcode = "110101",
            name = "东城区",
            fullName = "北京市 东城区",
        ),
        pickerState = LocationPickerState(),
        onRetry = {},
        onNormalRefresh = {},
        onForceRefresh = {},
        onOpenPicker = {},
        onClosePicker = {},
        onRegionClick = {},
        onLocationClick = {},
        onToggleFavorite = {},
        onBack = {},
        onRefreshDistricts = {},
        onSearchQueryChange = {},
        onConfigureApiKey = {},
    )
}
