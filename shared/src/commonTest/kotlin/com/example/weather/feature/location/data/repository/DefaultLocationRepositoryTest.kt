package com.example.weather.feature.location.data.repository

import com.example.weather.feature.location.data.remote.AmapDistrictDto
import com.example.weather.feature.location.data.remote.AmapDistrictResponseDto
import com.example.weather.feature.location.data.remote.DistrictRemoteDataSource
import com.example.weather.feature.location.domain.error.LocationFailure
import com.example.weather.feature.location.domain.model.RegionLevel
import com.example.weather.testutil.InMemoryKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultLocationRepositoryTest {

    @Test
    fun mapsDistrictTreeToDomainModel() = runTest {
        val repository = repository(FakeDistrictRemoteDataSource())

        val provinces = repository.getProvinces()

        val province = provinces.single()
        assertEquals("北京市", province.name)
        assertEquals(RegionLevel.PROVINCE, province.level)
        val city = province.children.single()
        assertEquals("北京市", city.name)
        assertEquals(RegionLevel.CITY, city.level)
        val district = city.children.single()
        assertEquals("东城区", district.name)
        assertEquals(RegionLevel.DISTRICT, district.level)
    }

    @Test
    fun cachesDistrictTreeWithinSameDay() = runTest {
        val source = CountingDistrictRemoteDataSource()
        val repository = repository(source, dateProvider = { "20260101" })

        repository.getProvinces()
        repository.getProvinces()

        assertEquals(1, source.calls)
    }

    @Test
    fun forceRefreshBypassesCache() = runTest {
        val source = CountingDistrictRemoteDataSource()
        val repository = repository(source, dateProvider = { "20260101" })

        repository.getProvinces()
        repository.getProvinces(forceRefresh = true)

        assertEquals(2, source.calls)
    }

    @Test
    fun rejectsMissingDistrictTable() = runTest {
        val source = object : DistrictRemoteDataSource {
            override suspend fun getDistrictTree() =
                AmapDistrictResponseDto(status = "1", infocode = "10000", districts = emptyList())
        }

        assertFailsWith<LocationFailure.MissingData> {
            repository(source).getProvinces()
        }
    }

    private fun repository(
        source: DistrictRemoteDataSource,
        dateProvider: () -> String = { "20260101" },
    ) = DefaultLocationRepository(
        remoteDataSource = source,
        store = InMemoryKeyValueStore(),
        dateProvider = dateProvider,
    )

    private class CountingDistrictRemoteDataSource : DistrictRemoteDataSource {
        var calls = 0
            private set

        override suspend fun getDistrictTree(): AmapDistrictResponseDto {
            calls++
            return FakeDistrictRemoteDataSource().getDistrictTree()
        }
    }

    private class FakeDistrictRemoteDataSource : DistrictRemoteDataSource {
        override suspend fun getDistrictTree() = AmapDistrictResponseDto(
            status = "1",
            info = "OK",
            infocode = "10000",
            districts = listOf(
                AmapDistrictDto(
                    adcode = "100000",
                    name = "中国",
                    level = "country",
                    districts = listOf(
                        AmapDistrictDto(
                            adcode = "110000",
                            name = "北京市",
                            level = "province",
                            districts = listOf(
                                AmapDistrictDto(
                                    adcode = "110100",
                                    name = "北京市",
                                    level = "city",
                                    districts = listOf(
                                        AmapDistrictDto(
                                            adcode = "110101",
                                            name = "东城区",
                                            level = "district",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
