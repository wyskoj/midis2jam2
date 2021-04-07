package org.wysko.midis2jam2.instrument;

/**
 * Denotes when a specified mapping type does not match the XML file.
 */
public class InvalidMappingType extends Exception {
	public InvalidMappingType(String message) {
		super(message);
	}
}
