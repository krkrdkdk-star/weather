package com.example.weather.core.storage

/**
 * 极简的跨平台键值存储抽象。
 *
 * commonMain 只依赖这份稳定的读写语义，不感知具体存储引擎：
 * Android 使用 MMKV，桌面 JVM 使用本地 properties 文件。
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** 创建平台默认的键值存储实例。 */
expect fun createPlatformKeyValueStore(): KeyValueStore
