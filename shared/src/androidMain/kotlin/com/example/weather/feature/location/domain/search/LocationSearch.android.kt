package com.example.weather.feature.location.domain.search

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

actual fun locationSearchTokens(name: String): Set<String> = buildLocationSearchTokens(name)

private fun buildLocationSearchTokens(name: String): Set<String> {
    val normalized = name.lowercase().replace(" ", "")
    val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }
    val syllables = name.map { char ->
        runCatching { PinyinHelper.toHanyuPinyinStringArray(char, format)?.firstOrNull() }
            .getOrNull()
            ?: char.lowercase()
    }
    return setOf(normalized, syllables.joinToString(""), syllables.joinToString("") { it.take(1) })
}
