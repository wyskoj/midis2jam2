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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wysko.midis2jam2.Midis2jam2;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Calculates fret heights using a lookup table.
 */
public class FretHeightByTable implements FretHeightCalculator {
	
	
	/**
	 * The lookup table. The key is the fret and the value is the scaling.
	 */
	final Map<Integer, Float> lookupTable;
	
	/**
	 * Instantiates a new Fret height by table.
	 *
	 * @param lookupTable the lookup table
	 */
	public FretHeightByTable(Map<Integer, Float> lookupTable) {
		this.lookupTable = lookupTable;
	}
	
	public static FretHeightByTable fromXml(Class<? extends FrettedInstrument> clazz) {
		try {
			var xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(FretHeightByTable.class.getResourceAsStream("/fret_heights.xml"));
			NodeList instrumentList = xmlDoc.getDocumentElement().getElementsByTagName("instrument");
			Map<Integer, Float> lookup = new HashMap<>();
			for (var i = 0; i < instrumentList.getLength(); i++) {
				if (instrumentList.item(i).getAttributes().getNamedItem("name").getTextContent().equals(clazz.getSimpleName())) {
					var instrument = instrumentList.item(i);
					var fretHeights = ((Element) instrument).getElementsByTagName("value");
					for (int j = 0; j < fretHeights.getLength(); j++) {
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
		} catch (Exception e) {
			Midis2jam2.logger.log(Level.SEVERE, "Failed to load fret height from XML for %s.".formatted(clazz.getName()));
			e.printStackTrace();
			return new FretHeightByTable(new HashMap<>());
		}
		
	}
	
	@Override
	public float calculateScale(int fret) {
		return lookupTable.get(fret);
	}
}
