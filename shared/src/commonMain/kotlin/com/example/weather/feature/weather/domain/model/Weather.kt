package com.example.weather.feature.weather.domain.model

import kotlinx.serialization.Serializable

/** 应用内部使用的天气模型，同时作为本地缓存的序列化模型。 */
@Serializable
data class Weather(
    val current: CurrentWeather,
    val forecast: WeatherForecast,
)

@Serializable
data class CurrentWeather(
    val province: String,
    val city: String,
    val condition: String,
    val temperatureCelsius: String,
    val humidityPercent: String,
    val windDirection: String,
    val windPower: String,
    val observedAt: String,
)

@Serializable
data class WeatherForecast(
    val province: String,
    val city: String,
    val reportedAt: String,
    val days: List<DailyForecast>,
)

@Serializable
data class DailyForecast(
    val date: String,
    val week: String,
    val dayCondition: String,
    val nightCondition: String,
    val dayTemperatureCelsius: String,
    val nightTemperatureCelsius: String,
    val dayWindDirection: String,
    val nightWindDirection: String,
    val dayWindPower: String,
    val nightWindPower: String,
)

/** 数据实际来源，用于让 UI 准确解释缓存状态。 */
enum class WeatherDataSource {
    NETWORK,
    TODAY_CACHE,
    STALE_CACHE,
}

/** 一次天气查询结果及其来源。 */
data class WeatherResult(
    val weather: Weather,
    val source: WeatherDataSource,
    /** 缓存日期，格式 yyyyMMdd；仅缓存结果有值。 */
    val cacheDateKey: String? = null,
)
