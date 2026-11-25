package com.example.weather.feature.location.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RegionLevel { PROVINCE, CITY, DISTRICT }

/** 行政区划节点，供地点选择器逐级展开（省 → 市 → 区县）。 */
@Serializable
data class AdministrativeRegion(
    val adcode: String,
    val name: String,
    val level: RegionLevel,
    val children: List<AdministrativeRegion> = emptyList(),
)

/** 用户当前选中的查询地点。 */
@Serializable
data class SelectedLocation(
    val adcode: String,
    val name: String,
    val fullName: String,
)
