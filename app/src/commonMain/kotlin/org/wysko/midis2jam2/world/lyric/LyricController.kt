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
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.world.font.resolveBitmapFont
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A controller for displaying lyrics.
 *
 * @property context The context to the main class.
 * @property events The list of the text events.
 */
class LyricController(private val context: PerformanceManager, private val events: List<MetaEvent.Lyric>) {

    private val words = events.filter { !separators.contains(it.text) }
    private val lines = events.partitionByNewLines()

    private val font = resolveBitmapFont(
        context.app.assetManager,
        context.app.assetManager.loadFont("Assets/Fonts/Inter.fnt"),
        lines.flatMapTo(mutableSetOf()) { it.renderString().toSet() },
        basePixelSize = LYRICS_FONT_BASE_SIZE,
    )

    /** The syllable that has most recently elapsed, or null before the first one. */
    internal var currentWord: MetaEvent.Lyric? = null
        private set

    /** The line currently being sung, or null before the first one. */
    internal var currentLine: LyricLine? = null
        private set

    /** Every line of this file's lyrics, in order. */
    internal val allLines: List<LyricLine> get() = lines

    /** Whether the lyric display is currently showing anything. */
    internal val isShowing: Boolean get() = isVisible

    /**
     * How many characters of [currentLine] have already been sung.
     *
     * The display colours this prefix white and leaves the rest gray, so this is the number
     * the on-screen highlight is driven from.
     */
    internal val elapsedCharactersOfCurrentLine: Int
        get() = currentLine?.take((currentLine?.indexOf(currentWord) ?: -1) + 1)
            ?.sumOf { it.text.display().length }
            ?: 0

    /**
     * How many characters of [currentLine] the display last showed as sung. In the glide style this is
     * fractional while a syllable is part-way sung; otherwise it matches [elapsedCharactersOfCurrentLine].
     */
    internal var sungCharactersOfCurrentLine: Float = 0f
        private set

    private val settings = context.config.settings.onScreenElementsSettings.lyricsSettings

    private val highlight = settings.style.highlight(context.app.assetManager)

    private val syllables = lines.associateWith { line ->
        line.map { TimedSyllable(context.sequence.getTimeOf(it), it.text.display().length) }
    }

    /** When the display moves on from each line to the one after it; the last line is never moved on from. */
    private val handOffs = lines.withIndex().associate { (index, line) ->
        line to (lines.getOrNull(index + 1)?.let { handOffTime(line, it) } ?: Duration.INFINITE)
    }

    private val wordCollector = EventCollector(context, words, onSeek = { currentWord = it.prev() })
    private val lineCollector = LyricLineCollector(context, lines, onSeek = {
        currentLine = it.prev()
    }, triggerCondition = { line: LyricLine, time: Duration ->
        currentLine?.let { time > handOffTime(it, line) } ?: (time >= context.startTime(line) - 1.seconds)
    })

    /**
     * When the display moves from [previous] to [next]: a little before [next] starts, sooner the longer the
     * gap between them (up to a point), so the next line has slid into place by the time it is sung.
     */
    private fun handOffTime(previous: LyricLine, next: LyricLine): Duration = with(context) {
        val timeBetween = (startTime(next) - endTime(previous)).coerceAtMost(4.seconds)
        startTime(next) - (timeBetween * 0.45)
    }

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
            size = (LYRICS_FONT_BASE_SIZE * settings.lyricsSize).toFloat()
            color = ColorRGBA.DarkGray
            text = it.renderString()
            alignment = BitmapFont.Align.Center
            highlight.prepare(this)
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

            sungCharactersOfCurrentLine = currentLine?.let { line ->
                texts[line]?.let { text ->
                    highlight.highlight(
                        text,
                        syllables.getValue(line),
                        handOffs.getValue(line),
                        elapsedCharactersOfCurrentLine,
                        time,
                        ColorRGBA(1f, 1f, 1f, opacity.value),
                    )
                }
            } ?: 0f

            opacity.tick(delta) { if (isVisible) 1f else 0f }
        }
    }

    private fun calculateVisibility(time: Duration) {
        with(context) {
            currentLine?.let {
                if (time in startTime(it)..endTime(it)) {
                    isVisible = true
                    return
                }
            }
            isVisible = when {
                lineCollector.peek()?.let { startTime(it) - time <= 2.0.seconds } == true -> true

                lineCollector.prev()?.let { prev ->
                    lineCollector.peek()?.let { peek ->
                        startTime(peek) - endTime(prev) <= 7.0.seconds
                    }
                } == true -> true

                lineCollector.prev()?.let { time - endTime(it) <= 2.0.seconds } == true -> true
                else -> false
            }
        }
    }

    /**
     * Renders the debug info.
     *
     * @param time The current time.
     * @return The debug info.
     */
    fun debugInfo(time: Duration): String = buildString {
        val lineTime = with(context) { currentLine?.let { startTime(it)..endTime(it) } }
        appendLine("currentLine        ${currentLine?.renderString()}")
        appendLine("currentLineTime    ${lineTime}\n")
        appendLine("in currentLineTime ${lineTime?.let { time in it } ?: ""}\n")
        appendLine("currentWord ${currentWord?.text}\n")
        appendLine("peek line   ${lineCollector.peek()?.renderString()}")
        appendLine("prev line   ${lineCollector.prev()?.renderString()}\n")
        appendLine("peek word   ${wordCollector.peek()?.text}")
        appendLine("prev word   ${wordCollector.prev()?.text}")
        appendLine("isVisible   $isVisible")
    }
}

/** The base pixel size `Assets/Fonts/Inter.fnt` (and any dynamic atlas standing in for it) is rendered at. */
private const val LYRICS_FONT_BASE_SIZE = 64

private val separators = listOf("\n", "\r", "\r\n")

internal fun String.display() = clean().replace("/", "").replace("\\", "").removePrefix("<").replace("^", " ")

internal fun String.clean() = when {
    this.trim().startsWith("\"") && this.trim().endsWith("\"") -> this.trim().removeSurrounding("\"")
    else -> this
}

internal fun List<MetaEvent.Lyric>.renderString(): String = joinToString("") { it.text.display() }

internal fun List<MetaEvent.Lyric>.partitionByNewLines(): List<LyricLine> {
    val result = mutableListOf<LyricLine>()
    var line = mutableListOf<MetaEvent.Lyric>()

    forEach { item ->
        when {
            separators.any { it == item.text } -> {
                if (line.isNotEmpty()) result.add(line)
                line = mutableListOf()
            }

            item.text.clean().startsWith("/") || item.text.clean().startsWith("\\") || item.text.clean().startsWith("<") -> {
                if (line.isNotEmpty()) result.add(line)
                line = mutableListOf()
                line.add(item)
            }

            item.text.endsWith("\n") || item.text.endsWith("\r") || item.text.endsWith("\r\n") || item.text.endsWith("/") -> {
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
