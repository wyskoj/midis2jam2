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
package org.wysko.midis2jam2.instrument.family.guitar

import org.jetbrains.annotations.Contract
import org.w3c.dom.Element
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.Utils.exceptionToLines
import org.wysko.midis2jam2.util.Utils.instantiateXmlParser
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/** Calculates fret heights using a lookup table. */
class FretHeightByTable(
    /** The lookup table. The key is the fret and the value is the scaling. */
    private val lookupTable: Map<Int, Float>
) : FretHeightCalculator {

    @Contract(pure = true)
    override fun calculateScale(fret: Int): Float {
        return lookupTable[fret]!!
    }

    companion object {
        /**
         * Given the [name] of a [FrettedInstrument], returns the XML data defined in `fret_heights.xml` for
         * that specific instrument.
         */
        fun fromXml(name: String): FretHeightByTable {
            return try {
                val xmlDoc = instantiateXmlParser("/fret_heights.xml")
                val instrumentList = xmlDoc.documentElement.getElementsByTagName("instrument")
                val lookup: MutableMap<Int, Float> = HashMap()

                /* For each instrument */
                for (i in 0 until instrumentList.length) {
                    /* If the name of class doesn't equal the name of the currently indexed instrument, skip */
                    if (instrumentList.item(i).attributes.getNamedItem("name").textContent != name) continue

                    val instrument = instrumentList.item(i)
                    val fretHeights = (instrument as Element).getElementsByTagName("value")
                    /* For each fret height definition */
                    for (j in 0 until fretHeights.length) {
                        /* Store attributes to the lookup table */
                        val attributes = fretHeights.item(j).attributes
                        lookup[attributes.getNamedItem("fret").textContent.toInt()] =
                            attributes.getNamedItem("scale").textContent.toFloat()
                    }
                    break
                }
                FretHeightByTable(lookup)
            } catch (e: ParserConfigurationException) {
                Midis2jam2.getLOGGER()
                    .severe("Failed to load fret height from XML for $name.%n${exceptionToLines(e)}")
                FretHeightByTable(HashMap())
            } catch (e: SAXException) {
                Midis2jam2.getLOGGER()
                    .severe("Failed to load fret height from XML for $name.%n${exceptionToLines(e)}")
                FretHeightByTable(HashMap())
            } catch (e: IOException) {
                Midis2jam2.getLOGGER()
                    .severe("Failed to load fret height from XML for $name.%n${exceptionToLines(e)}")
                FretHeightByTable(HashMap())
            }
        }
    }
}