package com.example.weather.testutil

import com.example.weather.core.storage.KeyValueStore

/** 供测试使用的内存键值存储，语义与平台存储一致。 */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}
