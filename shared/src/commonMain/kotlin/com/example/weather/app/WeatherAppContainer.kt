package com.example.weather.app

import com.example.weather.core.config.ApiKeyStore
import com.example.weather.core.network.createAppHttpClient
import com.example.weather.core.storage.createPlatformKeyValueStore
import com.example.weather.core.time.platformDateProvider
import com.example.weather.feature.location.data.remote.AmapDistrictRemoteDataSource
import com.example.weather.feature.location.data.repository.DefaultLocationRepository
import com.example.weather.feature.location.data.store.LocationStore
import com.example.weather.feature.location.domain.repository.LocationRepository
import com.example.weather.feature.weather.data.cache.WeatherCache
import com.example.weather.feature.weather.data.remote.AmapWeatherRemoteDataSource
import com.example.weather.feature.weather.data.repository.DefaultWeatherRepository
import com.example.weather.feature.weather.domain.repository.WeatherRepository
import io.ktor.client.HttpClient

/**
 * 应用级依赖容器。
 *
 * 不再在进程启动时要求 API Key。应用先读取用户本地配置，保存 Key 后再通过
 * [createWeatherFeature] 创建网络相关依赖，从而适合不附带私有密钥的开源发行。
 */
class WeatherAppContainer : AutoCloseable {
    private val httpClient: HttpClient = createAppHttpClient()
    private val keyValueStore = createPlatformKeyValueStore()
    private val dateProvider = platformDateProvider()

    val apiKeyStore = ApiKeyStore(keyValueStore)
    val locationStore = LocationStore(keyValueStore)

    fun createWeatherFeature(apiKey: String): WeatherFeatureDependencies {
        val normalizedKey = apiKey.trim()
        require(normalizedKey.isNotEmpty()) { "API key must not be blank" }

        return WeatherFeatureDependencies(
            weatherRepository = DefaultWeatherRepository(
                remoteDataSource = AmapWeatherRemoteDataSource(
                    apiKey = normalizedKey,
                    httpClient = httpClient,
                ),
                cache = WeatherCache(keyValueStore),
                dateProvider = dateProvider::todayKey,
            ),
            locationRepository = DefaultLocationRepository(
                remoteDataSource = AmapDistrictRemoteDataSource(
                    apiKey = normalizedKey,
                    httpClient = httpClient,
                ),
                store = keyValueStore,
                dateProvider = dateProvider::todayKey,
            ),
        )
    }

    override fun close() {
        httpClient.close()
    }
}

data class WeatherFeatureDependencies(
    val weatherRepository: WeatherRepository,
    val locationRepository: LocationRepository,
)
