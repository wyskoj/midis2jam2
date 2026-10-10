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
     *
     * Across a break between stanzas, the display has faded out, so it moves on just before it fades back in:
     * the first thing seen is [next], not [previous] again.
     */
    private fun handOffTime(previous: LyricLine, next: LyricLine): Duration = with(context) {
        val timeBetween = startTime(next) - endTime(previous)
        if (timeBetween > STANZA_BREAK) return startTime(next) - FADE_IN_LEAD - BREAK_HAND_OFF_MARGIN
        startTime(next) - (timeBetween.coerceAtMost(4.seconds) * 0.45)
    }

    private val lineIndex = lines.withIndex().associate { it.value to it.index }

    /** Whether a break between stanzas follows each line, so the line after it isn't shown beneath it. */
    private val isBreakAfter = lines.mapIndexed { index, line ->
        lines.getOrNull(index + 1)?.let { context.startTime(it) - context.endTime(line) > STANZA_BREAK } ?: true
    }

    /** The line shown, smaller, beneath [currentLine]: the one after it, unless a break between stanzas follows. */
    internal val upcomingLine: LyricLine?
        get() = currentLine?.let { lineIndex.getValue(it) }
            ?.takeUnless { isBreakAfter[it] }
            ?.let { lines[it + 1] }

    private val baseSize = (LYRICS_FONT_BASE_SIZE * settings.lyricsSize).toFloat()

    private val opacity = NumberSmoother(0f, 5.0)
    private var isVisible = false
    private val texts = lines.associateWith {
        BitmapText(font).apply {
            setBox(
                Rectangle(
                    0f,
                    context.app.viewPort.camera.height * 0.8f,
                    context.app.viewPort.camera.width.toFloat(),
                    100f
                )
            )
            size = baseSize * UPCOMING_LINE_SCALE
            color = ColorRGBA.DarkGray
            text = it.renderString()
            alignment = BitmapFont.Align.Center
            highlight.prepare(this)
        }
    }.onEach { context.app.guiNode += it.value }

    /** Where each line is, eased toward where its [Slot] puts it. */
    private val layouts = lines.associateWith { LineLayout(context.app.viewPort.camera.height * 0.8f) }

    /** The top of [line]'s text, in gui space. */
    internal fun topOf(line: LyricLine): Float = layouts.getValue(line).top.value

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

            layOutLines(delta)

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

    /**
     * Eases every line toward where its slot puts it: the current line, the one beneath it, or out of sight.
     * While the display is faded out, lines go straight there instead, so it fades back in already arranged.
     */
    private fun layOutLines(delta: Duration) {
        val camera = context.app.viewPort.camera
        val isHidden = opacity.value < HIDDEN_OPACITY
        val currentIndex = currentLine?.let { lineIndex.getValue(it) }
        val upcoming = upcomingLine

        val currentTop = camera.height * CURRENT_LINE_TOP
        val currentHeight = texts[currentLine]?.let { it.lineHeight * it.lineCount } ?: baseSize
        val upcomingTop = currentTop - currentHeight - baseSize * UPCOMING_LINE_GAP
        val drift = camera.height * LINE_DRIFT

        texts.forEach { (line, text) ->
            val index = lineIndex.getValue(line)
            val slot = when {
                currentIndex == null -> Slot.Waiting
                index < currentIndex -> Slot.Sung
                index == currentIndex -> Slot.Current
                line == upcoming -> Slot.Upcoming
                else -> Slot.Waiting
            }

            fun NumberSmoother.ease(target: Float): Float =
                if (isHidden) target.also { snap(it) } else tick(delta) { target }

            val layout = layouts.getValue(line)
            val top = layout.top.ease(
                when (slot) {
                    Slot.Sung -> currentTop + drift
                    Slot.Current -> currentTop
                    Slot.Upcoming -> upcomingTop
                    Slot.Waiting -> upcomingTop - drift
                }
            )
            val scale = layout.scale.ease(if (slot == Slot.Sung || slot == Slot.Current) 1f else UPCOMING_LINE_SCALE)
            val alpha = layout.alpha.ease(if (slot == Slot.Current || slot == Slot.Upcoming) 1f else 0f)

            (baseSize * scale).let { if (abs(text.size - it) > SIZE_EPSILON) text.size = it }
            text.color = text.color.clone().setAlpha(alpha * opacity.value)
            text.setBox(Rectangle(0f, top, camera.width.toFloat(), 100f))
        }
    }

    /** Where a line belongs on screen. */
    private enum class Slot {
        /** Already sung: drifting up and out of sight. */
        Sung,

        /** Being sung. */
        Current,

        /** Next to be sung, shown smaller beneath the current line. */
        Upcoming,

        /** Not yet shown. */
        Waiting,
    }

    /** A line's position, size (as a multiple of the lyrics size) and opacity, each eased toward its target. */
    private class LineLayout(initialTop: Float) {
        val top = NumberSmoother(initialTop, 10.0)
        val scale = NumberSmoother(UPCOMING_LINE_SCALE, 10.0)
        val alpha = NumberSmoother(0f, 10.0)
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
                lineCollector.peek()?.let { startTime(it) - time <= FADE_IN_LEAD } == true -> true

                lineCollector.prev()?.let { prev ->
                    lineCollector.peek()?.let { peek ->
                        startTime(peek) - endTime(prev) <= STANZA_BREAK
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
        appendLine("prev line   ${lineCollector.prev()?.renderString()}")
        appendLine("upcoming    ${upcomingLine?.renderString()}\n")
        appendLine("peek word   ${wordCollector.peek()?.text}")
        appendLine("prev word   ${wordCollector.prev()?.text}")
        appendLine("isVisible   $isVisible")
    }
}

/** The base pixel size `Assets/Fonts/Inter.fnt` (and any dynamic atlas standing in for it) is rendered at. */
private const val LYRICS_FONT_BASE_SIZE = 64

/** The top of the current line, as a fraction of the screen's height. */
private const val CURRENT_LINE_TOP = 0.9f

/** How far, as a fraction of the screen's height, a line drifts as it fades in or out. */
private const val LINE_DRIFT = 0.05f

/** How big the upcoming line is, relative to the current one. */
private const val UPCOMING_LINE_SCALE = 0.6f

/** The space between the current line and the upcoming line beneath it, as a fraction of the lyrics size. */
private const val UPCOMING_LINE_GAP = 0.15f

/** The smallest change in a line's size worth laying its text out again for. */
private const val SIZE_EPSILON = 0.01f

/**
 * A gap between lines longer than this is a break between stanzas: the display fades out during it, and the
 * line after it isn't shown beneath the line before it.
 */
private val STANZA_BREAK = 7.seconds

/** How long before a line starts the display begins fading in, when it had faded out. */
private val FADE_IN_LEAD = 2.seconds

/** How long before the display fades back in after a break between stanzas it moves on to the next line. */
private val BREAK_HAND_OFF_MARGIN = 0.25.seconds

/** Below this opacity the display counts as faded out, and lines are moved without easing. */
private const val HIDDEN_OPACITY = 0.01f

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
