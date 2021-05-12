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

public class Utils {
	
	private Utils() {
	}
	
	public static String exceptionToLines(Exception e) {
		var sb = new StringBuilder();
		sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString()).append("\n");
		}
		return sb.toString();
	}
	
	public static String getHTML(String urlToRead) throws IOException {
		var result = new StringBuilder();
		var url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		try (var reader = new BufferedReader(
				new InputStreamReader(conn.getInputStream()))) {
			for (String line; (line = reader.readLine()) != null; ) {
				result.append(line);
			}
		}
		return result.toString();
	}
	
	public static Document instantiateXmlParser(String resourceName) throws SAXException, ParserConfigurationException, IOException {
		final var df = DocumentBuilderFactory.newInstance();
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return df.newDocumentBuilder().parse(PressedKeysFingeringManager.class.getResourceAsStream(resourceName));
	}
}
