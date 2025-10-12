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

/**
 * Service to handle startup notifications, particularly for macOS file associations.
 * This allows the application to respond to file open events when launched from Finder.
 */
object StartupListenerService : KoinComponent {

    private var isListenerRegistered = false
    private var fileAssociationUsed = false
    private val startupLatch = CountDownLatch(1)

    /**
     * Registers a startup listener for macOS file associations.
     * This should be called early in the application lifecycle.
     */
    fun registerStartupListener() {
        if (!isMacOs() || isListenerRegistered) return

        try {
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
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        } catch (e: Exception) {
            // Log error but don't fail the application
            println("Failed to register startup listener: ${e.message}")
            startupLatch.countDown()
        }
    }

    /**
     * Checks if a file association was used to launch the application.
     * This should be called before starting the UI to determine if it should be shown.
     * Waits for a short period to allow any file association events to be processed.
     */
    fun wasFileAssociationUsed(): Boolean {
        if (!isMacOs()) return false

        // Wait briefly for any file association events to be processed
        // This prevents race conditions where the listener hasn't processed yet
        try {
            startupLatch.await(200, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        return fileAssociationUsed
    }

    /**
     * Handles file open events from macOS file associations.
     * This method processes the file path and starts the application with the file.
     */
    private fun handleFileOpenEvent(parameters: String) {
        if (parameters.isBlank()) {
            startupLatch.countDown()
            return
        }

        try {
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
        } catch (e: Exception) {
            println("Error handling file open event: ${e.message}")
        } finally {
            startupLatch.countDown()
        }
    }
}
