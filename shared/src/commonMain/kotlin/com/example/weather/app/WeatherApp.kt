package com.example.weather.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.weather.feature.weather.presentation.WeatherRoute

/** 跨平台应用根节点：先完成本地 Key 配置，再装配天气功能。 */
@Composable
fun WeatherApp(
    container: WeatherAppContainer,
) {
    var activeApiKey by remember { mutableStateOf(container.apiKeyStore.load()) }
    var configuringApiKey by remember { mutableStateOf(activeApiKey == null) }

    MaterialTheme {
        if (configuringApiKey) {
            ApiKeySetupScreen(
                initialValue = activeApiKey.orEmpty(),
                canCancel = activeApiKey != null,
                onSave = { apiKey ->
                    container.apiKeyStore.save(apiKey)
                    activeApiKey = apiKey
                    configuringApiKey = false
                },
                onCancel = { configuringApiKey = false },
            )
        } else {
            val apiKey = requireNotNull(activeApiKey)
            key(apiKey) {
                val dependencies = remember(apiKey) {
                    container.createWeatherFeature(apiKey)
                }
                WeatherRoute(
                    viewModelKey = apiKey,
                    weatherRepository = dependencies.weatherRepository,
                    locationRepository = dependencies.locationRepository,
                    locationStore = container.locationStore,
                    onConfigureApiKey = { configuringApiKey = true },
                )
            }
        }
    }
}
