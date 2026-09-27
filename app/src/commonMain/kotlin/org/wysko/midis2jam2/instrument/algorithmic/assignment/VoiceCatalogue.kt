/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.algorithmic.assignment

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.wysko.midis2jam2.util.resourceToString

/**
 * One voice (or drum kit) in a specification's sound set.
 *
 * @property program The program number **as the specifications print it**, from 1 to 128.
 * @property name The voice's name in the specification.
 * @property msb Bank select MSB (controller 0). For GS, this is the variation number.
 * @property lsb Bank select LSB (controller 32).
 * @property look The [Look] (or, for kits, [KitLook]) id this voice appears as. Without one, the voice falls back to
 * its General MIDI instrument.
 */
@Serializable
data class VoiceEntry(
    val program: Int,
    val name: String,
    val msb: Int = 0,
    val lsb: Int = 0,
    val look: String? = null,
)

/**
 * A specification's sound set, as listed in one of the `sharedAssets/voices` files.
 */
class VoiceCatalogue(val entries: List<VoiceEntry>) {

    private val byKey = entries.associateBy { Key(it.msb, it.lsb, it.program - 1) }

    /** The voice at bank [msb]/[lsb] and the zero-based [program], if the specification lists one. */
    operator fun get(msb: Int, lsb: Int, program: Int): VoiceEntry? = byKey[Key(msb, lsb, program)]

    private data class Key(val msb: Int, val lsb: Int, val program: Int)

    companion object {
        /** Parses a catalogue from its YAML [text]. */
        fun parse(text: String): VoiceCatalogue =
            VoiceCatalogue(Yaml.default.decodeFromString(ListSerializer(VoiceEntry.serializer()), text))

        /** Loads the catalogue at [resource], e.g. `/voices/gm.yaml`. */
        fun load(resource: String): VoiceCatalogue = parse(resourceToString(resource))
    }
}

/**
 * The shipped voice catalogues, loaded the first time each is needed.
 */
object VoiceCatalogues : VoiceResolver.Catalogues {
    /** General MIDI: the base every other specification falls back to. */
    override val gm: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/gm.yaml") }

    override val gs: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/gs.yaml") }

    override val xg: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/xg.yaml") }

    override val gm2: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/gm2.yaml") }

    /** The drum kits, by program, as the app has always read them. */
    override val gmKits: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/kits/gm.yaml") }

    override val gsKits: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/kits/gs.yaml") }

    override val xgKits: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/kits/xg.yaml") }

    override val gm2Kits: VoiceCatalogue by lazy { VoiceCatalogue.load("/voices/kits/gm2.yaml") }

    /** Every catalogue file, by its path under `sharedAssets`, and whether it lists kits. */
    val FILES: Map<String, Boolean> = mapOf(
        "voices/gm.yaml" to false,
        "voices/gs.yaml" to false,
        "voices/xg.yaml" to false,
        "voices/gm2.yaml" to false,
        "voices/kits/gm.yaml" to true,
        "voices/kits/gs.yaml" to true,
        "voices/kits/xg.yaml" to true,
        "voices/kits/gm2.yaml" to true,
    )
}
