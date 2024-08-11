/*
 * Copyright (C) 2024 Jacob Wysko
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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.wysko.midis2jam2.starter.configuration.APPLICATION_CONFIG_HOME
import org.wysko.midis2jam2.util.logger
import java.io.File
import java.util.*

private val LOCALE_FILE = File(APPLICATION_CONFIG_HOME, "locale.txt")

/**
 * Provides internationalization support for the application.
 */
object I18n {

    private val supportedLocales = arrayOf(
        "ar",
        "de",
        "en",
        "es",
        "fi",
        "fr",
        "hi",
        "it",
        "no",
        "pl",
        "ru",
        "th",
        "tl",
        "uk",
        "zh",
    ).map { Locale(it) }

    private var _currentLocale by mutableStateOf(
        run {
            // First, try to load the locale from the file, if it exists.
            var locale = getLocaleFromFile()

            // If the locale is null, try to load the locale from the system.
            // But we can only do this if the locale is supported.
            if (locale == null) {
                val systemLocale = Locale(Locale.getDefault().language)
                if (supportedLocales.contains(systemLocale)) {
                    logger().info("System locale is $systemLocale, supported, using it")
                    locale = systemLocale
                } else {
                    logger().info("System locale is $systemLocale, not supported, not using it")
                }
            }

            // If the locale is still null, just use English.
            locale ?: Locale.ENGLISH.also {
                logger().info("Resorting to English locale")
            }
        }
    )

    private fun getLocaleFromFile(): Locale? {
        if (!LOCALE_FILE.exists()) {
            logger().info("Locale file does not exist")
            return null
        }
        val localeString = try {
            LOCALE_FILE.readText().also {
                logger().info("Read locale file")
            }
        } catch (e: Exception) {
            logger().error("Failed to read locale file", e)
            null
        }
        return try {
            localeString?.let { Locale(it) }
        } catch (e: Exception) {
            logger().error("Failed to parse locale string $localeString", e)
            null
        }
    }

    /**
     * The current [locale][Locale]. All strings currently displayed in the application are in this locale.
     */
    val currentLocale: Locale
        get() = _currentLocale

    private var _strings: MutableState<ResourceBundle> =
        mutableStateOf(getStringsFromResourceBundle(_currentLocale))

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