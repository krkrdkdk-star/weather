package com.example.weather.feature.weather.domain.repository

import com.example.weather.feature.weather.domain.model.WeatherResult

/** 天气功能对数据层的稳定依赖边界。 */
interface WeatherRepository {
    /**
     * @param forceRefresh true 时跳过当天缓存并明确请求远程接口；网络失败仍允许回退到最近缓存。
     */
    suspend fun getWeather(city: String, forceRefresh: Boolean = false): WeatherResult
}
