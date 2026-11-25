package com.example.weather.feature.location.data.remote

import com.example.weather.feature.location.domain.error.LocationFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal interface DistrictRemoteDataSource {
    suspend fun getDistrictTree(): AmapDistrictResponseDto
}

/** 高德行政区划协议的唯一实现边界。 */
internal class AmapDistrictRemoteDataSource(
    private val apiKey: String,
    private val httpClient: HttpClient,
) : DistrictRemoteDataSource {

    init {
        require(apiKey.isNotBlank()) {
            "Missing Amap API key. Configure it in the app before loading locations."
        }
    }

    override suspend fun getDistrictTree(): AmapDistrictResponseDto {
        val response = executeRequest()
        if (response.status != SUCCESS_STATUS || response.infocode != SUCCESS_INFO_CODE) {
            throw LocationFailure.ServiceRejected(
                code = response.infocode,
                serviceMessage = response.info.ifBlank { "Location service rejected the request" },
            )
        }
        return response
    }

    private suspend fun executeRequest(): AmapDistrictResponseDto = try {
        httpClient.get(DISTRICT_ENDPOINT) {
            parameter("key", apiKey)
            parameter("keywords", CHINA_KEYWORD)
            parameter("subdistrict", "3")
            parameter("extensions", "base")
        }.body()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (httpError: ResponseException) {
        throw LocationFailure.Http(httpError.response.status.value, httpError)
    } catch (conversionError: JsonConvertException) {
        throw LocationFailure.InvalidResponse(conversionError)
    } catch (serializationError: SerializationException) {
        throw LocationFailure.InvalidResponse(serializationError)
    } catch (transformationError: NoTransformationFoundException) {
        throw LocationFailure.InvalidResponse(transformationError)
    } catch (knownFailure: LocationFailure) {
        throw knownFailure
    } catch (networkError: Exception) {
        throw LocationFailure.Network(networkError)
    }

    private companion object {
        const val DISTRICT_ENDPOINT = "https://restapi.amap.com/v3/config/district"
        const val CHINA_KEYWORD = "中国"
        const val SUCCESS_STATUS = "1"
        const val SUCCESS_INFO_CODE = "10000"
    }
}
