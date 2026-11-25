package com.example.weather.feature.weather.data.cache

import com.example.weather.core.storage.KeyValueStore
import com.example.weather.feature.weather.domain.model.Weather
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 天气结果本地缓存：保留当天键，同时维护每个地点的最近成功快照用于离线回退。 */
internal class WeatherCache(
    private val store: KeyValueStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun get(city: String, dateKey: String): Weather? {
        val raw = store.getString(cacheKey(city, dateKey)) ?: return null
        return runCatching { json.decodeFromString<Weather>(raw) }.getOrNull()
    }

    fun getLatest(city: String): CachedWeather? {
        val raw = store.getString(latestCacheKey(city)) ?: return null
        return runCatching { json.decodeFromString<CachedWeather>(raw) }.getOrNull()
    }

    fun put(city: String, dateKey: String, weather: Weather) {
        val weatherRaw = runCatching { json.encodeToString(weather) }.getOrNull() ?: return
        val latestRaw = runCatching {
            json.encodeToString(CachedWeather(dateKey = dateKey, weather = weather))
        }.getOrNull() ?: return

        store.putString(cacheKey(city, dateKey), weatherRaw)
        store.putString(latestCacheKey(city), latestRaw)
    }

    private fun cacheKey(city: String, dateKey: String) = "weather_cache_${city}_$dateKey"
    private fun latestCacheKey(city: String) = "weather_cache_latest_$city"
}

@Serializable
internal data class CachedWeather(
    val dateKey: String,
    val weather: Weather,
)
