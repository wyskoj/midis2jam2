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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands
import org.wysko.midis2jam2.util.Utils.exceptionToLines
import org.wysko.midis2jam2.util.Utils.instantiateXmlParser
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/** Handles fingering that uses hands. */
open class HandPositionFingeringManager : FingeringManager<Hands> {

    /** The table of fingerings. */
    private val table = HashMap<Int, Hands>()

    override fun fingering(midiNote: Int): Hands? {
        return table[midiNote]
    }

    /** A pair of indices. */
    data class Hands(
        /** Left hand index. */
        val left: Int,

        /** Right hand index. */
        val right: Int
    )

    companion object {
        /**
         * Loads the fingering manager from XML given the class
         *
         * @param clazz the class of the instrument
         * @return the hand position fingering manager
         */
        @JvmStatic
        fun from(clazz: Class<out Instrument>): HandPositionFingeringManager {
            val className = clazz.simpleName
            val manager = HandPositionFingeringManager()
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
                        if ("hands" != mappingType) {
                            Midis2jam2.getLOGGER().severe { "XML has a mapping type of $mappingType." }
                            return manager
                        }

                        /* Get key mapping */
                        val mapping = (instrument as Element).getElementsByTagName("mapping").item(0)
                        val maps = (mapping as Element).getElementsByTagName("map")
                        val mapSize = maps.length
                        /* For each defined note */
                        for (j in 0 until mapSize) {
                            val attributes = maps.item(j).attributes
                            val note = attributes.getNamedItem("note").textContent.toInt()
                            val leftHand = attributes.getNamedItem("lh").textContent.toInt()
                            val rightHand = attributes.getNamedItem("rh").textContent.toInt()
                            manager.table[note] = Hands(leftHand, rightHand)
                        }
                        break
                    }
                }
            } catch (e: SAXException) {
                Midis2jam2.getLOGGER().severe(exceptionToLines(e))
            } catch (e: IOException) {
                Midis2jam2.getLOGGER().severe(exceptionToLines(e))
            } catch (e: ParserConfigurationException) {
                Midis2jam2.getLOGGER().severe(exceptionToLines(e))
            }
            return manager
        }
    }
}