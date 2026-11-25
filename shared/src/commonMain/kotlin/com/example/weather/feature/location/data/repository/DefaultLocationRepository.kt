package com.example.weather.feature.location.data.repository

import com.example.weather.core.storage.KeyValueStore
import com.example.weather.feature.location.data.remote.AmapDistrictDto
import com.example.weather.feature.location.data.remote.DistrictRemoteDataSource
import com.example.weather.feature.location.domain.error.LocationFailure
import com.example.weather.feature.location.domain.model.AdministrativeRegion
import com.example.weather.feature.location.domain.model.RegionLevel
import com.example.weather.feature.location.domain.repository.LocationRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 从高德获取省市区划树，并做「按天」缓存；同一天内重复获取直接读缓存。
 */
internal class DefaultLocationRepository(
    private val remoteDataSource: DistrictRemoteDataSource,
    private val store: KeyValueStore,
    private val dateProvider: () -> String,
) : LocationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getProvinces(forceRefresh: Boolean): List<AdministrativeRegion> {
        val dateKey = dateProvider()

        if (!forceRefresh) {
            store.getString(cacheKey(dateKey))
                ?.let { raw ->
                    runCatching { json.decodeFromString<List<AdministrativeRegion>>(raw) }
                        .getOrNull()
                        ?.takeIf { it.isNotEmpty() }
                }
                ?.let { return it }
        }

        val root = remoteDataSource.getDistrictTree().districts.firstOrNull()
            ?: throw LocationFailure.MissingData("district table")

        val provinces = root.districts
            .map(AmapDistrictDto::toDomain)
            .filter { it.adcode.isNotBlank() }

        runCatching { json.encodeToString(provinces) }
            .getOrNull()
            ?.let { store.putString(cacheKey(dateKey), it) }

        return provinces
    }

    private fun cacheKey(dateKey: String) = "district_cache_$dateKey"
}

private fun AmapDistrictDto.toDomain(): AdministrativeRegion = AdministrativeRegion(
    adcode = adcode,
    name = name,
    level = level.toRegionLevel(),
    children = districts.map(AmapDistrictDto::toDomain),
)

private fun String.toRegionLevel(): RegionLevel = when (this) {
    "province" -> RegionLevel.PROVINCE
    "city" -> RegionLevel.CITY
    "district" -> RegionLevel.DISTRICT
    else -> RegionLevel.CITY
}
