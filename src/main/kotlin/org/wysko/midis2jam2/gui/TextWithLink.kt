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

package org.wysko.midis2jam2.gui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import org.wysko.midis2jam2.util.ThrowableDisplay.display
import java.awt.Desktop
import java.net.URL

/**
 * [Composable] that displays a text with a link. When the user clicks on the link, the link is opened in the default browser.
 */
@Suppress("FunctionName")
@ExperimentalComposeUiApi
@Composable
fun TextWithLink(
    /** The full content of the text. */
    text: String,
    /** The portion of the text that is a link. */
    textToLink: String,
    /** The URL to open when the user clicks on the link. */
    url: String,
    /** The text style to use for the text. */
    style: TextStyle = MaterialTheme.typography.body1
) {
    return Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.substringBefore(textToLink),
            style = style
        )
        ClickableText(
            text = buildAnnotatedString {
                append(textToLink)
                addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = 0,
                    end = textToLink.length
                )
                addStyle(
                    style = SpanStyle(
                        color = Color(0xff64B5F6),
                        textDecoration = TextDecoration.Underline
                    ),
                    start = 0,
                    end = textToLink.length
                )
            },
            onClick = {
                try {
                    Desktop.getDesktop().browse(URL(url).toURI())
                } catch (e: Throwable) {
                    e.display("Cannot open link", "There was an error opening this link.")
                }
            },
            style = style,
            modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand, true)
        )
        Text(
            text = text.substringAfter(textToLink),
            style = style
        )
    }
}
