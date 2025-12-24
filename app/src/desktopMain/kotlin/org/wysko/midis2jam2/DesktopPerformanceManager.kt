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

package org.wysko.midis2jam2

import Platform
import com.jme3.app.Application
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetLoadException
import org.koin.mp.KoinPlatformTools
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.time
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.background.BackgroundController
import org.wysko.midis2jam2.world.background.BackgroundImageFormatException
import kotlin.time.Duration.Companion.seconds

open class DesktopPerformanceManager(
    val sequencer: JwSequencer,
    val midiFile: TimeBasedSequence,
    val onClose: () -> Unit,
    fileName: String,
    configs: Collection<Configuration>,
) : PerformanceManager(midiFile, fileName, configs) {
    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()
    override val onLoadingProgress: (Float) -> Unit = {}

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        logger().debug("Initializing application...")

        try {
            BackgroundController.configureBackground(
                context = this@DesktopPerformanceManager,
                config = configs.find(),
                root = root,
                Platform.Desktop
            )
        } catch (e: BackgroundImageFormatException) {
            errorLogService.addError(
                e.message ?: "There was an error loading the images for the background.",
                e.stackTraceToString()
            )
        } catch (e: IllegalArgumentException) {
            errorLogService.addError(
                message = when (e.message) {
                    "Image width and height must be the same" -> "The background images must be square."
                    else -> e.message ?: "There was an error loading the images for the background."
                },
                e.stackTraceToString()
            )
        } catch (e: AssetLoadException) {
            errorLogService.addError(
                message = "There was an error loading the background images. Did you remember to assign them in the settings?",
                e.stackTraceToString()
            )
        }

        logger().debug("Application initialized")
    }

    override fun cleanup() {
        logger().debug("Cleaning up...")
        sequencer.run {
            stop()
            close()
        }
        onClose()
        logger().debug("Cleanup complete")
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        val delta = tpf.toDouble().seconds
        instruments.forEach { it.tick(app.time, delta) }
    }
}
