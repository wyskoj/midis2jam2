/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.gui

import org.wysko.midis2jam2.util.logger
import java.util.Locale
import java.util.ResourceBundle

/**
 * Provides an easy means for delivering internationalized strings throughout the application.
 */
object Internationalization {
    /**
     * The current locale [i18n] should be delivering strings in.
     */
    var locale: Locale = Locale.forLanguageTag(launcherState.getProperty("locale")).also {
        Locale.setDefault(it)
    }
        set(value) {
            i18n = ResourceBundle.getBundle("i18n.midis2jam2", value)
            Locale.setDefault(locale)
            field = value
            Internationalization.logger().info("Locale changed to $value")
        }

    /* *** THESE TWO VARIABLES MUST BE NOT BE REORDERED!!! *** */

    /**
     * Provides internationalization strings throughout the application. If the [locale] is changed, this object will load
     * strings in the new locale.
     */
    var i18n: ResourceBundle = ResourceBundle.getBundle("i18n.midis2jam2")
        private set
}

/**
 * Makes nice syntax for obtaining a string from [i18n].
 */
operator fun ResourceBundle.get(key: String): String = this.getString(key)
