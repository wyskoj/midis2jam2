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

package org.wysko.midis2jam2.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/**
 * an optimized ShortMessage that does not need an array.
 *
 * @author Florian Bomers
 */
@SuppressWarnings("all")
public final class FastShortMessage extends ShortMessage {
	
	private int packedMsg;
	
	public FastShortMessage(int packedMsg) throws InvalidMidiDataException {
		this.packedMsg = packedMsg;
		getDataLength(packedMsg & 0xFF); // to check for validity
	}
	
	/** Creates a FastShortMessage from this ShortMessage */
	public FastShortMessage(ShortMessage msg) {
		this.packedMsg = msg.getStatus()
				| (msg.getData1() << 8)
				| (msg.getData2() << 16);
	}
	
	int getPackedMsg() {
		return packedMsg;
	}
	
	@Override
	public byte[] getMessage() {
		int length = 0;
		try {
			// fix for bug 4851018: MidiMessage.getLength and .getData return wrong values
			// fix for bug 4890405: Reading MidiMessage byte array fails in 1.4.2
			length = getDataLength(packedMsg & 0xFF) + 1;
		} catch (InvalidMidiDataException imde) {
			// should never happen
		}
		byte[] returnedArray = new byte[length];
		if (length > 0) {
			returnedArray[0] = (byte) (packedMsg & 0xFF);
			if (length > 1) {
				returnedArray[1] = (byte) ((packedMsg & 0xFF00) >> 8);
				if (length > 2) {
					returnedArray[2] = (byte) ((packedMsg & 0xFF0000) >> 16);
				}
			}
		}
		return returnedArray;
	}
	
	@Override
	public int getLength() {
		try {
			return getDataLength(packedMsg & 0xFF) + 1;
		} catch (InvalidMidiDataException imde) {
			// should never happen
		}
		return 0;
	}
	
	@Override
	public void setMessage(int status) throws InvalidMidiDataException {
		// check for valid values
		int dataLength = getDataLength(status); // can throw InvalidMidiDataException
		if (dataLength != 0) {
			super.setMessage(status); // throws Exception
		}
		packedMsg = (packedMsg & 0xFFFF00) | (status & 0xFF);
	}
	
	@Override
	public void setMessage(int status, int data1, int data2) throws InvalidMidiDataException {
		getDataLength(status); // can throw InvalidMidiDataException
		packedMsg = (status & 0xFF) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16);
	}
	
	@Override
	public void setMessage(int command, int channel, int data1, int data2) throws InvalidMidiDataException {
		getDataLength(command); // can throw InvalidMidiDataException
		packedMsg = (command & 0xF0) | (channel & 0x0F) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16);
	}
	
	@Override
	public int getChannel() {
		return packedMsg & 0x0F;
	}
	
	@Override
	public int getCommand() {
		return packedMsg & 0xF0;
	}
	
	@Override
	public int getData1() {
		return (packedMsg & 0xFF00) >> 8;
	}
	
	@Override
	public int getData2() {
		return (packedMsg & 0xFF0000) >> 16;
	}
	
	@Override
	public int getStatus() {
		return packedMsg & 0xFF;
	}
	
	/**
	 * Creates a new object of the same class and with the same contents as this object.
	 *
	 * @return a clone of this instance.
	 */
	@Override
	public Object clone() {
		try {
			return new FastShortMessage(packedMsg);
		} catch (InvalidMidiDataException imde) {
			// should never happen
		}
		return null;
	}
	
} // class FastShortMsg
