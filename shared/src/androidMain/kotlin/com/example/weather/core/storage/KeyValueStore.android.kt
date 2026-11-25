package com.example.weather.core.storage

import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize

actual fun createPlatformKeyValueStore(): KeyValueStore {
    // mmkv-kmp 通过 AndroidX App Startup 自动捕获 Application Context，
    // 无参 initialize() 即用该 Context 完成初始化。
    MMKV.initialize()
    return MmkvKeyValueStore(MMKV.defaultMMKV())
}

private class MmkvKeyValueStore(
    private val mmkv: MMKV,
) : KeyValueStore {

    override fun getString(key: String): String? = mmkv.decodeString(key)

    override fun putString(key: String, value: String) {
        mmkv.encodeString(key, value)
    }
}
