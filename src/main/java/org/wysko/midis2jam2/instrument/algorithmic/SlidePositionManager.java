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
import org.wysko.midis2jam2.util.Utils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SlidePositionManager implements FingeringManager<List<Integer>> {
	
	/**
	 * Stores the slide table.
	 */
	private final HashMap<Integer, List<Integer>> slideTable = new HashMap<>();
	
	private SlidePositionManager() {
	}
	
	/**
	 * Instantiates a new slide position manager.
	 *
	 * @param clazz the class who correlates the instrument in the XML file
	 */
	public static SlidePositionManager from(Class<? extends Instrument> clazz) {
		String className = clazz.getSimpleName();
		var manager = new SlidePositionManager();
		/* XML Parsing */
		try {
			Document xmlDoc = Utils.instantiateXmlParser("/instrument_mapping.xml");
			NodeList instrumentList = xmlDoc.getDocumentElement().getElementsByTagName("instrument");
			
			/* For each instrument */
			for (var i = 0; i < instrumentList.getLength(); i++) {
				Node instrument = instrumentList.item(i);
				NamedNodeMap instrumentAttributes = instrument.getAttributes();
				/* Find instrument with matching name */
				if (instrumentAttributes.getNamedItem("name").getTextContent().equals(className)) {
					String mappingType = instrumentAttributes.getNamedItem("mapping-type").getTextContent();
					if (!mappingType.equals("slide_position")) throw new InvalidMappingType(String.format("XML has a " +
							"mapping type of %s.", mappingType));
					
					/* Get key mapping */
					Node mapping = ((Element) instrument).getElementsByTagName("mapping").item(0);
					NodeList maps = ((Element) mapping).getElementsByTagName("map");
					
					for (var j = 0; j < maps.getLength(); j++) {
						var note = maps.item(j);
						var noteValue = Integer.parseInt(note.getAttributes().getNamedItem("note").getTextContent());
						var validPositions = ((Element) note).getElementsByTagName("pos");
						List<Integer> validPosList = new ArrayList<>();
						for (var k = 0; k < validPositions.getLength(); k++) {
							validPosList.add(Integer.parseInt(validPositions.item(k).getTextContent()));
						}
						manager.slideTable.put(noteValue, validPosList);
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
	public List<Integer> fingering(int midiNote) {
		return slideTable.get(midiNote);
	}
}
