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

package org.wysko.midis2jam2.util;

import com.jme3.math.FastMath;
import com.jme3.scene.Spatial.CullHint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Provides various utility functions.
 */
public final class Utils {
	
	private Utils() {
	}
	
	/**
	 * Returns an appropriate {@link CullHint} for the passed boolean. Essentially, it returns the correct {@link
	 * CullHint} assuming {@code true} means the object should be visible, otherwise it should not be visible.
	 * <p>
	 * When {@code true} is passed, returns {@link CullHint#Dynamic}. Otherwise, returns {@link CullHint#Always}. Useful
	 * for avoiding writing {@code b ? Dynamic : Always}.
	 *
	 * @param b true if an object should be visible, false otherwise
	 * @return {@link CullHint#Dynamic} or {@link CullHint#Always}
	 */
	@SuppressWarnings("java:S2301")
	@Contract(pure = true)
	@NotNull
	public static CullHint cullHint(boolean b) {
		if (b) {
			return CullHint.Dynamic;
		} else {
			return CullHint.Always;
		}
	}
	
	/**
	 * Given an {@link Exception}, converts it to a {@link String}, including the exception name and stack trace. For
	 * example:
	 * <pre>
	 * ArithmeticException: / by zero
	 * UtilsTest.exceptionToLines(UtilsTest.java:27)
	 * java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	 * ...
	 * </pre>
	 *
	 * @param e the exception
	 * @return a formatted string containing the exception and a stack trace
	 */
	public static String exceptionToLines(Throwable e) {
		var sb = new StringBuilder();
		sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString()).append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * Given a URL, retrieves the contents through HTTP GET.
	 *
	 * @param url the URL to fetch
	 * @return a string containing the response
	 * @throws IOException if there was an error fetching data
	 */
	@SuppressWarnings("java:S1943")
	public static String getHTML(String url) throws IOException {
		var result = new StringBuilder();
		var conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			for (String line; (line = reader.readLine()) != null; ) {
				result.append(line);
			}
		}
		return result.toString();
	}
	
	/**
	 * Given the file name of a resource file, instantiates and configures an XML parser for reading data from the
	 * specified file. Pass a resource string, for example, {@code "/instrument_mapping.xml"}.
	 *
	 * @param resourceName the filename of a resource file
	 * @return a {@link Document} ready for traversal
	 * @throws SAXException                 if there was an error parsing the XML file
	 * @throws ParserConfigurationException if there was an error instantiating the document parser
	 * @throws IOException                  if there was an IO error
	 */
	@SuppressWarnings("java:S1160")
	public static Document instantiateXmlParser(String resourceName)
			throws SAXException, ParserConfigurationException, IOException {
		final var df = DocumentBuilderFactory.newInstance();
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return df.newDocumentBuilder().parse(PressedKeysFingeringManager.class.getResourceAsStream(resourceName));
	}
	
	/**
	 * Converts an angle expressed in degrees to radians.
	 *
	 * @param deg the angle expressed in degrees
	 * @return the angle expressed in radians
	 */
	public static float rad(float deg) {
		return deg / 180 * FastMath.PI;
	}
	
	/**
	 * Converts an angle expressed in degrees to radians.
	 *
	 * @param deg the angle expressed in degrees
	 * @return the angle expressed in radians
	 */
	public static float rad(double deg) {
		return (float) (deg / 180 * FastMath.PI);
	}
	
	/**
	 * Given a string containing the path to a resource file, retrieves the contents of the file and returns it as a
	 * string.
	 *
	 * @param file the file to read
	 * @return the contents
	 */
	@NotNull
	@SuppressWarnings("java:S1943")
	public static String resourceToString(String file) {
		return new BufferedReader(new InputStreamReader(requireNonNull(Utils.class.getResourceAsStream(file))))
				.lines().collect(Collectors.joining("\n"));
	}
}
