/*
 * Copyright (C) 2025 Jacob Wysko
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

@file:UseSerializers(FileAsStringSerializer::class)

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.gui.viewmodel.GERVILL
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.util.FileAsStringSerializer
import org.wysko.midis2jam2.util.logger
import java.io.File
import java.util.*

private val resolutionRegex = Regex("""RES_(\d+)_(\d+)""")

object LegacyConfigurationImporter {
    fun importLegacyConfiguration(): Collection<Configuration?> {
        val configs = mutableSetOf<Configuration?>()

        // Read data from old .properties files
        val launcherPropertiesFile = File(APPLICATION_CONFIG_HOME, "launcher.properties")
        if (launcherPropertiesFile.exists()) {
            try {
                val launcherProperties = Properties().apply { load(launcherPropertiesFile.inputStream()) }
                try {
                    launcherProperties.getProperty("soundfonts")?.let { soundfonts ->
                        configs += SoundbankConfiguration(
                            soundbanks = Json.decodeFromString<List<String>>(soundfonts).map { File(it) }.toSet()
                        )
                    }
                } catch (e: Exception) {
                    logger().warn("Failed to import SoundFonts from legacy properties.", e)
                }
                launcherProperties.getProperty("locale")?.let {
                    I18n.setLocale(Locale(it))
                }
                HomeConfiguration(
                    lastMidiFileSelectedDirectory = launcherProperties.getProperty("lastdir"),
                    selectedMidiDevice = launcherProperties.getProperty("midi_device") ?: GERVILL
                )
            } catch (e: Exception) {
                logger().warn("Failed to load legacy launcher.properties.", e)
            }
        }

        val settingsPropertiesFile = File(APPLICATION_CONFIG_HOME, "settings.properties")
        if (settingsPropertiesFile.exists()) {
            try {
                val settingsProperties = Properties().apply {
                    load(settingsPropertiesFile.inputStream())
                }
                val autoAutocam: String? = settingsProperties.getProperty("auto_autocam")
                val neverHidden: String? = settingsProperties.getProperty("never_hidden")
                val fullscreen: String? = settingsProperties.getProperty("fullscreen")
                val showHud: String? = settingsProperties.getProperty("show_hud")
                val lyrics: String? = settingsProperties.getProperty("lyrics")

                configs += SettingsConfiguration(
                    isFullscreen = fullscreen?.toBoolean() ?: false,
                    startAutocamWithSong = autoAutocam?.toBoolean() ?: false,
                    showHud = showHud?.toBoolean() ?: true,
                    showLyrics = lyrics?.toBoolean() ?: true,
                    instrumentsAlwaysVisible = neverHidden?.toBoolean() ?: false
                )
            } catch (e: Exception) {
                logger().warn("Failed to load legacy settings.properties.", e)
            }
        }

        val graphicsPropertiesFile = File(APPLICATION_CONFIG_HOME, "graphics.properties")
        if (graphicsPropertiesFile.exists()) {
            try {
                val graphicsProperties = Properties().apply {
                    load(graphicsPropertiesFile.inputStream())
                }
                val shadows: String? = graphicsProperties.getProperty("shadows")
                val resolution: String? = graphicsProperties.getProperty("resolution")
                val antialiasing: String? = graphicsProperties.getProperty("antialiasing")
                configs += GraphicsConfiguration(
                    windowResolution = resolution?.let { resolutionString ->
                        resolutionRegex.matchEntire(resolutionString)?.let {
                            Resolution.CustomResolution(
                                it.groupValues[1].toInt(),
                                it.groupValues[2].toInt()
                            )
                        } ?: Resolution.DefaultResolution
                    } ?: Resolution.DefaultResolution,
                    shadowQuality = shadows?.let { QualityScale.valueOf(it) } ?: QualityScale.MEDIUM,
                    antiAliasingQuality = antialiasing?.let { QualityScale.valueOf(it) } ?: QualityScale.LOW
                )
            } catch (e: Exception) {
                logger().warn("Failed to load legacy graphics.properties.", e)
            }
        }


        val backgroundPropertiesFile = File(APPLICATION_CONFIG_HOME, "background.properties")
        if (backgroundPropertiesFile.exists()) {
            try {
                val backgroundProperties = Properties().apply {
                    load(backgroundPropertiesFile.inputStream())
                }
                val type: String? = backgroundProperties.getProperty("type")
                val value: String? = backgroundProperties.getProperty("value")

                configs += when (type) {
                    "DEFAULT" -> BackgroundConfiguration.DefaultBackground
                    "REPEATED_CUBEMAP" -> BackgroundConfiguration.RepeatedCubeMapBackground(texture = value!!)
                    "UNIQUE_CUBEMAP" -> {
                        val textures = Json.decodeFromString<List<String>>(value!!)
                        BackgroundConfiguration.UniqueCubeMapBackground(
                            CubemapTexture(
                                west = textures[0],
                                east = textures[1],
                                north = textures[2],
                                south = textures[3],
                                up = textures[4],
                                down = textures[5]
                            )
                        )
                    }

                    "COLOR" -> BackgroundConfiguration.ColorBackground(value!!.toInt())
                    else -> null
                }
            } catch (e: Exception) {
                logger().warn("Failed to load legacy background.properties.", e)
            }
        }

        // Delete old .properties files, we don't need them anymore
        launcherPropertiesFile.delete()
        settingsPropertiesFile.delete()
        graphicsPropertiesFile.delete()
        backgroundPropertiesFile.delete()

        return configs
    }
}