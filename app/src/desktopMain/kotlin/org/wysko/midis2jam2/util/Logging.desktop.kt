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

package org.wysko.midis2jam2.util

import org.slf4j.LoggerFactory.getLogger

actual fun <T : Any> T.logger(): JwLogger = DesktopLogger(this::class.simpleName ?: "Unknown")

class DesktopLogger(private val tag: String) : JwLogger {
    private val _logger = getLogger(tag)

    override fun debug(message: String) {
        _logger.debug(message)
    }

    override fun warn(message: String) {
        _logger.warn(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        _logger.error(message, throwable)
    }
}
