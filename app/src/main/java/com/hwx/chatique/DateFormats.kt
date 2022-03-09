package com.hwx.chatique

import java.text.SimpleDateFormat
import java.util.*

object DateFormats {
    private val locale: Locale = Locale.getDefault() ?: Locale.US

    val ddMMHHmm = SimpleDateFormat("dd.MM.YYYY HH:mm:ss", locale)
}