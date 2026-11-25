package com.example.weather.feature.location.data.store

import com.example.weather.feature.location.domain.model.SelectedLocation
import com.example.weather.testutil.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationStoreTest {

    @Test
    fun recentLocationsAreUniqueNewestFirstAndLimited() {
        val store = LocationStore(InMemoryKeyValueStore())
        repeat(10) { index ->
            store.recordRecent(location(index.toString(), "地点$index"))
        }
        store.recordRecent(location("5", "地点5"))

        val recent = store.loadRecent()
        assertEquals(8, recent.size)
        assertEquals("5", recent.first().adcode)
        assertEquals(1, recent.count { it.adcode == "5" })
    }

    @Test
    fun toggleFavoriteAddsAndRemovesLocation() {
        val store = LocationStore(InMemoryKeyValueStore())
        val beijing = location("110101", "东城区")

        assertEquals(listOf(beijing), store.toggleFavorite(beijing))
        assertTrue(store.toggleFavorite(beijing).isEmpty())
    }

    private fun location(code: String, name: String) = SelectedLocation(code, name, name)
}
