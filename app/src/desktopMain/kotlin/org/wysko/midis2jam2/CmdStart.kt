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

import com.install4j.api.launcher.SplashScreen
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.domain.ExecutionState
import java.io.File

object CmdStart : KoinComponent {
    fun start(args: Array<String>) {
        if (args.isEmpty()) return

        val applicationService: org.wysko.midis2jam2.domain.ApplicationService by inject()
        val midiFile = PlatformFile(File(args.first()))

        applicationService.startApplication(ExecutionState(midiFile))
        try {
            SplashScreen.hide()
        } catch (_: Exception) {}
        runBlocking {
            applicationService.isApplicationRunning.first { !it }
        }
    }
}
