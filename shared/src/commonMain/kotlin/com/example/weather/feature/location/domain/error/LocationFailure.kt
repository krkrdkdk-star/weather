package com.example.weather.feature.location.domain.error

/**
 * 地点（行政区划）功能可预期的业务失败。
 *
 * 不暴露 Ktor、序列化库或高德响应 DTO，使 presentation 只依赖稳定的业务语义。
 */
sealed class LocationFailure(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    data class ServiceRejected(
        val code: String,
        val serviceMessage: String,
    ) : LocationFailure("Location service rejected the request: $code")

    data class Http(val statusCode: Int, val originalCause: Throwable) :
        LocationFailure("Location service returned HTTP $statusCode", originalCause)

    data class Network(val originalCause: Throwable) :
        LocationFailure("Location service is unreachable", originalCause)

    data class InvalidResponse(val originalCause: Throwable) :
        LocationFailure("Location service returned an invalid response", originalCause)

    data class MissingData(val dataName: String) :
        LocationFailure("Location service returned no $dataName data")
}
