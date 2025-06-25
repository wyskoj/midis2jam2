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

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule

class Midis2jam2App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Midis2jam2App)
            modules(applicationModule, midiSystemModule, systemModule, uiModule)
        }
    }

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
