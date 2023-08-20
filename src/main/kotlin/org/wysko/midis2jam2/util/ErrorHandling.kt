/*
 * Copyright (C) 2023 Jacob Wysko
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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.slf4j.Logger

object ErrorHandling {
    private val _isShowErrorDialog = mutableStateOf(false)
    val isShowErrorDialog: State<Boolean> = _isShowErrorDialog

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _errorException = mutableStateOf<Throwable?>(null)
    val errorException: State<Throwable?> = _errorException

    fun Logger.errorDisp(message: String, exception: Throwable? = null) {
        _errorMessage.value = message
        _errorException.value = exception
        _isShowErrorDialog.value = true
        this.error(message, exception)
    }

    fun dismiss() {
        _isShowErrorDialog.value = false
    }
}