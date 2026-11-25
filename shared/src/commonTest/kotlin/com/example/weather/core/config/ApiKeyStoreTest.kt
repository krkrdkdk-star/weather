package com.example.weather.core.config

import com.example.weather.testutil.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ApiKeyStoreTest {

    @Test
    fun returnsNullBeforeConfiguration() {
        assertNull(ApiKeyStore(InMemoryKeyValueStore()).load())
    }

    @Test
    fun savesNormalizedKey() {
        val store = ApiKeyStore(InMemoryKeyValueStore())

        store.save("  0123456789abcdef0123456789abcdef  ")

        assertEquals("0123456789abcdef0123456789abcdef", store.load())
    }

    @Test
    fun rejectsInvalidKey() {
        val store = ApiKeyStore(InMemoryKeyValueStore())

        assertFailsWith<IllegalArgumentException> {
            store.save("too-short")
        }
    }

    @Test
    fun validatorReturnsUserFacingMessage() {
        assertEquals("请输入 API Key", validateAmapApiKey("  "))
        assertEquals("高德 API Key 应为 32 位字符", validateAmapApiKey("abc"))
        assertEquals(
            "API Key 只能包含字母和数字",
            validateAmapApiKey("0123456789abcdef0123456789abcde-"),
        )
        assertNull(validateAmapApiKey("0123456789abcdef0123456789abcdef"))
    }
}
