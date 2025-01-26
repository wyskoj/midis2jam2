package org.wysko.midis2jam2.settings.category

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.settings.SettingsKeys

class BackgroundSettings(private val settings: Settings) {
    private var _configurationType = MutableStateFlow(
        runCatching {
            settings.getStringOrNull(SettingsKeys.Background.CONFIGURATION_TYPE)?.let {
                ConfigurationType.valueOf(it)
            }
        }.getOrNull() ?: ConfigurationType.Default
    )
    val configurationType: StateFlow<ConfigurationType>
        get() = _configurationType

    private var _repeatedCubeMapTexture = MutableStateFlow(
        settings.getString(SettingsKeys.Background.REPEATED_CUBE_MAP_TEXTURE, "")
    )
    val repeatedCubeMapTexture: StateFlow<String>
        get() = _repeatedCubeMapTexture

    private var _uniqueCubeMapTextures = MutableStateFlow(
        runCatching {
            settings.getStringOrNull(SettingsKeys.Background.UNIQUE_CUBE_MAP_TEXTURES)
                ?.let { Json.decodeFromString<List<String>>(it) }
        }.getOrNull() ?: List(6) { "" }
    )
    val uniqueCubeMapTextures: StateFlow<List<String>>
        get() = _uniqueCubeMapTextures

    private var _color = MutableStateFlow(settings.getInt(SettingsKeys.Background.COLOR, Color.Black.toArgb()))
    val color: StateFlow<Int>
        get() = _color

    fun setConfigurationType(value: ConfigurationType) {
        _configurationType.value = value
        settings.putString(SettingsKeys.Background.CONFIGURATION_TYPE, value.name)
    }

    fun setRepeatedCubeMapTexture(value: String) {
        _repeatedCubeMapTexture.value = value
        settings.putString(SettingsKeys.Background.REPEATED_CUBE_MAP_TEXTURE, value)
    }

    fun setUniqueCubeMapTextures(value: List<String>) {
        _uniqueCubeMapTextures.value = value
        settings.putString(SettingsKeys.Background.UNIQUE_CUBE_MAP_TEXTURES, Json.encodeToString(value))
    }

    fun setColor(value: Int) {
        _color.value = value
        settings.putInt(SettingsKeys.Background.COLOR, value)
    }

    enum class ConfigurationType {
        Default, RepeatedCubeMap, UniqueCubeMap, Color
    }
}