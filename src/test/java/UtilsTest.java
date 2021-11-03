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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wysko.midis2jam2.util.Utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.wysko.midis2jam2.util.Utils.rad;

/** Tests utility functions from {@link Utils}. */
class UtilsTest {
	
	@Test
	void testExceptionToLines() {
		try {
			@SuppressWarnings({"NumericOverflow", "divzero", "unused"}) int a = 3 / 0;
		} catch (ArithmeticException e) {
			assertDoesNotThrow(() -> {
				Utils.exceptionToLines(e);
			}, "Method should not throw an exception.");
		}
	}
	
	@Test
	void testGetHTML() {
		assertDoesNotThrow(() -> {
			Utils.getHTML("https://google.com/");
		}, "Method should not throw an exception.");
	}
	
	@Test
	void testInstantiateXmlParser() {
		assertDoesNotThrow(() -> {
			Utils.instantiateXmlParser("/instrument_mapping.xml");
		}, "Method should not throw an exception.");
	}
	
	@Test
	void testRadFloat() {
		Assertions.assertEquals(0, rad(0.0), 0, "0 degrees is exactly 0 radians.");
		Assertions.assertEquals(1.5707963, rad(90.0), 0.000001, "90 degrees is about 1.5707963 radians.");
		Assertions.assertEquals(3.1415926, rad(180.0), 0.000001, "180 degrees is about 3.1415926 radians.");
	}
	
	@Test
	void testRadDouble() {
		Assertions.assertEquals(0, rad(0.0), 0, "0 degrees is exactly 0 radians.");
		Assertions.assertEquals(1.5707963, rad(90D), 0.000001, "90 degrees is about 1.5707963 radians.");
		Assertions.assertEquals(3.1415926, rad(180D), 0.000001, "180 degrees is about 3.1415926 radians.");
	}
}
