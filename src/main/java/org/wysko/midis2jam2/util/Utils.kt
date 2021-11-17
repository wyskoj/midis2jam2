/*
 * Copyright (C) 2021 Jacob Wysko
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

import com.jme3.math.FastMath
import com.jme3.scene.Spatial.CullHint
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.jetbrains.annotations.Contract
import org.w3c.dom.Document
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.xml.sax.SAXException
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/** Provides various utility functions. */
object Utils {

    /**
     * Returns an appropriate [CullHint] for the passed boolean. Essentially, it returns the correct value assuming
     * `true` means the object should be visible, otherwise it should not be visible.
     *
     * When `true` is passed, returns [CullHint.Dynamic]. Otherwise, returns [CullHint.Always]. Useful
     * for avoiding writing `b ? Dynamic : Always`.
     *
     * @param b true if an object should be visible, false otherwise
     * @return [CullHint.Dynamic] or [CullHint.Always]
     */
    @Contract(pure = true)
    @JvmStatic
    fun cullHint(b: Boolean): CullHint = if (b) Dynamic else Always

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
    @JvmStatic
    fun exceptionToLines(e: Throwable) = buildString {
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
    @JvmStatic
    @Throws(IOException::class)
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
     * Given the file name of a resource file, instantiates and configures an XML parser for reading data from the
     * specified file. Pass a resource string, for example, `"/instrument_mapping.xml"`.
     *
     * @param resourceName the filename of a resource file
     * @return a [Document] ready for traversal
     * @throws SAXException                 if there was an error parsing the XML file
     * @throws ParserConfigurationException if there was an error instantiating the document parser
     * @throws IOException                  if there was an IO error
     */
    @JvmStatic
    @Throws(SAXException::class, ParserConfigurationException::class, IOException::class)
    fun instantiateXmlParser(resourceName: String): Document {
        val df = DocumentBuilderFactory.newInstance()
        return df.newDocumentBuilder().parse(PressedKeysFingeringManager::class.java.getResourceAsStream(resourceName))
    }

    /**
     * Converts an angle expressed in degrees to radians.
     *
     * @param deg the angle expressed in degrees
     * @return the angle expressed in radians
     */
    fun rad(deg: Float) = deg / 180 * FastMath.PI


    /**
     * Converts an angle expressed in degrees to radians.
     *
     * @param deg the angle expressed in degrees
     * @return the angle expressed in radians
     */
    @JvmStatic
    fun rad(deg: Double) = (deg / 180 * FastMath.PI).toFloat()


    /**
     * Given a string containing the path to a resource file, retrieves the contents of the file and returns it as a
     * string.
     *
     * @param file the file to read
     * @return the contents
     */
    @JvmStatic
    fun resourceToString(file: String): String =
        BufferedReader(InputStreamReader(Objects.requireNonNull(Utils::class.java.getResourceAsStream(file)))).lines()
            .collect(Collectors.joining("\n"))

    /** Given a [file], returns the contents of the file as a string. */
    @JvmStatic
    fun fileToString(file: File): String = file.readText()

    /** Simple lerp function. */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}