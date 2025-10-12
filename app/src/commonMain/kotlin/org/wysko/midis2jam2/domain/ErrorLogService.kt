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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ErrorLogService {
    private val _errors = MutableStateFlow<List<ErrorLogEntry>>(listOf())
    val errors: StateFlow<List<ErrorLogEntry>>
        get() = _errors

    private val _mapIsRead = MutableStateFlow<Map<ErrorLogEntry, Boolean>>(mapOf())
    val mapIsRead: StateFlow<Map<ErrorLogEntry, Boolean>>
        get() = _mapIsRead

    val unreadCount = _mapIsRead.map { map -> map.values.count { !it } }

    fun addError(message: String, stackTrace: String) {
        val newEntry = ErrorLogEntry(System.currentTimeMillis(), message, stackTrace)
        _errors.value += newEntry
        _mapIsRead.value += (newEntry to false)
    }

    fun removeError(entry: ErrorLogEntry) {
        _errors.value -= entry
        _mapIsRead.value -= entry
    }

    fun markAllAsRead() {
        _mapIsRead.value = _mapIsRead.value.mapValues { true }
    }

    fun clearAll() {
        _errors.value = listOf()
        _mapIsRead.value = mapOf()
    }
}

class ErrorLogEntry(
    val timestamp: Long,
    val message: String,
    val stackTrace: String,
)