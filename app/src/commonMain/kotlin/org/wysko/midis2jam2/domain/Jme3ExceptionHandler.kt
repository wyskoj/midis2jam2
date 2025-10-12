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

package org.wysko.midis2jam2.domain

import org.koin.mp.KoinPlatformTools

class Jme3ExceptionHandler(
    private val onUncaught: () -> Unit = {},
) : Thread.UncaughtExceptionHandler {
    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (t?.name == "jME3 Main") {
            errorLogService.addError(
                message = "An unexpected error occurred in the 3D engine.",
                stackTrace = e?.stackTraceToString() ?: "No stacktrace."
            )
            onUncaught()
        }
    }

    companion object {
        fun setup(onUncaught: () -> Unit) {
            // Find the jME3 Main thread and set the handler
            val threads = Array(Thread.activeCount()) { Thread.currentThread() }
            Thread.enumerate(threads)

            for (thread in threads) {
                if (thread.name == "jME3 Main") {
                    thread.uncaughtExceptionHandler = Jme3ExceptionHandler(onUncaught)
                    println("Exception handler set for jME3 Main thread")
                    return
                }
            }

            println("jME3 Main thread not found.")
        }
    }
}