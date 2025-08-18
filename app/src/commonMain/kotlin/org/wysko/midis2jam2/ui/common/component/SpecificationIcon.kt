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

package org.wysko.midis2jam2.ui.common.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.WorldOneRegular
import org.jetbrains.compose.resources.Font
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification

@Composable
fun SpecificationIcon(
    specification: MidiSpecification,
) {
    SpecificationIconImpl(
        letters = when (specification) {
            MidiSpecification.GeneralMidi -> "GM"
            MidiSpecification.GeneralStandard -> "GS"
            MidiSpecification.ExtendedGeneral -> "XG"
        }
    )
}

@Composable
private fun SpecificationIconImpl(
    letters: String,
) {
    val worldOneRegular = FontFamily(Font(Res.font.WorldOneRegular))
    Text(
        text = letters,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(24.dp),
        fontFamily = worldOneRegular,
        overflow = TextOverflow.Visible,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        softWrap = false,
    )
}