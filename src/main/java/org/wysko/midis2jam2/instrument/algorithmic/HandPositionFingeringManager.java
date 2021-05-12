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
import java.util.HashMap;

import static org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands;

/**
 * Handles fingering that uses hands.
 */
public class HandPositionFingeringManager implements FingeringManager<Hands> {
	
	/**
	 * The table of fingerings.
	 */
	private final HashMap<Integer, Hands> table = new HashMap<>();
	
	/**
	 * Loads the fingering manager from XML given the class
	 *
	 * @param clazz the class of the instrument
	 * @return the hand position fingering manager
	 */
	public static HandPositionFingeringManager from(Class<? extends Instrument> clazz) {
		String className = clazz.getSimpleName();
		var manager = new HandPositionFingeringManager();
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
					if (!mappingType.equals("hands")) throw new InvalidMappingType(String.format("XML has a " +
							"mapping type of %s.", mappingType));
					
					/* Get key mapping */
					Node mapping = ((Element) instrument).getElementsByTagName("mapping").item(0);
					NodeList maps = ((Element) mapping).getElementsByTagName("map");
					int mapSize = maps.getLength();
					/* For each defined note */
					for (var j = 0; j < mapSize; j++) {
						NamedNodeMap attributes = maps.item(j).getAttributes();
						var note = Integer.parseInt(attributes.getNamedItem("note").getTextContent());
						var leftHand = Integer.parseInt(attributes.getNamedItem("lh").getTextContent());
						var rightHand = Integer.parseInt(attributes.getNamedItem("rh").getTextContent());
						manager.table.put(note, new Hands(leftHand, rightHand));
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
	public Hands fingering(int midiNote) {
		return table.get(midiNote);
	}
	
	/**
	 * Defines the indices for left and right hands.
	 */
	public static class Hands {
		
		/**
		 * The index of the left hand.
		 */
		public final int left;
		
		/**
		 * The index of the right hand.
		 */
		public final int right;
		
		/**
		 * Instantiates a new hand configuration.
		 *
		 * @param left  the left hand index
		 * @param right the right hand index
		 */
		public Hands(int left, int right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public String toString() {
			return "Hands{" +
					"left=" + left +
					", right=" + right +
					'}';
		}
	}
}
