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
package org.wysko.midis2jam2.util

import com.jme3.material.Material
import com.jme3.math.ColorRGBA.Blue
import com.jme3.math.ColorRGBA.Green
import com.jme3.math.ColorRGBA.Red
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import com.jme3.scene.debug.Arrow
import org.wysko.midis2jam2.Midis2jam2
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Collectors
import kotlin.math.pow
import kotlin.math.sqrt

/** Provides various utility functions. */
object Utils {

    /**
     * Given a URL, retrieves the contents through HTTP GET.
     *
     * @param url The URL to fetch.
     * @return A string containing the response.
     * @throws IOException if there was an error fetching data
     */
    fun getHTML(url: String): String = (URL(url).openConnection() as HttpURLConnection)
        .apply { requestMethod = "GET" }
        .inputStream
        .bufferedReader()
        .use { it.readText() }

    /**
     * Converts an angle expressed in degrees to radians.
     *
     * @param deg the angle expressed in degrees
     * @return the angle expressed in radians
     */
    fun rad(deg: Float): Float = deg / 180 * FastMath.PI

    /**
     * Converts an angle expressed in degrees to radians.
     *
     * @param deg the angle expressed in degrees
     * @return the angle expressed in radians
     */
    fun rad(deg: Double): Float = (deg / 180 * FastMath.PI).toFloat()

    /**
     * Given a string containing the path to a resource file, retrieves the contents of the file and returns it as a
     * string.
     *
     * @param file the file to read
     * @return the contents
     */
    fun resourceToString(file: String): String {
        val resourceAsStream = Utils::class.java.getResourceAsStream(file) ?: return ""
        return BufferedReader(InputStreamReader(resourceAsStream)).lines().collect(Collectors.joining("\n"))
    }

    /**
     * Linearly interpolates between two values.
     *
     * @param a The starting value.
     * @param b The ending value.
     * @param t The interpolation factor.
     */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

/** Converts a boolean into its appropriate [CullHint]. */
val Boolean.ch: CullHint
    get() = if (this) Dynamic else Always

/** Given a list of integers, determines if the root integer is equal to at least one of the provided integers. */
fun Int.oneOf(vararg options: Int): Boolean = options.any { it == this }

/** Chunks a sequence based on a predicate. */
fun <T> Sequence<T>.chunked(predicate: (T, T) -> Boolean): Sequence<List<T>> {
    val underlyingSequence = this
    return sequence {
        val buffer = mutableListOf<T>()
        var last: T? = null
        for (current in underlyingSequence) {
            val shouldSplit = last?.let { predicate(it, current) } ?: false
            if (shouldSplit) {
                yield(buffer.toList())
                buffer.clear()
            }
            buffer.add(current)
            last = current
        }
        if (buffer.isNotEmpty()) {
            yield(buffer)
        }
    }
}

/**
 * Renders debug axes.
 *
 * @param context The context to the main class.
 */
@Suppress("Unused")
fun debugAxes(context: Midis2jam2): Node {
    val axes = mapOf(
        Vector3f.UNIT_X to Red,
        Vector3f.UNIT_Y to Green,
        Vector3f.UNIT_Z to Blue
    )
    return node {
        axes.forEach { (axis, color) ->
            +Geometry(
                "coordinate axis",
                Arrow(axis.mult(10f)).apply {
                    material = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                        additionalRenderState.run {
                            isWireframe = true
                            lineWidth = 4f
                        }
                        setColor("Color", color)
                    }
                }
            )
        }
    }
}

/**
 * Converts a [Vector3f] to a [Quaternion], without any degrees–radians conversion.
 *
 * @receiver The vector to convert.
 * @return The quaternion.
 */
fun Vector3f.toQuaternion(): Quaternion = Quaternion().fromAngles(this.x, this.y, this.z)

/**
 * The sign of a boolean value as a float.
 *
 * Returns `1.0` if the boolean value is `true`, `-1.0` if it is `false`.
 */
val Boolean.sign: Float
    get() = if (this) 1f else -1f

/**
 * Wraps a string to a given length.
 *
 * @param length The length to wrap the string to.
 */
fun String.wrap(length: Int): String = this.chunked(length).joinToString("\n")

fun Collection<Number>.stdDev(): Double {
    val asDoubles = this.map { it.toDouble() }
    val mean = asDoubles.average()
    val sum = asDoubles.sumOf { (it - mean).pow(2) }
    return sqrt(sum / asDoubles.size)
}