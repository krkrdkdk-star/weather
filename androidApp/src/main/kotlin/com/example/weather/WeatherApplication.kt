package com.example.weather

import android.app.Application
import com.example.weather.app.WeatherAppContainer

/** Android 进程级依赖入口。 */
class WeatherApplication : Application() {
    lateinit var container: WeatherAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = WeatherAppContainer()
    }
}
