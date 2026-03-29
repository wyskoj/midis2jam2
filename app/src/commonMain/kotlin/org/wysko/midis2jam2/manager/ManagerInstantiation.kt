/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.manager

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.Configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.starter.getCameraManager

fun instantiateManagers(
    configurations: Collection<Configuration>,
    sequence: TimeBasedSequence,
    sequencer: JwSequencer,
    isQueueApplication: Boolean = false,
    onPlaybackComplete: (() -> Unit)? = null,
): List<BaseManager> {
    val settings = configurations.find<AppSettingsConfiguration>()
    val isLooping = configurations.find<Configuration.HomeConfiguration>().isLooping

    val managers = buildList {
        if (!settings.appSettings.graphicsSettings.shadowsSettings.isUseShadows) {
            add(FakeShadowsManager())
        }
        add(PreferencesManager(settings.appSettings))
        add(ActionsManager())
        add(getCameraManager())
        add(CollectorsManager())
        add(DebugTextManager().apply { isEnabled = false })
        add(DrumSetVisibilityManager())
        add(FadeManager())
        add(HudManager())
        add(StageManager())
        add(StandManager())
        add(
            PlaybackManager(
                sequence = sequence,
                sequencer = sequencer,
                isLooping = isLooping && !isQueueApplication,
                onPlaybackComplete = onPlaybackComplete,
            )
        )
    }

    return managers
}
