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
import org.wysko.midis2jam2.instrument.family.brass.Trombone
import org.wysko.midis2jam2.util.Utils.instantiateXmlParser
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/**
 * Manages and defines the position of the [Trombone] slide for each note.
 */
class SlidePositionManager private constructor() : FingeringManager<List<Int>> {

    /** Stores the slide table. */
    private val slideTable = HashMap<Int, List<Int>>()

    override fun fingering(midiNote: Int): List<Int>? {
        return slideTable[midiNote]
    }

    companion object {
        /**
         * Instantiates a new slide position manager.
         *
         * @param clazz the class who correlates the instrument in the XML file
         */
        fun from(clazz: Class<out Instrument>): SlidePositionManager {
            val className = clazz.simpleName
            val manager = SlidePositionManager()

            /* XML Parsing */
            try {
                val instrumentList = instantiateXmlParser("/instrument_mapping.xml")
                    .documentElement.getElementsByTagName("instrument")

                /* For each instrument */
                for (i in 0 until instrumentList.length) {
                    val instrument = instrumentList.item(i)
                    val instrumentAttributes = instrument.attributes

                    /* Find instrument with matching name */
                    if (instrumentAttributes.getNamedItem("name").textContent == className) {

                        /* Ensure the mapping type is correct */
                        val mappingType = instrumentAttributes.getNamedItem("mapping-type").textContent
                        if (mappingType != "slide_position")
                            throw InvalidMappingType("XML has a mapping type of $mappingType.")

                        /* Get key mapping */
                        val mapping = (instrument as Element).getElementsByTagName("mapping").item(0)
                        val maps = (mapping as Element).getElementsByTagName("map")

                        for (j in 0 until maps.length) {
                            val note = maps.item(j)
                            val noteValue = note.attributes.getNamedItem("note").textContent.toInt()
                            val validPositions = (note as Element).getElementsByTagName("pos")
                            val validPosList: MutableList<Int> = ArrayList()
                            (0 until validPositions.length).mapTo(validPosList) { validPositions.item(it).textContent.toInt() }
                            manager.slideTable[noteValue] = validPosList
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