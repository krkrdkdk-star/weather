package com.example.weather.core.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter

actual fun platformDateProvider(): DateProvider = DateProvider {
    LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
}
