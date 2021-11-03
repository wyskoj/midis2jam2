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
package org.wysko.midis2jam2.instrument.algorithmic

import org.w3c.dom.Element
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.util.Utils.instantiateXmlParser
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/**
 * Handles fingering for instruments that play by defining which arrangement of keys are pressed (e.g., saxophone,
 * trumpet, tuba)
 */
class PressedKeysFingeringManager private constructor() : FingeringManager<Array<Int>> {

    /** Stores the fingering table. */
    private val fingerTable = HashMap<Int, Array<Int>>()

    override fun fingering(midiNote: Int): Array<Int>? {
        return fingerTable[midiNote]
    }

    companion object {
        /**
         * Instantiates a new pressed keys fingering manager.
         *
         * @param clazz the class who correlates the instrument in the XML file
         */
        fun from(clazz: Class<out Instrument>): PressedKeysFingeringManager {
            val className = clazz.simpleName
            val manager = PressedKeysFingeringManager()
            /* XML Parsing */
            try {
                val xmlDoc = instantiateXmlParser("/instrument_mapping.xml")
                val instrumentList = xmlDoc.documentElement.getElementsByTagName("instrument")

                /* For each instrument */
                for (i in 0 until instrumentList.length) {
                    val instrument = instrumentList.item(i)
                    val instrumentAttributes = instrument.attributes
                    /* Find instrument with matching name */
                    if (instrumentAttributes.getNamedItem("name").textContent == className) {
                        val mappingType = instrumentAttributes.getNamedItem("mapping-type").textContent
                        if (mappingType != "pressed_keys") throw InvalidMappingType(
                            "XML has a mapping type of $mappingType."
                        )

                        /* Get key mapping */
                        val mapping = (instrument as Element).getElementsByTagName("mapping").item(0)
                        val maps = (mapping as Element).getElementsByTagName("map")
                        val mapSize = maps.length

                        /* For each defined note */
                        for (j in 0 until mapSize) {
                            val note = maps.item(j)
                            val keys = (note as Element).getElementsByTagName("key")
                            val keyInts = Array(keys.length) {
                                keys.item(it).textContent.toInt()
                            }
                            manager.fingerTable[note.getAttribute("note").toInt()] = keyInts
                        }
                        break
                    }
                }
            } catch (e: SAXException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ParserConfigurationException) {
                e.printStackTrace()
            } catch (e: InvalidMappingType) {
                e.printStackTrace()
            }
            return manager
        }
    }
}