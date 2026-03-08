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
import com.jme3.asset.AssetNotFoundException
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.manager.LoadingProgressManager
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.time
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.Configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.background.BackgroundController
import org.wysko.midis2jam2.world.background.BackgroundFactory
import org.wysko.midis2jam2.world.background.BackgroundImageFormatException
import org.wysko.midis2jam2.world.background.BackgroundImageMissingException
import kotlin.time.Duration.Companion.seconds

class AndroidPerformanceManager(
    fileName: String,
    val midiFile: TimeBasedSequence,
    val onClose: () -> Unit,
    val sequencer: JwSequencer,
    configs: Collection<Configuration>,
) : PerformanceManager(midiFile, fileName, configs) {
    override val onLoadingProgress: (Float) -> Unit = {
        app.stateManager.getState(LoadingProgressManager::class.java).onLoadingProgress(it)
    }

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        val configuration = configs.find<AppSettingsConfiguration>()
        try {
            BackgroundController.configureBackground(this@AndroidPerformanceManager, configuration, root, Platform.Android)
        } catch (e: BackgroundImageMissingException) {
            logger().warn(e.message ?: "Not all cubemap background images have been assigned.")
            root.attachChild(BackgroundFactory.Default(app.assetManager).create())
        } catch (e: BackgroundImageFormatException) {
            logger().warn(e.message ?: "There was an error loading the images for the background.")
            root.attachChild(BackgroundFactory.Default(app.assetManager).create())
        } catch (e: IllegalArgumentException) {
            logger().warn(
                when (e.message) {
                    "Image width and height must be the same" -> "The background images must be square."
                    else -> e.message ?: "There was an error loading the images for the background."
                }
            )
            root.attachChild(BackgroundFactory.Default(app.assetManager).create())
        } catch (e: AssetNotFoundException) {
            logger().warn("One or more background images could not be found. Please check your background settings.")
            root.attachChild(BackgroundFactory.Default(app.assetManager).create())
        } catch (e: AssetLoadException) {
            logger().warn("There was an error loading the background images.")
            root.attachChild(BackgroundFactory.Default(app.assetManager).create())
        }
        app.camera.fov = configuration.appSettings.cameraSettings.defaultFieldOfView
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