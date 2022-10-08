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
package org.wysko.midis2jam2.util

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import com.jme3.scene.debug.Arrow
import org.wysko.midis2jam2.Midis2jam2
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import java.util.stream.Collectors

/** Provides various utility functions. */
object Utils {

    /**
     * Given an [Exception], converts it to a [String], including the exception name and stack trace. For
     * example:
     *
     * ```
     * ArithmeticException: / by zero
     * UtilsTest.exceptionToLines(UtilsTest.java:27)
     * java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * ...
     * ```
     *
     * @param e the exception
     * @return a formatted string containing the exception and a stack trace
     */
    fun exceptionToLines(e: Throwable): String = buildString {
        append("${e.javaClass.simpleName}: ${e.message}\n")
        e.stackTrace.forEach { append("$it\n") }
    }

    /**
     * Given a URL, retrieves the contents through HTTP GET.
     *
     * @param url the URL to fetch
     * @return a string containing the response
     * @throws IOException if there was an error fetching data
     */
    fun getHTML(url: String): String {
        val result = StringBuilder()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line)
            }
        }
        return result.toString()
    }

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

    /** Simple lerp function. */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Determines if a string is an integer.
     *
     * @return true if the string is an integer, false otherwise
     */
    fun String.isInt(): Boolean {
        return try {
            toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun getResourceFolderFiles(folder: String): Array<File> =
        File(Thread.currentThread().contextClassLoader.getResource(folder).path).listFiles()
}

/**
 * Converts a boolean into its appropriate [CullHint].
 *
 * @return [CullHint.Always] if the boolean is true, [CullHint.Never] otherwise
 */
fun Boolean.cullHint(): CullHint = if (this) Dynamic else Always

/** Given a list of integers, determines if the root integer is equal to at least one of the provided integers. */
fun Int.oneOf(vararg options: Int): Boolean = options.any { it == this }

/**
 * A utility function that adds many pairs to a [Properties] object.
 */
fun <K, V> Properties.setProperties(vararg properties: Pair<K, V>) {
    properties.forEach { (key, value) -> this[key] = value }
}


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

/** Returns debug axes. */
fun debugAxes(context: Midis2jam2): Spatial {
    val node = Node()

    fun putShape(shape: Mesh, color: ColorRGBA): Geometry {
        val g = Geometry("coordinate axis", shape)
        val mat = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        mat.additionalRenderState.isWireframe = true
        mat.additionalRenderState.lineWidth = 4f
        mat.setColor("Color", color)
        g.material = mat
        node.attachChild(g)
        return g
    }

    var arrow = Arrow(Vector3f.UNIT_X.mult(10f))
    putShape(arrow, ColorRGBA.Red)

    arrow = Arrow(Vector3f.UNIT_Y.mult(10f))
    putShape(arrow, ColorRGBA.Green)

    arrow = Arrow(Vector3f.UNIT_Z.mult(10f))
    putShape(arrow, ColorRGBA.Blue)

    return node
}
