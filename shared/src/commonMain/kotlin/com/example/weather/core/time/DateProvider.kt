package com.example.weather.core.time

/**
 * 提供“今天”的本地日期键，供“同一天内只查询一次”的缓存策略使用。
 *
 * 使用 fun interface 便于在测试中注入固定日期。
 */
fun interface DateProvider {
    /** 返回本地时区下今天的日期，格式 yyyyMMdd。 */
    fun todayKey(): String
}

/** 平台默认的日期提供者。 */
expect fun platformDateProvider(): DateProvider
