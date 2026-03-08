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

package org.wysko.midis2jam2.starter

import com.jme3.app.SimpleApplication
import com.jme3.system.AppSettings
import org.wysko.midis2jam2.starter.configuration.*

internal fun SimpleApplication.applyConfigurations(configurations: Collection<Configuration>) {
    setSettings(
        AppSettings(false).apply {
            copyFrom(DEFAULT_JME_SETTINGS)
            applyResolution(configurations)
        }
    )
    setDisplayStatView(false)
    setDisplayFps(false)
    isPauseOnLostFocus = false
    isShowSettings = false
}

private fun AppSettings.applyResolution(configurations: Collection<Configuration>) {
    val measuredResolution = screenResolution()
    when {
        configurations.find<Configuration.AppSettingsConfiguration>().appSettings.graphicsSettings.isFullscreen -> {
            isFullscreen = true
            if (measuredResolution != null) {
                this@applyResolution.width = measuredResolution.width
                this@applyResolution.height = measuredResolution.height
            }
        }

        else -> {
            isFullscreen = false
            with(configurations.find<Configuration.AppSettingsConfiguration>().appSettings.graphicsSettings) {
                when (resolutionSettings.isUseDefaultResolution) {
                    true -> {
                        measuredResolution?.let { screenRes ->
                            preferredResolution(screenRes).run {
                                this@applyResolution.width = this.width
                                this@applyResolution.height = this.height
                            }
                        }
                    }

                    false -> with(resolutionSettings) {
                        this@applyResolution.width = resolutionWidth
                        this@applyResolution.height = resolutionHeight
                    }
                }
            }
        }
    }
}

internal fun screenResolution(): Resolution.CustomResolution? = getScreenResolution()

internal fun preferredResolution(screenResolution: Resolution.CustomResolution): Resolution.CustomResolution =
    with(screenResolution) {
        Resolution.CustomResolution((width * 0.95).toInt(), (height * 0.85).toInt())
    }

private val DEFAULT_JME_SETTINGS = AppSettings(true).apply {
    frameRate = -1
    applyScreenFrequency()
    isVSync = true
    isResizable = false
    isGammaCorrection = false
    applyIcons()
    title = "midis2jam2"
    audioRenderer = null
    centerWindow = true
    setUseJoysticks(true)
}

internal expect fun AppSettings.applyIcons()
internal expect fun AppSettings.applyScreenFrequency()
internal expect fun getScreenResolution(): Resolution.CustomResolution?
