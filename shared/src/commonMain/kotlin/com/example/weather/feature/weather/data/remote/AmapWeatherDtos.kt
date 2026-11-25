package com.example.weather.feature.weather.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 高德天气接口的传输模型；仅用于 data 层解析远程协议。 */
@Serializable
internal data class AmapWeatherResponseDto(
    val status: String = "",
    val info: String = "",
    val infocode: String = "",
    val lives: List<AmapCurrentWeatherDto> = emptyList(),
    val forecasts: List<AmapForecastDto> = emptyList(),
)

@Serializable
internal data class AmapCurrentWeatherDto(
    val province: String = "",
    val city: String = "",
    val weather: String = "",
    val temperature: String = "",
    @SerialName("winddirection") val windDirection: String = "",
    @SerialName("windpower") val windPower: String = "",
    val humidity: String = "",
    @SerialName("reporttime") val reportTime: String = "",
)

@Serializable
internal data class AmapForecastDto(
    val city: String = "",
    val province: String = "",
    @SerialName("reporttime") val reportTime: String = "",
    @SerialName("casts") val days: List<AmapDailyForecastDto> = emptyList(),
)

@Serializable
internal data class AmapDailyForecastDto(
    val date: String = "",
    val week: String = "",
    @SerialName("dayweather") val dayCondition: String = "",
    @SerialName("nightweather") val nightCondition: String = "",
    @SerialName("daytemp") val dayTemperature: String = "",
    @SerialName("nighttemp") val nightTemperature: String = "",
    @SerialName("daywind") val dayWindDirection: String = "",
    @SerialName("nightwind") val nightWindDirection: String = "",
    @SerialName("daypower") val dayWindPower: String = "",
    @SerialName("nightpower") val nightWindPower: String = "",
)
