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

package org.wysko.midis2jam2.instrument.family.guitar;

import org.jetbrains.annotations.Contract;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.util.Utils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.wysko.midis2jam2.util.Utils.exceptionToLines;

/**
 * Calculates fret heights using a lookup table.
 */
public class FretHeightByTable implements FretHeightCalculator {
	
	/**
	 * The lookup table. The key is the fret and the value is the scaling.
	 */
	private final Map<Integer, Float> lookupTable;
	
	/**
	 * Instantiates a new Fret height by table.
	 *
	 * @param lookupTable the lookup table
	 */
	public FretHeightByTable(Map<Integer, Float> lookupTable) {
		this.lookupTable = lookupTable;
	}
	
	/**
	 * Given the class of a {@link FrettedInstrument}, returns the XML data defined in {@code fret_heights.xml} for that
	 * specific instrument.
	 *
	 * @param clazz the class of the instrument to get data
	 * @return a {@link FretHeightByTable} for that instrument, containing the data in the XML
	 */
	public static FretHeightByTable fromXml(Class<? extends FrettedInstrument> clazz) {
		try {
			var xmlDoc = Utils.instantiateXmlParser("/fret_heights.xml");
			NodeList instrumentList = xmlDoc.getDocumentElement().getElementsByTagName("instrument");
			Map<Integer, Float> lookup = new HashMap<>();
			
			/* For each instrument */
			for (var i = 0; i < instrumentList.getLength(); i++) {
				/* If the name of class equals the name of the currently indexed instrument */
				if (instrumentList.item(i).getAttributes().getNamedItem("name").getTextContent().equals(clazz.getSimpleName())) {
					var instrument = instrumentList.item(i);
					var fretHeights = ((Element) instrument).getElementsByTagName("value");
					/* For each fret height definition */
					for (var j = 0; j < fretHeights.getLength(); j++) {
						/* Store attributes to the lookup table */
						var attributes = fretHeights.item(j).getAttributes();
						lookup.put(
								Integer.parseInt(attributes.getNamedItem("fret").getTextContent()),
								Float.parseFloat(attributes.getNamedItem("scale").getTextContent())
						);
					}
					break;
				}
			}
			return new FretHeightByTable(lookup);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			Midis2jam2.getLOGGER().severe("Failed to load fret height from XML for %s.%n%s"
					.formatted(clazz.getName(), exceptionToLines(e)));
			return new FretHeightByTable(new HashMap<>());
		}
		
	}
	
	@Override
	@Contract(pure = true)
	public float calculateScale(int fret) {
		return lookupTable.get(fret);
	}
}
