package com.example.weather.feature.location.domain.repository

import com.example.weather.feature.location.domain.model.AdministrativeRegion

/** 地点功能对数据层的稳定依赖边界。 */
interface LocationRepository {
    /**
     * 返回省级节点列表（内部递归包含市、区县）。
     *
     * @param forceRefresh true 时跳过本地缓存、强制重新联网获取。
     */
    suspend fun getProvinces(forceRefresh: Boolean = false): List<AdministrativeRegion>
}
