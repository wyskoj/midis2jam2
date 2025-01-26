package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.gui.SUPPORTED_LOCALES
import org.wysko.midis2jam2.gui.material.Theme
import org.wysko.midis2jam2.settings.SettingsKeys
import java.util.*

private val ENGLISH_LOCALE = Locale.Builder().setLanguage("en").build()

class GeneralSettings(private val settings: Settings) {
    private val locales = SUPPORTED_LOCALES

    private val _locale = MutableStateFlow(getDefaultLocale())
    val locale: StateFlow<Locale>
        get() = _locale

    private val _theme = MutableStateFlow(runCatching {
        settings.getStringOrNull(SettingsKeys.General.THEME)?.let {
            Theme.valueOf(it)
        }
    }.getOrNull() ?: Theme.System)
    val theme: StateFlow<Theme>
        get() = _theme

    fun getAvailableLocales(): List<Locale> = locales

    fun setLocale(locale: Locale) {
        _locale.value = locale
        settings.putString(SettingsKeys.General.LOCALE, locale.toLanguageTag())
    }

    fun setTheme(theme: Theme) {
        _theme.value = theme
        settings.putString(SettingsKeys.General.THEME, theme.name)
    }

    private fun getDefaultLocale(): Locale =
        settings.getStringOrNull(SettingsKeys.General.LOCALE)?.let { storedLocale ->
            Locale.forLanguageTag(storedLocale)
        } ?: run {
            locales.firstOrNull { it.language == Locale.getDefault().language } ?: ENGLISH_LOCALE
        }
}