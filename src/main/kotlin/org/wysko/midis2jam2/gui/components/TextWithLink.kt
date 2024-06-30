/*
 * Copyright (C) 2024 Jacob Wysko
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

package org.wysko.midis2jam2.gui.components


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import org.wysko.midis2jam2.util.logger
import java.awt.Desktop
import java.net.URL

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("FunctionName")
@Composable
fun TextWithLink(
    modifier: Modifier = Modifier,
    text: String,
    textToLink: String,
    link: Linkable,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    var isHovered by remember { mutableStateOf(false) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary)
    val linkStyleUnderline = linkStyle.copy(textDecoration = TextDecoration.Underline)

    val formattedText = buildFormattedText(isHovered, text, textToLink, linkStyle, linkStyleUnderline)

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = modifier
    ) {
        ClickableText(
            text = formattedText,
            onClick = { offset ->
                formattedText.getStringAnnotations(tag = "link", start = offset, end = offset).firstOrNull()?.let {
                    browseOrOpen(link)
                }
            },
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
            onTextLayout = { textLayout = it },
            modifier = Modifier.onPointerEvent(PointerEventType.Move) { pointerEvent ->
                processPointerEvent(pointerEvent, textLayout, formattedText) { isHovered = it }
            }.onPointerEvent(PointerEventType.Enter) { pointerEvent ->
                processPointerEvent(pointerEvent, textLayout, formattedText) { isHovered = it }
            }.onPointerEvent(PointerEventType.Exit) {
                isHovered = false
            }.pointerHoverIcon(if (isHovered) Hand else PointerIcon.Default)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("FunctionName")
@Composable
fun TextWithLink(
    modifier: Modifier = Modifier,
    text: String,
    textToLink: String,
    onLinkClick: () -> Unit,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    var isHovered by remember { mutableStateOf(false) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary)
    val linkStyleUnderline = linkStyle.copy(textDecoration = TextDecoration.Underline)

    val formattedText = buildFormattedText(isHovered, text, textToLink, linkStyle, linkStyleUnderline)

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = modifier
    ) {
        ClickableText(
            text = formattedText,
            onClick = { offset ->
                formattedText.getStringAnnotations(tag = "link", start = offset, end = offset).firstOrNull()?.let {
                    onLinkClick()
                }
            },
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
            onTextLayout = { textLayout = it },
            modifier = Modifier.onPointerEvent(PointerEventType.Move) { pointerEvent ->
                processPointerEvent(pointerEvent, textLayout, formattedText) { isHovered = it }
            }.onPointerEvent(PointerEventType.Enter) { pointerEvent ->
                processPointerEvent(pointerEvent, textLayout, formattedText) { isHovered = it }
            }.onPointerEvent(PointerEventType.Exit) {
                isHovered = false
            }.pointerHoverIcon(if (isHovered) Hand else PointerIcon.Default)
        )
    }
}

sealed class Linkable {
    data class URL(val url: String) : Linkable()
    data class File(val file: java.io.File) : Linkable()
}

private fun processPointerEvent(
    it: PointerEvent,
    textLayout: TextLayoutResult?,
    formattedText: AnnotatedString,
    setIsHovered: (Boolean) -> Unit = {}
) {
    it.changes.firstOrNull()?.let { change ->
        setIsHovered(isLinkHovered(textLayout, formattedText, change.position))
    }
}

private fun isLinkHovered(
    textLayout: TextLayoutResult?, formattedText: AnnotatedString, position: Offset
): Boolean {
    textLayout?.let { layout ->
        layout.getOffsetForPosition(position).let { offset ->
            return formattedText.getStringAnnotations(tag = "link", start = offset, end = offset).isNotEmpty()
        }
    }
    return false
}

private fun buildFormattedText(
    isHovered: Boolean, text: String, textToLink: String, linkStyle: SpanStyle, linkStyleUnderline: SpanStyle
): AnnotatedString {
    return buildAnnotatedString {
        val substringBefore = text.substringBefore(textToLink)
        val substringAfter = text.substringAfter(textToLink)

        append(substringBefore)

        pushStringAnnotation(tag = "link", annotation = "")
        withStyle(style = if (isHovered) linkStyleUnderline else linkStyle) {
            append(textToLink)
        }
        pop()

        append(substringAfter)
    }
}

/**
 * Opens or browses a link depending on whether it is a URL or a file path.
 *
 * @param link the link to be opened or browsed
 */
private fun browseOrOpen(link: Linkable) {
    try {
        when (link) {
            is Linkable.URL -> Desktop.getDesktop().browse(URL(link.url).toURI())
            is Linkable.File -> Desktop.getDesktop().open(link.file)
        }
    } catch (e: Throwable) {
        {}.logger().error("Could not open link \"$link\"", e)
    }
}