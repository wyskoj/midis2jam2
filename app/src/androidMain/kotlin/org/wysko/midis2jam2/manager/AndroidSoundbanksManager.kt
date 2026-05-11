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

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.copyTo
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val soundbanksModule: Module = module {
    single<AndroidSoundbanksManager> { AndroidSoundbanksManager(androidContext()) }
}

private const val COPY_BUFFER_SIZE = 8192L

class AndroidSoundbanksManager(
    androidContext: Context
) {
    private val soundbanksDir = File(androidContext.filesDir, "soundbanks").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
    private val _jobs = MutableStateFlow<MutableList<SoundbankImportJob>>(mutableListOf())

    val jobs: StateFlow<List<SoundbankImportJob>>
        get() = _jobs

    private val _soundbanks = MutableStateFlow(listSoundbanks())
    val soundbanks: StateFlow<List<PlatformFile>>
        get() = _soundbanks

    fun importSoundbanks(files: List<PlatformFile>) {
        files.forEach {
            if (it.extension.lowercase() == "sf2") {
                copyPlatformFile(it, File(soundbanksDir, it.name))
            }
        }
    }

    private fun listSoundbanks(): List<PlatformFile> {
        return soundbanksDir.listFiles()?.toList()?.map {
            PlatformFile(it.toUri())
        } ?: run {
            Log.w("AndroidSoundbanksManager", "Could not list files in internal soundbanks directory.")
            emptyList()
        }
    }

    fun deleteSoundbank(name: String) {
        File(soundbanksDir, name).delete()
        refreshSoundbanksList()
    }

    private fun copyPlatformFile(platformFile: PlatformFile, destination: File) {
        val copyJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = Buffer()
            val outStream = destination.outputStream()
            platformFile.source().use { bufferedSource ->
                outStream.use { out ->
                    var bytesRead: Long
                    do {
                        bytesRead = bufferedSource.readAtMostTo(buffer, COPY_BUFFER_SIZE)
                        buffer.copyTo(out)
                        buffer.clear()
                    } while (bytesRead != -1L)
                }
            }
            refreshSoundbanksList()
        }
        _jobs.getAndUpdate {
            (it + SoundbankImportJob(copyJob, destination.name)).toMutableList()
        }
        copyJob.invokeOnCompletion {
            refreshActiveJobs()
        }
    }

    private fun refreshActiveJobs() {
        _jobs.update { jobs -> jobs.filter { (job, _) -> job.isActive }.toMutableList() }
    }

    private fun refreshSoundbanksList() {
        _soundbanks.update { listSoundbanks() }
    }

    data class SoundbankImportJob(
        val job: Job,
        val name: String,
    )
}