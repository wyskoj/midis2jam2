/*
 * Copyright (C) 2023 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.gui.viewmodel

import androidx.compose.runtime.*
import org.wysko.midis2jam2.starter.configuration.APPLICATION_CONFIG_HOME
import org.wysko.midis2jam2.util.logger
import java.io.File
import java.util.*

private val LOCALE_FILE = File(APPLICATION_CONFIG_HOME, "locale.txt")

/**
 * Provides internationalization support for the application.
 */
object I18n {
    private var _currentLocale by mutableStateOf(
        try {
            if (LOCALE_FILE.exists()) {
                Locale(LOCALE_FILE.readText())
            } else {
                Locale(Locale.getDefault().language)
            }
        } catch (e: Exception) {
            logger().error("Failed to load locale from file.", e)
            Locale.getDefault()
        }
    )

    /**
     * The current [locale][Locale]. All strings currently displayed in the application are in this locale.
     */
    val currentLocale: Locale
        get() = _currentLocale

    private var _strings: MutableState<ResourceBundle> = mutableStateOf(getStringsFromResourceBundle(_currentLocale))

    private val supportedLocales = arrayOf(
        "en",
        "es",
        "fi",
        "fr",
        "no",
        "ru",
        "th",
        "tl",
        "zh",
    ).map { Locale(it) }

    /**
     * Gets the string associated with the given key.
     */
    operator fun get(key: String): State<String> = derivedStateOf { _strings.value.getString(key) }

    private fun getStringsFromResourceBundle(locale: Locale): ResourceBundle {
        return ResourceBundle.getBundle("i18n.midis2jam2", locale)
    }

    /**
     * Sets the current locale to the given locale.
     */
    fun setLocale(locale: Locale) {
        _currentLocale = locale
        _strings.value = getStringsFromResourceBundle(_currentLocale)
        LOCALE_FILE.writeText(locale.toString())
    }

    /**
     * Increments the current locale to the next locale in the list of supported locales.
     */
    fun incrementLocale() {
        val index = supportedLocales.indexOf(_currentLocale)
        setLocale(supportedLocales[(index + 1) % supportedLocales.size])
    }
}