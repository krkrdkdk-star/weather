package com.example.weather.core.config

import com.example.weather.core.storage.KeyValueStore

/** 持久化用户自行配置的高德 Web 服务 API Key。 */
class ApiKeyStore(
    private val store: KeyValueStore,
) {
    fun load(): String? = store.getString(API_KEY_STORAGE_KEY)
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    fun save(apiKey: String) {
        val normalized = apiKey.trim()
        require(validateAmapApiKey(normalized) == null) { "Invalid Amap API key" }
        store.putString(API_KEY_STORAGE_KEY, normalized)
    }

    private companion object {
        const val API_KEY_STORAGE_KEY = "amap_api_key"
    }
}

/** 返回 null 表示合法，否则返回可直接展示给用户的中文错误信息。 */
fun validateAmapApiKey(input: String): String? {
    val normalized = input.trim()
    return when {
        normalized.isEmpty() -> "请输入 API Key"
        normalized.length != AMAP_API_KEY_LENGTH -> "高德 API Key 应为 32 位字符"
        !normalized.all(Char::isLetterOrDigit) -> "API Key 只能包含字母和数字"
        else -> null
    }
}

private const val AMAP_API_KEY_LENGTH = 32
