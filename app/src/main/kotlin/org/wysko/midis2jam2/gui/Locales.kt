package org.wysko.midis2jam2.gui

import java.util.*

val SUPPORTED_LOCALES = listOf(
    "ar", "de", "en", "es", "fi", "fr", "hi", "it", "no", "pl", "ru", "th", "tl", "uk", "zh",
).map { Locale.Builder().setLanguage(it).build() }