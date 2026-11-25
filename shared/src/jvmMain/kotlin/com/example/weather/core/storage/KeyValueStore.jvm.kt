package com.example.weather.core.storage

import java.io.File
import java.util.Properties

actual fun createPlatformKeyValueStore(): KeyValueStore = PropertiesKeyValueStore()

/** 桌面 JVM 的降级实现：MMKV 目前没有 JVM target，用 properties 文件保持同语义。 */
private class PropertiesKeyValueStore : KeyValueStore {

    private val file: File = storeFile()
    private val properties: Properties = Properties()

    init {
        if (file.exists()) {
            file.inputStream().use { properties.load(it) }
        }
    }

    @Synchronized
    override fun getString(key: String): String? = properties.getProperty(key)

    @Synchronized
    override fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        file.parentFile?.mkdirs()
        file.outputStream().use { properties.store(it, "weather app storage") }
    }

    private fun storeFile(): File {
        val base = System.getProperty("user.home")
            ?.takeIf(String::isNotBlank)
            ?.let { File(it, ".weather") }
            ?: File(System.getProperty("java.io.tmpdir"), "weather")
        return File(base, "weather-store.properties")
    }
}
