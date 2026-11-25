package com.example.weather.feature.location.domain.search

/** 返回地点名称的检索词：原文、小写拼音全拼和拼音首字母。 */
expect fun locationSearchTokens(name: String): Set<String>

fun matchesLocationQuery(name: String, query: String): Boolean {
    val normalizedQuery = query.trim().lowercase().replace(" ", "")
    if (normalizedQuery.isEmpty()) return true
    return locationSearchTokens(name).any { token -> normalizedQuery in token }
}
