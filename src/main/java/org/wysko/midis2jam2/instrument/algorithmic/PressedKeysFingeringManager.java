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

package org.wysko.midis2jam2.instrument.algorithmic;

import org.w3c.dom.*;
import org.wysko.midis2jam2.instrument.Instrument;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Handles fingering for instruments that play by defining which arrangement of keys are pressed (e.g., saxophone,
 * trumpet, tuba)
 */
public class PressedKeysFingeringManager implements FingeringManager<Integer[]> {
	
	/**
	 * Stores the fingering table.
	 */
	private final HashMap<Integer, Integer[]> fingerTable = new HashMap<>();
	
	private PressedKeysFingeringManager() {
	}
	
	/**
	 * Instantiates a new pressed keys fingering manager.
	 *
	 * @param clazz the class who correlates the instrument in the XML file
	 */
	public static PressedKeysFingeringManager from(Class<? extends Instrument> clazz) {
		String className = clazz.getSimpleName();
		PressedKeysFingeringManager manager = new PressedKeysFingeringManager();
		/* XML Parsing */
		try {
			Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(PressedKeysFingeringManager.class.getResourceAsStream("/instrument_mapping.xml"));
			
			NodeList instrumentList = xmlDoc.getDocumentElement().getElementsByTagName("instrument");
			
			/* For each instrument */
			for (int i = 0; i < instrumentList.getLength(); i++) {
				Node instrument = instrumentList.item(i);
				NamedNodeMap instrumentAttributes = instrument.getAttributes();
				/* Find instrument with matching name */
				if (instrumentAttributes.getNamedItem("name").getTextContent().equals(className)) {
					String mappingType = instrumentAttributes.getNamedItem("mapping-type").getTextContent();
					if (!mappingType.equals("pressed_keys")) throw new InvalidMappingType(String.format("XML has a " +
							"mapping type of %s.", mappingType));
					
					/* Get key mapping */
					Node mapping = ((Element) instrument).getElementsByTagName("mapping").item(0);
					NodeList maps = ((Element) mapping).getElementsByTagName("map");
					int mapSize = maps.getLength();
					/* For each defined note */
					for (int j = 0; j < mapSize; j++) {
						Node note = maps.item(j);
						NodeList keys = ((Element) note).getElementsByTagName("key");
						Integer[] keyInts = new Integer[keys.getLength()];
						/* Collect pressed keys */
						for (int k = 0; k < keys.getLength(); k++) {
							keyInts[k] = Integer.parseInt(keys.item(k).getTextContent());
						}
						manager.fingerTable.put(Integer.parseInt(((Element) note).getAttribute("note")), keyInts);
					}
					break;
				}
			}
		} catch (SAXException | IOException | ParserConfigurationException | InvalidMappingType e) {
			e.printStackTrace();
		}
		return manager;
	}
	
	@Override
	public Integer[] fingering(int midiNote) {
		return fingerTable.get(midiNote);
	}
}
