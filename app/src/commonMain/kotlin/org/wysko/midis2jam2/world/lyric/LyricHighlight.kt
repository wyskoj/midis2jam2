/*
 * Copyright (C) 2026 Jacob Wysko
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

import com.jme3.asset.AssetManager
import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.material.RenderState.BlendMode
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import org.wysko.midis2jam2.domain.settings.AppSettings.OnScreenElementsSettings.LyricsSettings.LyricsStyle
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val LYRIC_WIPE_MAT: String = "Assets/MatDefs/LyricWipe.j3md"

/** The vertex colour that tells the wipe shader which glyph is being sung. Nothing else is drawn in it. */
private val MARKER_COLOR = ColorRGBA(1f, 0f, 1f, 1f)

/** The longest a syllable takes to glide, so one held before a long rest doesn't crawl for seconds. */
internal val MAX_GLIDE: Duration = 1.5.seconds

/** A syllable of a line: when it is sung, and how many characters of the displayed line it covers. */
internal data class TimedSyllable(val start: Duration, val length: Int)

/**
 * How the lyric display marks the sung part of the current line. Each [LyricsStyle] has one.
 */
internal interface LyricHighlight {

    /** Readies [text], the display of one line, once when it is built. */
    fun prepare(text: BitmapText) = Unit

    /**
     * Marks the sung part of [text], the display of the current line, in [sungColor], and returns how many
     * characters of it count as sung (fractional while a character is part-way sung).
     *
     * The controller resets the whole line to its base colour every frame before calling this.
     *
     * @param syllables The line's syllables, in order.
     * @param handOff When the display moves on to the next line, after which this line is no longer highlighted.
     * @param elapsedCharacters How many characters belong to syllables that have started.
     */
    fun highlight(
        text: BitmapText,
        syllables: List<TimedSyllable>,
        handOff: Duration,
        elapsedCharacters: Int,
        time: Duration,
        sungColor: ColorRGBA,
    ): Float
}

/** The highlight for this style. */
internal fun LyricsStyle.highlight(assetManager: AssetManager): LyricHighlight = when (this) {
    LyricsStyle.Syllable -> SyllableHighlight
    LyricsStyle.Glide -> GlideHighlight(assetManager)
}

/** Each syllable turns to the sung colour, all at once, the moment it starts. */
internal object SyllableHighlight : LyricHighlight {
    override fun highlight(
        text: BitmapText,
        syllables: List<TimedSyllable>,
        handOff: Duration,
        elapsedCharacters: Int,
        time: Duration,
        sungColor: ColorRGBA,
    ): Float {
        text.setColor(0, elapsedCharacters, sungColor)
        return elapsedCharacters.toFloat()
    }
}

/**
 * The sung colour sweeps through each syllable over the time it is sung, like karaoke.
 *
 * Bitmap text can only colour whole characters, so the characters already sung are coloured as usual, and
 * the one being sung is drawn in a marker colour. The line's material paints that glyph sung up to the wipe
 * and unsung after it, measuring across the glyph by its texture coordinates. That way, the wipe sits
 * wherever the text's own layout put the glyph, however the line is aligned or wrapped.
 */
internal class GlideHighlight(private val assetManager: AssetManager) : LyricHighlight {

    override fun prepare(text: BitmapText) {
        text.children.filterIsInstance<Geometry>().forEach { page ->
            page.material = Material(assetManager, LYRIC_WIPE_MAT).apply {
                setTexture("ColorMap", page.material.getTextureParam("ColorMap").textureValue)
                setColor("MarkerColor", MARKER_COLOR)
                additionalRenderState.blendMode = BlendMode.Alpha
            }
        }
    }

    override fun highlight(
        text: BitmapText,
        syllables: List<TimedSyllable>,
        handOff: Duration,
        elapsedCharacters: Int,
        time: Duration,
        sungColor: ColorRGBA,
    ): Float {
        val sung = glidedCharacters(syllables, time, handOff)
        val whole = floor(sung).toInt()
        text.setColor(0, whole, sungColor)

        text.text.getOrNull(whole)?.let { character ->
            text.setColor(whole, whole + 1, MARKER_COLOR.clone().setAlpha(sungColor.a))

            val charSet = text.font.charSet
            val glyph = charSet.getCharacter(character.code)
            val u0 = (glyph?.x ?: 0).toFloat() / charSet.width
            val u1 = u0 + (glyph?.width ?: 0).toFloat() / charSet.width
            text.children.filterIsInstance<Geometry>().forEach {
                with(it.material) {
                    setColor("SungColor", sungColor)
                    setColor("UnsungColor", text.color)
                    setFloat("WipeU0", u0)
                    setFloat("WipeU1", u1)
                    setFloat("WipeFraction", sung - whole)
                }
            }
        }

        return sung
    }
}

/**
 * How many characters of a line have been sung by [time], counting a syllable as sung in proportion to
 * how far through it [time] is.
 *
 * A syllable glides from its start to the start of the next one, or for [maxGlide] if that comes sooner. The
 * line's last syllable has no next one to glide to, so it glides for [maxGlide], but always finishes by
 * [handOff], when the display moves on to the next line.
 */
internal fun glidedCharacters(
    syllables: List<TimedSyllable>,
    time: Duration,
    handOff: Duration = Duration.INFINITE,
    maxGlide: Duration = MAX_GLIDE,
): Float {
    var sung = 0f
    syllables.forEachIndexed { index, syllable ->
        if (time < syllable.start) return sung
        val next = syllables.getOrNull(index + 1)?.start ?: handOff
        val glideEnd = minOf(next, syllable.start + maxGlide)
        val glide = glideEnd - syllable.start
        val progress = if (glide <= Duration.ZERO) 1.0 else ((time - syllable.start) / glide).coerceIn(0.0, 1.0)
        sung += (syllable.length * progress).toFloat()
    }
    return sung
}
