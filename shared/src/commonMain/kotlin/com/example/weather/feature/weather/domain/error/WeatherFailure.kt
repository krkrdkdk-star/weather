package com.example.weather.feature.weather.domain.error

/**
 * 天气功能可预期的业务失败。
 *
 * 这里不暴露 Ktor、序列化库或高德响应 DTO，使 presentation 只依赖稳定的业务语义。
 */
sealed class WeatherFailure(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data object InvalidCity : WeatherFailure("City must not be blank")

    data class ServiceRejected(
        val code: String,
        val serviceMessage: String,
    ) : WeatherFailure("Weather service rejected the request: $code")

    data class Http(val statusCode: Int, val originalCause: Throwable) :
        WeatherFailure("Weather service returned HTTP $statusCode", originalCause)

    data class MissingData(val dataName: String) :
        WeatherFailure("Weather service returned no $dataName data")

    data class Network(val originalCause: Throwable) :
        WeatherFailure("Weather service is unreachable", originalCause)

    data class InvalidResponse(val originalCause: Throwable) :
        WeatherFailure("Weather service returned an invalid response", originalCause)
}
