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

package org.wysko.midis2jam2.ui.queue

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.SaverResultLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.PlatformFiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.QueueExecutionState
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import java.io.File

class QueueTabModel(
    private val applicationService: ApplicationService,
) : ScreenModel {
    private val _queue = MutableStateFlow<List<PlatformFile>>(listOf())
    val queue: StateFlow<List<PlatformFile>>
        get() = _queue

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean>
        get() = _isDirty

    val isPlayButtonEnabled: Flow<Boolean>
        get() = _queue.combine(applicationService.isApplicationRunning) { queue, isRunning ->
            !isRunning && queue.isNotEmpty()
        }

    fun setQueue(queue: List<PlatformFile>) {
        _queue.value = queue
    }

    fun clearQueue() {
        _queue.value = listOf()
    }

    fun setIsDirty(isDirty: Boolean) {
        _isDirty.value = isDirty
    }

    fun removeAtIndex(index: Int) {
        _queue.value = _queue.value.toMutableList().apply {
            removeAt(index)
        }
    }

    @Composable
    fun midiFilePicker(): PickerResultLauncher = rememberFilePickerLauncher(
        type = PickerType.File(MIDI_FILE_EXTENSIONS),
        mode = PickerMode.Multiple(),
        title = "Select MIDI files",
    ) { files ->
        if (files != null) {
            addToQueue(files)
        }
    }

    fun addToQueue(files: PlatformFiles) {
        setQueue((queue.value + files).distinct())
        _isDirty.value = true
    }

    @Composable
    fun queueLoadPicker(displayWarningDialog: (missingFiles: List<String>) -> Unit = {}): PickerResultLauncher =
        rememberFilePickerLauncher(
            type = PickerType.File(listOf("txt")),
            mode = PickerMode.Single,
            title = "Select playlist",
        ) { file ->
            applyQueue(file, displayWarningDialog)
        }

    fun applyQueue(
        file: PlatformFile?,
        displayWarningDialog: (List<String>) -> Unit,
    ) {
        if (file != null) {
            val files = file.file.readText().split("\n").map { PlatformFile(File(it.trim())) }
            val filesIsFound = files.associateWith { it.file.exists() }
            _isDirty.value = false
            setQueue(files.filter { filesIsFound[it] == true })

            if (!filesIsFound.values.all { it }) {
                displayWarningDialog(filesIsFound.filterValues { !it }.keys.map { it.file.absolutePath })
            }
        }
    }

    @Composable
    fun queueSavePicker(onSave: () -> Unit = {}): SaverResultLauncher = rememberFileSaverLauncher {
        _isDirty.value = false
        if (it != null) {
            onSave()
        }
    }

    fun shuffleQueue() {
        _queue.value = _queue.value.shuffled()
        _isDirty.value = true
    }

    fun startApplication() {
        check(_queue.value.isNotEmpty()) { "Queue empty" }

        applicationService.startQueueApplication(QueueExecutionState(_queue.value))
    }
}
