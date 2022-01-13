/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.world

import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.font.Rectangle
import com.jme3.math.ColorRGBA
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiTextEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.cullHint

/**
 * The LyricController is responsible for controlling the displaying of lyrics. There are two lines of lyrics, the top
 * line displays the current lyric and the bottom line displays the next lyric.
 */
class LyricController(
    /** The list of lyric events */
    lyricEvents: List<MidiTextEvent>,

    /** Context to midis2jam2 */
    val context: Midis2jam2
) {

    /** Contains each lyric "line", where each syllable is an individual event. */
    private val lyricGroups = ArrayList<List<MidiTextEvent>>()

    /** As the file progresses, the current lyric group is incremented and put here. */
    private var currentGroup: List<MidiTextEvent> = emptyList()

    /** The previous lyric group. */
    private var previousGroup: List<MidiTextEvent> = emptyList()

    /** Font used to display lyrics. */
    private val bitmapFont = this.context.assetManager.loadFont("Interface/Fonts/Default.fnt")

    /** The GUI text element to display the current lyric. */
    private val currentLine: BitmapText = BitmapText(bitmapFont, false).apply {
        size = bitmapFont.charSet.renderedSize.toFloat() * 1.5f
        context.app.guiNode.attachChild(this)
        setBox(
            Rectangle(
                0f, context.app.viewPort.camera.height * 0.9f, context.app.viewPort.camera.width.toFloat(), 100f
            )
        )
        color = ColorRGBA.DarkGray
        alignment = BitmapFont.Align.Center
    }

    /** The GUI text element to display the next lyric. */
    private val nextLine: BitmapText = BitmapText(bitmapFont, false).apply {
        size = bitmapFont.charSet.renderedSize.toFloat()
        context.app.guiNode.attachChild(this)
        setBox(
            Rectangle(
                0f, context.app.viewPort.camera.height * 0.9f - 40f, context.app.viewPort.camera.width.toFloat(), 100f
            )
        )
        color = ColorRGBA.DarkGray
        alignment = BitmapFont.Align.Center
    }

    /** The index of the current syllable in the current lyric group. */
    private var currentWordIndex: Int = 0

    /** The number of syllables in the current lyric group. */
    private var lineWordCount: Int = 0

    /** Time of the next lyric group start. */
    private fun tns() = context.file.eventInSeconds(lyricGroups.first().first())

    /** Time of the current lyric group end. */
    private fun tce() = currentGroup.lastOrNull()?.let { context.file.eventInSeconds(it) } ?: 0.0

    /** The full text of the current lyric. */
    private val currentLineText: String
        get() = currentGroup.joinToString("") { it.text.stripped() }

    /** The full text of the next lyric. */
    private val nextLineText: String
        get() = lyricGroups.firstOrNull()?.let { group -> group.joinToString("") { it.text.stripped() } } ?: ""

    init {
        val stripped = lyricEvents.filter { !it.text.startsWith("@") } as ArrayList<MidiTextEvent>
        /* Group lyrics events by new lines */
        var group = ArrayList<MidiTextEvent>()
        while (stripped.isNotEmpty()) {
            val first = stripped.removeFirst()
            if (first.text.startsWith("\\") || first.text.startsWith("/")) {
                if (group.isNotEmpty()) lyricGroups.add(group)
                group = ArrayList()
            }
            group.add(first)
        }
        if (group.isNotEmpty()) lyricGroups.add(group) // Add the last group
    }

    /** Updates the lyric display. */
    fun tick(time: Double) {
        setLyricsVisibility(time)

        /* If it is time to display this lyric group */
        if (lyricGroups.isNotEmpty() && shouldAdvance(time)) {
            /* Store last group */
            previousGroup = currentGroup

            /* Remove it from the list */
            currentGroup = lyricGroups.removeAt(0)

            /* Write down the number of words in this group */
            lineWordCount = currentGroup.size

            /* Reset the word index */
            currentWordIndex = -1

            /* Set the text of the current line */
            currentLine.text = currentLineText
        }

        /* If there are words in the current group */
        if (currentGroup.isNotEmpty()) {
            /* If it is time to display the next word */
            while (currentWordIndex < lineWordCount - 1 && context.file.eventInSeconds(
                    currentGroup[currentWordIndex + 1].time
                ) - time < 0.0
            ) {
                /* Increment the word index */
                currentWordIndex++
            }
            /* Color the words up to the current word */
            currentLine.setColor(
                0, currentGroup.map { it.text.stripped() }.charCountUpTo(currentWordIndex), ColorRGBA.White
            )
        }

        /* Set the text position */
        currentLine.setBox(
            Rectangle(
                0f, context.app.viewPort.camera.height * 0.9f, context.app.viewPort.camera.width.toFloat(), 100f
            )
        )

        nextLine.setBox(
            Rectangle(
                0f, context.app.viewPort.camera.height * 0.9f - 40f, context.app.viewPort.camera.width.toFloat(), 100f
            )
        )
        nextLine.text = nextLineText
    }

    private fun setLyricsVisibility(time: Double) {
        calculateLyricsVisibility(time).cullHint().let {
            currentLine.cullHint = it
            nextLine.cullHint = it
        }
    }

    private fun calculateLyricsVisibility(time: Double): Boolean {
        /* Within one second of the next lyric group? Visible. */
        if (lyricGroups.isNotEmpty() && tns() - time <= 1.0) {
            return true
        }

        /* If within a 7-second gap between lyric groups? Visible. */
        if (lyricGroups.isNotEmpty() && tns() - tce() <= 7.0) {
            return true
        }

        /* If after 2 seconds of the previous lyric group? Visible. */
        if (previousGroup.isNotEmpty() && time - tce() <= 2.0) {
            return true
        }

        if (lyricGroups.isEmpty()
            && currentGroup.isNotEmpty()
            && time in (context.file.eventInSeconds(currentGroup.first()) - 1)
            ..(context.file.eventInSeconds(currentGroup.last()) + 2)
        ) {
            return true
        }

        return false
    }

    /** Determines if the current lyric group should be advanced. See [this flowchart](https://app.code2flow.com/Y5F13y). */
    private fun shouldAdvance(time: Double): Boolean {
        return if (currentGroup.isNotEmpty()) {
            if (tns() - tce() > 2) {
                tns() - time < 1
            } else {
                time > (tce() + tns()) / 2
            }
        } else {
            tns() - time < 1
        }
    }
}

/** Given a string, removes '/' and '\' characters if it starts with one of those. */
private fun String.stripped(): String {
    return this.removePrefix("/").removePrefix("\\")
}

/** Given a list of strings and a [max] index, returns the number of characters in the list up to the [max] index. */
private fun List<String>.charCountUpTo(max: Int): Int {
    if (max == -1) return 0
    return (0..max).sumOf { this[it].length }
}
