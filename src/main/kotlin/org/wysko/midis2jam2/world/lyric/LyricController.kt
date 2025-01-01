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

package org.wysko.midis2jam2.world.lyric

import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.font.Rectangle
import com.jme3.math.ColorRGBA
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.plusAssign
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A controller for displaying lyrics.
 *
 * @property context The context to the main class.
 * @property events The list of the text events.
 */
class LyricController(val context: Midis2jam2, val events: List<MetaEvent.Lyric>) {

    private val font = context.assetManager.loadFont("Assets/Fonts/Inter.fnt")

    private val words = events.filter { !separators.contains(it.text) }
    private val lines = events.partitionByNewLines()

    private var currentWord: MetaEvent.Lyric? = null
    private var currentLine: LyricLine? = null

    private val wordCollector = EventCollector(context, words, onSeek = { currentWord = it.prev() })
    private val lineCollector = LyricLineCollector(context, lines, onSeek = {
        currentLine = it.prev()
    }, triggerCondition = { line: LyricLine, time: Duration ->
        with(context) {
            currentLine?.let { currentLine ->
                if (line.startTime - currentLine.endTime < 3.seconds) {
                    time > ((line.startTime - currentLine.endTime) * 0.8) + currentLine.endTime
                } else {
                    line.startTime - time < 2.seconds
                }
            } ?: let {
                time >= line.startTime - 2.seconds
            }
        }
    })

    private val opacity = NumberSmoother(0f, 5.0)
    private var isVisible = false
    private val texts = lines.associateWith {
        BitmapText(font).apply {
            setBox(
                Rectangle(
                    0f,
                    context.app.viewPort.camera.height * 0.85f,
                    context.app.viewPort.camera.width.toFloat(),
                    100f
                )
            )
            setSize(64f)
            color = ColorRGBA.DarkGray
            text = it.renderString()
            alignment = BitmapFont.Align.Center
        }
    }.onEach { context.app.guiNode += it.value }
    private val linePositionCtrl = lines.associateWith { NumberSmoother(0.8f, 10.0) }

    /**
     * Updates the controller.
     *
     * @param time The current time.
     * @param delta The time since the last update.
     */
    fun tick(time: Duration, delta: Duration) {
        with(context) {
            lineCollector.advanceCollect(time)?.let {
                currentLine = it
                (texts[currentLine] ?: return@let).color = ColorRGBA.DarkGray
            }
            wordCollector.advanceCollectOne(time)?.let { currentWord = it }

            texts.forEach { (line, text) ->
                linePositionCtrl[line]?.tick(delta) {
                    when {
                        lines.indexOf(line) < lines.indexOf(currentLine) -> 0.95f
                        line == currentLine -> 0.9f
                        else -> 0.85f
                    }
                }?.let {
                    text.color = text.color.clone().setAlpha(
                        (1f - (abs(it - 0.9f).coerceIn(0f..0.05f) * 20f)) * opacity.value
                    )
                    text.setBox(
                        Rectangle(
                            0f,
                            context.app.viewPort.camera.height * it,
                            context.app.viewPort.camera.width.toFloat(),
                            100f
                        )
                    )
                }
            }

            calculateVisibility(time)

            texts[currentLine]?.setColor(
                0,
                currentLine?.take(currentLine!!.indexOf(currentWord) + 1)?.sumOf { it.text.display().length } ?: 0,
                ColorRGBA(1f, 1f, 1f, opacity.value)
            )

            opacity.tick(delta) { if (isVisible) 1f else 0f }
        }
    }

    context(Midis2jam2)
    private fun calculateVisibility(time: Duration) {
        if (currentLine != null && time in currentLine!!.startTime..currentLine!!.endTime) {
            isVisible = true
            return
        }
        isVisible = when {
            lineCollector.peek()?.let { it.startTime - time <= 2.0.seconds } ?: false -> true

            lineCollector.prev()?.let { prev ->
                lineCollector.peek()?.let { peek ->
                    peek.startTime - prev.endTime <= 7.0.seconds
                }
            } ?: false -> true

            lineCollector.prev()?.let { time - it.endTime <= 2.0.seconds } ?: false -> true
            else -> false
        }
    }

    /**
     * Renders the debug info.
     *
     * @param time The current time.
     * @return The debug info.
     */
    fun debugInfo(time: Duration): String = buildString {
        val lineTime = with(context) { currentLine?.endTime?.let { currentLine?.startTime?.rangeTo(it) } }
        appendLine("currentLine        ${currentLine?.renderString()}")
        appendLine("currentLineTime    ${lineTime}\n")
        appendLine("in currentLineTime ${lineTime?.let { time in it }}\n")
        appendLine("currentWord ${currentWord?.text}\n")
        appendLine("peek line   ${lineCollector.peek()?.renderString()}")
        appendLine("prev line   ${lineCollector.prev()?.renderString()}\n")
        appendLine("peek word   ${wordCollector.peek()?.text}")
        appendLine("prev word   ${wordCollector.prev()?.text}")
        appendLine("isVisible   $isVisible")
    }
}

private val separators = listOf("\n", "\r", "\r\n")

private fun String.display() = clean().removePrefix("\\").removePrefix("/")

private fun String.clean() = when {
    this.trim().startsWith("\"") && this.trim().endsWith("\"") -> this.trim().removeSurrounding("\"")
    else -> this
}

private fun List<MetaEvent.Lyric>.renderString(): String = joinToString("") { it.text.display() }

private fun List<MetaEvent.Lyric>.partitionByNewLines(): List<LyricLine> {
    val result = mutableListOf<LyricLine>()
    var line = mutableListOf<MetaEvent.Lyric>()

    forEach { item ->
        when {
            separators.any { it == item.text } -> {
                if (line.isNotEmpty()) result.add(line)
                line = mutableListOf()
            }

            item.text.clean().startsWith("/") || item.text.clean().startsWith("\\") -> {
                if (line.isNotEmpty()) result.add(line)
                line = mutableListOf()
                line.add(item)
            }

            item.text.endsWith("\n") || item.text.endsWith("\r") || item.text.endsWith("\r\n") -> {
                line.add(item)
                result.add(line)
                line = mutableListOf()
            }

            else -> {
                line.add(item)
            }
        }
    }
    if (line.isNotEmpty()) {
        result.add(line)
    }

    if (result.size == 1) {
        // The file had no newlines, so we should split it if a lyric ends with a .!?
        val newResult = mutableListOf<LyricLine>()
        var newLine = mutableListOf<MetaEvent.Lyric>()
        result[0].forEach {
            newLine.add(it)
            if (it.text.trim().endsWith(".") || it.text.trim().endsWith("!") || it.text.trim().endsWith("?")) {
                newResult.add(newLine)
                newLine = mutableListOf()
            }
        }
        if (newLine.isNotEmpty()) {
            newResult.add(newLine)
        }
        return newResult
    }

    return result
}
