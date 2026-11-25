package com.example.weather.feature.location.data.store

import com.example.weather.core.storage.KeyValueStore
import com.example.weather.feature.location.domain.model.SelectedLocation
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 持久化当前地点、最近使用地点与收藏地点。 */
class LocationStore(
    private val store: KeyValueStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): SelectedLocation? = decodeLocation(SELECTED_LOCATION_KEY)

    fun save(location: SelectedLocation) {
        encodeLocation(SELECTED_LOCATION_KEY, location)
    }

    fun loadRecent(): List<SelectedLocation> = decodeLocations(RECENT_LOCATIONS_KEY)

    fun recordRecent(location: SelectedLocation): List<SelectedLocation> {
        val updated = (listOf(location) + loadRecent())
            .distinctBy(SelectedLocation::adcode)
            .take(MAX_RECENT_LOCATIONS)
        encodeLocations(RECENT_LOCATIONS_KEY, updated)
        return updated
    }

    fun loadFavorites(): List<SelectedLocation> = decodeLocations(FAVORITE_LOCATIONS_KEY)

    fun toggleFavorite(location: SelectedLocation): List<SelectedLocation> {
        val current = loadFavorites()
        val updated = if (current.any { it.adcode == location.adcode }) {
            current.filterNot { it.adcode == location.adcode }
        } else {
            listOf(location) + current
        }
        encodeLocations(FAVORITE_LOCATIONS_KEY, updated)
        return updated
    }

    private fun decodeLocation(key: String): SelectedLocation? {
        val raw = store.getString(key) ?: return null
        return runCatching { json.decodeFromString<SelectedLocation>(raw) }.getOrNull()
    }

    private fun decodeLocations(key: String): List<SelectedLocation> {
        val raw = store.getString(key) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SelectedLocation>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun encodeLocation(key: String, location: SelectedLocation) {
        runCatching { json.encodeToString(location) }
            .getOrNull()
            ?.let { store.putString(key, it) }
    }

    private fun encodeLocations(key: String, locations: List<SelectedLocation>) {
        runCatching { json.encodeToString(locations) }
            .getOrNull()
            ?.let { store.putString(key, it) }
    }

    private companion object {
        const val SELECTED_LOCATION_KEY = "selected_location"
        const val RECENT_LOCATIONS_KEY = "recent_locations"
        const val FAVORITE_LOCATIONS_KEY = "favorite_locations"
        const val MAX_RECENT_LOCATIONS = 8
    }
}
