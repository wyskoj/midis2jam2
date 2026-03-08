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

import com.install4j.api.launcher.StartupNotification
import io.github.vinceglb.filekit.core.PlatformFile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.util.isMacOs
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object StartupListenerService : KoinComponent {

    private var isListenerRegistered = false
    private var fileAssociationUsed = false
    private val startupLatch = CountDownLatch(1)

    fun registerStartupListener() {
        if (!isMacOs() || isListenerRegistered) return

        StartupNotification.registerStartupListener { parameters ->
            handleFileOpenEvent(parameters)
        }
        isListenerRegistered = true

        // Start a background thread to count down the latch after a timeout
        // This ensures we don't wait forever if no file association event occurs
        Thread {
            try {
                Thread.sleep(100) // Give a brief moment for any events
                if (startupLatch.count > 0) {
                    startupLatch.countDown()
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.start()
    }

    fun wasFileAssociationUsed(): Boolean {
        if (!isMacOs()) return false

        // Wait briefly for any file association events to be processed
        // This prevents race conditions where the listener hasn't processed yet
        try {
            startupLatch.await(200, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        return fileAssociationUsed
    }

    private fun handleFileOpenEvent(parameters: String) {
        if (parameters.isBlank()) {
            startupLatch.countDown()
            return
        }

        val file = File(parameters)
        if (!file.exists() || !file.isFile) {
            println("Invalid file path provided: $parameters")
            startupLatch.countDown()
            return
        }

        // Mark that file association was used
        fileAssociationUsed = true

        val applicationService: ApplicationService by inject()
        val midiFile = PlatformFile(file)

        // Use the shared method from CmdStart to start the application
        CmdStart.startApplicationWithFile(applicationService, midiFile)
        startupLatch.countDown()
    }
}
