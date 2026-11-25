package com.example.weather

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.weather.app.WeatherApp
import com.example.weather.app.WeatherAppContainer

fun main() {
    val container = WeatherAppContainer()

    try {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Weather",
            ) {
                WeatherApp(container = container)
            }
        }
    } finally {
        container.close()
    }
}
