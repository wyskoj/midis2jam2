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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule
import org.wysko.midis2jam2.util.logger
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    @Throws(IOException::class)
    private fun copyAssetToTmpFile(fileName: String): String {
        assets.open(fileName).use { `is` ->
            val tempFileName = "tmp_$fileName"
            openFileOutput(tempFileName, MODE_PRIVATE).use { fos ->
                var bytes_read: Int
                val buffer = ByteArray(4096)
                while ((`is`.read(buffer).also { bytes_read = it }) != -1) {
                    fos.write(buffer, 0, bytes_read)
                }
            }
            Log.d("MainActivity", "Copied asset to temp file: $tempFileName")
            return "$filesDir/$tempFileName"
        }
    }

    external fun helloWorldFluidSynth(soundfontPath: String)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
