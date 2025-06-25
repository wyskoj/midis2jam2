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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.provider.Settings
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.*

actual class SystemInteractionService : KoinComponent {
    actual fun openFolder(folder: File) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(EXTRA_INITIAL_URI, folder.toUri())
        context().startActivity(intent)
    }

    actual fun openSystemLanguageSettings() {
        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", "org.wysko.midis2jam2", null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context().startActivity(intent)
    }

    actual fun getLocale(): Locale {
        return context().resources.configuration.locales.get(0)
    }

    private fun context(): Context {
        val context: Context by inject()
        return context
    }
}