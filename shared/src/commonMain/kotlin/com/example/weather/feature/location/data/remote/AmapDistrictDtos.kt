package com.example.weather.feature.location.data.remote

import kotlinx.serialization.Serializable

/** 高德行政区划查询接口的传输模型；仅用于 data 层解析远程协议。 */
@Serializable
internal data class AmapDistrictResponseDto(
    val status: String = "",
    val info: String = "",
    val infocode: String = "",
    val districts: List<AmapDistrictDto> = emptyList(),
)

/**
 * 注意：高德该接口中「国家」级节点的 citycode 是空数组 []，而省/市/区是字符串，
 * 因此这里刻意不声明 citycode（以及用不到的 center），交由 ignoreUnknownKeys 跳过，
 * 避免类型不一致导致反序列化失败。
 */
@Serializable
internal data class AmapDistrictDto(
    val adcode: String = "",
    val name: String = "",
    val level: String = "",
    val districts: List<AmapDistrictDto> = emptyList(),
)
