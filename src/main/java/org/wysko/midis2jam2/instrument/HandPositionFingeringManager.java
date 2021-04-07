package org.wysko.midis2jam2.instrument;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;

import static org.wysko.midis2jam2.instrument.HandPositionFingeringManager.Hands;

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
		HandPositionFingeringManager manager = new HandPositionFingeringManager();
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
					if (!mappingType.equals("hands")) throw new InvalidMappingType(String.format("XML has a " +
							"mapping type of %s.", mappingType));
					
					/* Get key mapping */
					Node mapping = ((Element) instrument).getElementsByTagName("mapping").item(0);
					NodeList maps = ((Element) mapping).getElementsByTagName("map");
					int mapSize = maps.getLength();
					/* For each defined note */
					for (int j = 0; j < mapSize; j++) {
						NamedNodeMap attributes = maps.item(j).getAttributes();
						int note = Integer.parseInt(attributes.getNamedItem("note").getTextContent());
						int leftHand = Integer.parseInt(attributes.getNamedItem("lh").getTextContent());
						int rightHand = Integer.parseInt(attributes.getNamedItem("rh").getTextContent());
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
