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

package org.wysko.midis2jam2.manager.camera.cinematic.framing

import com.jme3.math.Vector3f
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.FrenchHorn
import org.wysko.midis2jam2.instrument.family.brass.StageHorns
import org.wysko.midis2jam2.instrument.family.brass.Trombone
import org.wysko.midis2jam2.instrument.family.brass.Trumpet
import org.wysko.midis2jam2.instrument.family.brass.Tuba
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TinkleBell
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani
import org.wysko.midis2jam2.instrument.family.ethnic.BagPipe
import org.wysko.midis2jam2.instrument.family.guitar.Banjo
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.Shamisen
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussive.Agogos
import org.wysko.midis2jam2.instrument.family.percussive.MelodicTom
import org.wysko.midis2jam2.instrument.family.percussive.SteelDrums
import org.wysko.midis2jam2.instrument.family.percussive.SynthDrum
import org.wysko.midis2jam2.instrument.family.percussive.TaikoDrum
import org.wysko.midis2jam2.instrument.family.percussive.Woodblocks
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.pipe.BlownBottle
import org.wysko.midis2jam2.instrument.family.pipe.Flute
import org.wysko.midis2jam2.instrument.family.pipe.Ocarina
import org.wysko.midis2jam2.instrument.family.pipe.PanFlute
import org.wysko.midis2jam2.instrument.family.pipe.Piccolo
import org.wysko.midis2jam2.instrument.family.pipe.Recorder
import org.wysko.midis2jam2.instrument.family.pipe.Whistles
import org.wysko.midis2jam2.instrument.family.reed.Clarinet
import org.wysko.midis2jam2.instrument.family.reed.Oboe
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax
import org.wysko.midis2jam2.instrument.family.soundeffects.BirdTweet
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.instrument.family.strings.Cello
import org.wysko.midis2jam2.instrument.family.strings.Fiddle
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.instrument.family.strings.Viola
import org.wysko.midis2jam2.instrument.family.strings.Violin
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Kalimba
import org.wysko.midis2jam2.manager.camera.AutoCamPosition
import kotlin.reflect.KClass

/**
 * The direction each instrument looks best from.
 *
 * The standard auto-cam's hand-placed positions ([AutoCamPosition]) were each chosen to show one instrument well:
 * a keyboard from above so the keys can be seen, a guitar face-on so the fretboard can, a drum kit from the front
 * and a little above. Instruments stand at all sorts of angles to the stage, so a direction measured from the front
 * of the stage shows many of them obliquely. Each of those positions, seen from where its instrument stands, gives
 * the direction to film that instrument from.
 *
 * The positions stand where the first of each instrument stands. Where several of an instrument are on stage, or one
 * has slid aside, the direction from it to the position is still a good guide.
 */
object PreferredViews {

    /** For each instrument, the hand-placed position that shows it. The first that matches is used. */
    private val positions: List<Pair<KClass<out Instrument>, AutoCamPosition>> = listOf(
        DrumSet::class to AutoCamPosition.DRUM_SET,
        BassGuitar::class to AutoCamPosition.BASS_GUITAR,
        Guitar::class to AutoCamPosition.GUITAR,
        Keyboard::class to AutoCamPosition.KEYBOARDS,
        SopranoSax::class to AutoCamPosition.SOPRANO_SAX,
        AltoSax::class to AutoCamPosition.ALTO_SAX,
        TenorSax::class to AutoCamPosition.TENOR_SAX,
        BaritoneSax::class to AutoCamPosition.BARITONE_SAX,
        Mallets::class to AutoCamPosition.MALLETS,
        MusicBox::class to AutoCamPosition.MUSIC_BOX,
        TelephoneRing::class to AutoCamPosition.TELEPHONE_RING,
        SpaceLaser::class to AutoCamPosition.SPACE_LASER,
        AcousticBass::class to AutoCamPosition.ACOUSTIC_BASS,
        Violin::class to AutoCamPosition.VIOLIN,
        Viola::class to AutoCamPosition.VIOLA,
        Cello::class to AutoCamPosition.CELLO,
        Harp::class to AutoCamPosition.HARP,
        StageChoir::class to AutoCamPosition.CHOIR,
        TubularBells::class to AutoCamPosition.TUBULAR_BELLS,
        StageStrings::class to AutoCamPosition.STAGE_STRINGS_1,
        StageHorns::class to AutoCamPosition.STAGE_HORNS,
        PizzicatoStrings::class to AutoCamPosition.PIZZICATO_STRINGS,
        Accordion::class to AutoCamPosition.ACCORDION,
        Banjo::class to AutoCamPosition.BANJO,
        Shamisen::class to AutoCamPosition.SHAMISEN,
        Timpani::class to AutoCamPosition.TIMPANI,
        MelodicTom::class to AutoCamPosition.MELODIC_TOM,
        SynthDrum::class to AutoCamPosition.SYNTH_DRUM,
        TaikoDrum::class to AutoCamPosition.TAIKO_DRUM,
        Trombone::class to AutoCamPosition.TROMBONE,
        Piccolo::class to AutoCamPosition.PICCOLO,
        Flute::class to AutoCamPosition.FLUTE,
        Recorder::class to AutoCamPosition.RECORDER,
        Harmonica::class to AutoCamPosition.HARMONICA,
        PanFlute::class to AutoCamPosition.PAN_FLUTE,
        Whistles::class to AutoCamPosition.WHISTLES,
        BlownBottle::class to AutoCamPosition.BLOWN_BOTTLE,
        Agogos::class to AutoCamPosition.AGOGOS,
        Woodblocks::class to AutoCamPosition.WOODBLOCKS,
        Tuba::class to AutoCamPosition.TUBA,
        FrenchHorn::class to AutoCamPosition.FRENCH_HORN,
        Trumpet::class to AutoCamPosition.TRUMPET,
        Oboe::class to AutoCamPosition.OBOE,
        Clarinet::class to AutoCamPosition.CLARINET,
        SteelDrums::class to AutoCamPosition.STEEL_DRUMS,
        Fiddle::class to AutoCamPosition.FIDDLE,
        Ocarina::class to AutoCamPosition.OCARINA,
        BagPipe::class to AutoCamPosition.BAG_PIPE,
        Kalimba::class to AutoCamPosition.KALIMBA,
        TinkleBell::class to AutoCamPosition.TINKLE_BELL,
        BirdTweet::class to AutoCamPosition.BIRD_TWEET,
    )

    /** The hand-placed position that shows [instrument], or `null` if there isn't one. */
    fun positionFor(instrument: Instrument): Vector3f? =
        positions.firstOrNull { (kind, _) -> kind.isInstance(instrument) }?.second?.location

    /**
     * The yaw and pitch, in degrees, to film [instrument] from when it stands at [center], or `null` if it has no
     * hand-placed position.
     */
    fun viewOf(instrument: Instrument, center: Vector3f): Pair<Float, Float>? {
        val position = positionFor(instrument) ?: return null
        val forward = center.subtract(position)
        if (forward.lengthSquared() < 1e-6f) return null
        return FramingSolver.anglesOf(forward)
    }
}
