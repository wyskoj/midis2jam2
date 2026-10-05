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

package org.wysko.midis2jam2.manager.camera.cinematic.planning

import org.wysko.midis2jam2.manager.camera.cinematic.analysis.Moment
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.MomentKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SectionRole
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SongAnalysis
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectKind
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/** The shortest an ordinary shot may be, in seconds. */
const val MIN_SHOT: Double = 1.5

/** The longest any shot in the body of the song may be, in seconds. */
const val MAX_SHOT: Double = 14.0

/**
 * How long every shot is held, at least, in seconds. Long enough to take in, but short enough that the camera can
 * leave a moment that is over for the next one, rather than linger.
 */
const val MIN_HOLD: Double = 2.0

/** How long before a fill lands the camera joins it, in seconds, so the viewer sees it build. */
const val FILL_LEAD: Double = 3.0

/** The longest a cutaway from a solo or a duet lasts, in seconds. */
const val CUTAWAY_HOLD: Double = 3.0

/** How many players coming in together make an arrival of the band, filmed on the whole stage. */
const val BAND_ARRIVAL: Int = 4

/** How much screen time, in seconds, halves how likely a player is to be picked for an ordinary shot. */
private const val SCREEN_TIME_HALVING = 20.0

/** How long the camera stays on a player who has just come in, at least, in seconds. */
const val ENTRANCE_HOLD: Double = 5.0

/** How long before playback starts the plan begins, in seconds. Playback starts two seconds before the music. */
private const val LEAD_IN = 2.0

/** How many bars the opening establishing shot holds once the music starts. */
private const val OPENING_BARS = 4

/** How many bars before the last note the closing shot begins. */
private const val CLOSING_BARS = 3

/** How long the closing pull-out takes after the last note, in seconds. */
private const val CLOSING_TAIL = 6.0

/** The least time between two shots of drum fills, in seconds, at normal pacing. */
private const val FILL_SPACING = 16.0

/** The least time between two shots of band hits, in seconds, at normal pacing. */
private const val HIT_SPACING = 12.0

/** The least time between two shots of entrances, in seconds, at normal pacing. */
private const val ENTRANCE_SPACING = 10.0

/** How long after an entrance the newcomer is judged over, to see whether they play a real part, in seconds. */
const val ENTRANCE_JUDGEMENT: Double = 3.0

/** How interesting a newcomer must be, against the most interesting player on stage, to be given an entrance shot. */
const val WORTHY_ENTRANCE: Float = 0.75f

/**
 * How far a shot's subject may fall behind, against how they stood against the other players as the shot began,
 * before the shot ends, as a fraction. A subject who was level with the best or ahead may fall to this fraction of
 * the best; one picked from a little behind may fall this much further.
 */
const val LOST_INTEREST: Float = 0.8f

/** How interesting a player must be, against the most interesting player on stage, to be picked for a shot. */
const val CONTENDER: Float = 0.6f

/** How far a player's interest may fall from where it was as their shot began before the shot ends, as a fraction. */
const val FADED_INTEREST: Float = 0.5f

/** The most dolly zooms in one song, and the least time between them, in seconds. */
const val MAX_DOLLY_ZOOMS: Int = 3
const val DOLLY_ZOOM_SPACING: Double = 45.0

/** The least time between two whip pans, in seconds, at normal pacing. */
const val WHIP_SPACING: Double = 25.0

/** How often an eligible cut becomes a whip pan instead. */
private const val WHIP_CHANCE = 0.2

/** How many bars a band opening holds once the music starts. */
private const val BAND_OPENING_BARS = 2

/**
 * How many bars, at most, a song that starts at full tilt stays on the band as a whole. Until the first section
 * ends, or this many bars have gone by, nobody is singled out: the whole band came in together, and no one in it has
 * yet done anything to deserve a shot of their own.
 */
private const val BAND_PHASE_BARS = 8

/** How soon after playback starts the music must begin for the song to count as starting straight away, in seconds. */
private const val IMMEDIATE_START = 1.0

/** How busy a song's first bars must be, against the song as a whole, for it to start at full tilt. */
private const val STRONG_START = 0.7

/** Moves that slide or swing the camera the same way across the stage, which repeat awkwardly when cut together. */
private val SIDEWAYS_MOVES: List<Set<Move>> = listOf(
    setOf(Move.TrackLeft, Move.ArcLeft),
    setOf(Move.TrackRight, Move.ArcRight),
)

/**
 * How far apart two shots of the same subject must be, in degrees, unless their sizes differ by at least
 * [MIN_SIZE_CHANGE] steps. This is the 30-degree rule: a smaller change reads as the picture jumping, not as a cut.
 */
const val MIN_ANGLE_CHANGE: Float = 30f

/** How many steps of [ShotSize] apart two shots of the same subject must be to cut between them at any angle. */
const val MIN_SIZE_CHANGE: Int = 2

/** Pairs of moves that travel in opposite directions, which jar when cut together. */
val OPPOSITE_MOVES: Set<Set<Move>> = setOf(
    setOf(Move.TrackLeft, Move.TrackRight),
    setOf(Move.ArcLeft, Move.ArcRight),
    setOf(Move.CraneUp, Move.CraneDown),
    setOf(Move.PushIn, Move.PullOut),
)

/** The directions shots are filmed from, in degrees of yaw. */
private val YAWS = listOf(-45f, -30f, -15f, 0f, 15f, 30f, 45f)

/** Further round, for when every usual direction is too close to the shot before. */
private val EXTREME_YAWS = listOf(-60f, 60f)

/**
 * Plans the edit of a song: which shots, in which order, cut where.
 *
 * The planner works like the director of a concert film. It opens on the whole stage and closes the same way. In
 * between, it cuts on bar lines, cuts to a soloist a beat before they start, cuts to the drummer for a fill and
 * back again on the downbeat, stays on a player who has just come in, and goes wide when the whole band is at full
 * strength. It holds shots for several seconds, longer when the song is calm, and moves on once someone else is
 * clearly more interesting than the player it's on, or stops playing. It follows the editor's rules for cutting: it
 * never cuts from an instrument straight back to the same instrument (a long solo is intercut with the band), and a
 * move is never cut into its opposite. It only films instruments that will stay on stage, and stay put, for the
 * whole shot. Now and then it whips round from one player to the next instead of cutting, and it saves the dolly
 * zoom for the song's biggest moments.
 *
 * Planning is deterministic: the same song and seed always give the same edit.
 */
object ShotPlanner {

    /**
     * Plans the edit of [analysis] with [seed].
     *
     * @param pacing How long shots last, as a multiple of normal: below 1 cuts faster, above 1 holds longer.
     */
    fun plan(analysis: SongAnalysis, seed: Long, pacing: Float = 1f): ShotPlan =
        ShotPlan(Director(analysis, Random(seed), pacing.coerceIn(0.4f, 2.5f)).plan(), seed)

    /** The direction the camera looks from at the start of a shot with [spec] lasting [length] seconds. */
    fun startYaw(spec: ShotSpec, length: Double): Float = spec.move.rig(spec.yaw, spec.pitch, 0.0, length).yaw

    /** The direction the camera looks from at the end of a shot with [spec] lasting [length] seconds. */
    fun endYaw(spec: ShotSpec, length: Double): Float = spec.move.rig(spec.yaw, spec.pitch, length, length).yaw
}

/** One planning pass. */
private class Director(val analysis: SongAnalysis, val random: Random, val pacing: Float) {
    private val grid = analysis.grid
    private val shots = mutableListOf<PlannedShot>()
    private val used = mutableSetOf<Moment>()
    private var lastFillShot = Double.NEGATIVE_INFINITY
    private var lastHitShot = Double.NEGATIVE_INFINITY
    private var lastEntranceShot = Double.NEGATIVE_INFINITY

    /** How long the shot being planned lasts, in seconds. */
    private var draftLength = 0.0
    private var featureStreak = 0
    private var featured: Moment? = null

    /** When the band's opening ends, if the song started at full tilt: until then, the camera stays on the band. */
    private var bandPhaseEnd = Double.NEGATIVE_INFINITY

    /** The song's biggest moments, strongest first and well apart, each filmed once with a dolly zoom. */
    private val dollyMoments: List<Moment> = buildList {
        analysis.moments
            .filter { it.kind in setOf(MomentKind.Hit, MomentKind.Tutti, MomentKind.Solo) && it.strength >= 0.6f }
            .sortedByDescending { it.strength }
            .forEach { moment ->
                if (size < MAX_DOLLY_ZOOMS && none { abs(it.start - moment.start) < DOLLY_ZOOM_SPACING }) add(moment)
            }
    }
    private val dollied = mutableSetOf<Moment>()
    private var lastWhip = Double.NEGATIVE_INFINITY

    fun plan(): List<PlannedShot> {
        val planStart = minOf(0.0, analysis.musicStart) - LEAD_IN
        if (analysis.musicEnd <= analysis.musicStart) {
            return listOf(PlannedShot(planStart, planStart + MAX_SHOT, establishing(Move.Static), "no music"))
        }

        // A song that starts at full tilt opens on the band playing, close enough to see. One that eases in opens on
        // the stage.
        val immediate = analysis.musicStart <= IMMEDIATE_START && startsStrong()
        val openingBars = if (immediate) BAND_OPENING_BARS else OPENING_BARS
        val openingEnd = snapToBar(analysis.musicStart + openingBars * grid.secondsPerBarAt(analysis.musicStart))
            .coerceAtLeast(planStart + MIN_HOLD)
        val closingStart = snapToBar(analysis.musicEnd - CLOSING_BARS * grid.secondsPerBarAt(analysis.musicEnd))
            .coerceAtLeast(openingEnd)

        draftLength = openingEnd - planStart
        val band = analysis.subjects.map { it.id }
            .filter { analysis.isOnStageThroughout(it, analysis.musicStart, openingEnd) }
            .sortedByDescending { analysis.interestOver(it, analysis.musicStart, openingEnd) }
            .take(3)
        val opening = if (immediate && band.isNotEmpty()) {
            val size = if (band.size > 1) ShotSize.Wide else ShotSize.Medium
            shot(band, size, approaching(), LensChoice.Wide) to "opening: the band"
        } else {
            establishing(approaching(), LensChoice.Wide) to "opening"
        }
        shots += PlannedShot(planStart, openingEnd, opening.first, opening.second)
        if (immediate) {
            val firstSectionEnd = analysis.sections.firstOrNull { it.end > openingEnd + 1e-6 }?.end ?: openingEnd
            val cap = analysis.musicStart + BAND_PHASE_BARS * grid.secondsPerBarAt(analysis.musicStart)
            bandPhaseEnd = snapToBar(minOf(firstSectionEnd, cap)).coerceAtLeast(openingEnd)
        }

        var cursor = openingEnd
        while (closingStart - cursor >= MIN_SHOT) {
            shots += nextShot(cursor, closingStart)
            cursor = shots.last().end
        }

        // Cutting from one shot of the stage to another reads as a stutter: the ending takes over instead.
        if (shots.size > 1 && shots.last().spec.size == ShotSize.Establishing) cursor = shots.removeLast().start

        val closingEnd = maxOf(analysis.musicEnd + CLOSING_TAIL, cursor + MIN_SHOT)
        draftLength = closingEnd - cursor
        val closing = establishing(pick(Move.PullOut to 0.6, Move.CraneUp to 0.4))
        shots += PlannedShot(cursor, closingEnd, closing, "closing", settles = true)
        return shots
    }

    /** Whether the song's first bars are about as busy as the song as a whole, rather than a quiet lead-in. */
    private fun startsStrong(): Boolean {
        val first = grid.beatAt(analysis.musicStart)
        val opening = first until (first + BAND_OPENING_BARS * grid.beatsPerBarAt(first)).coerceAtMost(grid.beatCount)
        val music = first..grid.beatAt(analysis.musicEnd)
        if (opening.isEmpty()) return false
        val openingEnergy = opening.map { analysis.energy[it] }.average()
        val songEnergy = music.map { analysis.energy[it] }.average()
        return openingEnergy >= STRONG_START * songEnergy
    }

    /** Plans the shot that starts at [cursor], ending no later than [limit]. */
    private fun nextShot(cursor: Double, limit: Double): PlannedShot {
        val section = analysis.sectionAt(cursor)
        val beat = grid.secondsPerBeatAt(grid.beatAt(cursor))
        val bar = grid.secondsPerBarAt(cursor)
        // How long to hold the shot, in seconds, so pacing doesn't depend on tempo. Rounded to whole bars, so the
        // cut lands on a bar line.
        val baseSeconds = when (section?.role) {
            SectionRole.Peak -> 5.0
            SectionRole.Build -> 6.0
            SectionRole.Breakdown, SectionRole.Intro, SectionRole.Outro -> 9.0
            else -> 7.0
        }
        val active = activeMoment(cursor, beat)?.takeUnless { cursor < bandPhaseEnd - 1e-6 && it.kind.isFeature }
        val isEntrance = active?.kind == MomentKind.Entrance
        val seconds = baseSeconds * pacing * (0.8 + random.nextDouble() * 0.4)
        val bars = (seconds / bar).roundToInt().coerceAtLeast(1)
        var targetLength = (bars * bar).coerceIn(MIN_SHOT, MAX_SHOT)
        if (isEntrance) targetLength = ENTRANCE_HOLD

        // However much happens next, a shot is held long enough to take in.
        val hold = when {
            isEntrance -> ENTRANCE_HOLD
            else -> MIN_HOLD
        }
        val upcoming = upcomingMoment(cursor, hold, targetLength, beat, bar)

        // When the shot ends: when its accent is over, just before the next moment, or on a bar line.
        var end = when {
            // A fill shot shows the crash the fill lands on, then cuts away a beat later.
            active != null && active.kind == MomentKind.Fill -> snapToBeat(active.end + beat)
            active != null && active.kind == MomentKind.Hit -> ceilBeat(maxOf(active.end, cursor + FILL_LEAD))
            upcoming != null -> snapToBeat(upcoming.start - anticipation(upcoming, beat))
            else -> snapToBar(cursor + targetLength)
        }
        // A solo or duet shot cuts away when the feature ends. An entrance shot stays on the newcomer.
        val isFeature = active?.kind == MomentKind.Solo || active?.kind == MomentKind.Duet
        if (active != null && isFeature && active.end >= cursor + hold && active.end < end) {
            end = snapToBeat(active.end)
        }
        val minimum = hold
        if (end - cursor > MAX_SHOT) end = floorBeat(cursor + MAX_SHOT)
        if (end - cursor < minimum) end = ceilBeat(cursor + minimum)
        if (limit - end < MIN_SHOT) end = limit

        draftLength = end - cursor
        val (spec, reason) = chooseShot(active, cursor, end)
        // A cutaway from a solo or a duet is a glance at the band, and the camera goes back to the feature.
        if (reason.startsWith("cutaway") && end - cursor > CUTAWAY_HOLD) {
            end = ceilBeat(cursor + CUTAWAY_HOLD)
            if (limit - end < MIN_SHOT) end = limit
        }
        val transition = transitionInto(spec, reason, cursor)
        if (active?.kind.isAccent) return PlannedShot(cursor, end, spec, reason, transition = transition)
        // A soloist is the feature for as long as the solo lasts: their shot ends only if they stop or fade.
        val featured = reason.startsWith("solo") || reason.startsWith("duet")
        val ending = endWhenInterestMoves(spec, cursor, end, hold, limit, mayBeUpstaged = !featured)
        return PlannedShot(cursor, ending, spec, reason, transition = transition)
    }

    /**
     * Whether to whip round into a shot of [spec] at [cursor] rather than cut. Only now and then, only from one
     * player to another, and never into or out of an accent, the opening or a dolly zoom.
     */
    private fun transitionInto(spec: ShotSpec, reason: String, cursor: Double): Transition {
        val previous = shots.lastOrNull() ?: return Transition.Cut
        val calm = listOf(previous.reason, reason).none {
            it.startsWith("fill") || it.startsWith("hit") || it.startsWith("climax") || it.startsWith("opening") ||
                it.startsWith("arrival")
        }
        val eligible = shots.size >= 2 && calm &&
            previous.spec.subjects.size == 1 && spec.subjects.size == 1 &&
            !looksSame(previous.spec.subjects, spec.subjects) &&
            cursor - lastWhip >= WHIP_SPACING * pacing
        if (!eligible || random.nextDouble() >= WHIP_CHANCE) return Transition.Cut
        lastWhip = cursor
        return Transition.Whip
    }

    /**
     * When a shot of one player should end: at the first bar line, once it has been held for [hold], where they
     * play nothing, or have faded to well below where they were when the shot began, or someone else on stage is
     * more interesting than they are. A shot of a player who has stopped doing much, or has been upstaged, cuts away
     * rather than lingering.
     */
    private fun endWhenInterestMoves(
        spec: ShotSpec,
        cursor: Double,
        end: Double,
        hold: Double,
        limit: Double,
        mayBeUpstaged: Boolean,
    ): Double {
        val subject = spec.subjects.singleOrNull() ?: return end
        val openingBar = grid.secondsPerBarAt(cursor)
        val opening = analysis.interestOver(subject, cursor, cursor + openingBar) / openingBar
        val upstaged = LOST_INTEREST * analysis.standingOf(subject, cursor, cursor + openingBar).coerceAtMost(1f)
        halfBars(cursor + hold, end)
            .filter { (from, to) -> to <= end + 1e-6 && limit - from >= MIN_SHOT }
            .forEach { (from, to) ->
                val faded = analysis.interestOver(subject, from, to) / (to - from) < FADED_INTEREST * opening
                val lost = mayBeUpstaged && analysis.standingOf(subject, from, to) < upstaged
                if (!analysis.playsDuring(subject, from, to) || faded || lost) return from
            }
        return end
    }

    /**
     * The halves of the bars from [from] (rounded up to the next half bar) on, until [until], as start and end
     * times. A bar with an odd number of beats is taken whole.
     */
    private fun halfBars(from: Double, until: Double): Sequence<Pair<Double, Double>> = grid.barTimes.asSequence()
        .takeWhile { it < until - 1e-6 }
        .flatMap { barStart ->
            val beats = grid.beatsPerBarAt(grid.beatAt(barStart))
            val barEnd = barStart + grid.secondsPerBarAt(barStart)
            if (beats % 2 == 0) {
                val middle = grid.timeOf(grid.beatAt(barStart) + beats / 2)
                sequenceOf(barStart to middle, middle to barEnd)
            } else {
                sequenceOf(barStart to barEnd)
            }
        }
        .filter { (start, _) -> start >= from - 1e-6 && start < until - 1e-6 }

    /**
     * Whether the newcomer in [entrance] plays a real part, rather than coming in to play a note or two. They are
     * judged from a bar after they come in, once the stir of their arrival has passed.
     */
    private fun isWorthy(entrance: Moment): Boolean {
        if (entrance.subjects.isEmpty()) return false
        // Several players coming in at once changes the song, whatever each of them plays.
        if (entrance.subjects.size > 1) return true
        val newcomer = entrance.subjects.single()
        val from = entrance.start + grid.secondsPerBarAt(entrance.start)
        val until = from + ENTRANCE_JUDGEMENT
        val own = analysis.interestOver(newcomer, from, until)
        val best = analysis.subjects.map { it.id }
            .filter { it != newcomer && analysis.isOnStageThroughout(it, from, until) }
            .maxOfOrNull { analysis.interestOver(it, from, until) } ?: 0f
        return own >= WORTHY_ENTRANCE * best
    }

    /** The most important moment happening at [cursor] that the plan hasn't spent yet. */
    private fun activeMoment(cursor: Double, beat: Double): Moment? {
        val feature = featureAt(cursor, beat)
        return analysis.moments
            .asSequence()
            .filter { it !in used || !it.kind.isOneShot }
            .filter { it.start - anticipation(it, beat) <= cursor + 0.5 * beat && it.end > cursor + 0.5 * beat }
            .filter { !it.kind.isOneShot || it.start >= cursor - beat }
            .filter { isAllowedNow(it, cursor) }
            .filterNot { it.interrupts(feature, beat) }
            .sortedWith(compareByDescending<Moment> { it.kind.priority }.thenByDescending { it.strength })
            .firstOrNull()
    }

    /** The solo or duet under way at [cursor], if any. */
    private fun featureAt(cursor: Double, beat: Double): Moment? = analysis.moments
        .filter { it.kind == MomentKind.Solo || it.kind == MomentKind.Duet }
        .filter { it.start - beat <= cursor + 0.5 * beat && it.end > cursor + 0.5 * beat }
        .maxByOrNull { it.strength }

    /**
     * Whether cutting to this accent would take the camera off a soloist or duet before they finish. The camera joins
     * a fill before it begins, so that is when it would leave.
     */
    private fun Moment.interrupts(feature: Moment?, beat: Double): Boolean =
        feature != null && kind.isAccent && start - anticipation(this, beat) < feature.end - 1e-6

    /** The first moment worth cutting for that starts soon after [cursor], once the shot has been held for [hold]. */
    private fun upcomingMoment(cursor: Double, hold: Double, targetLength: Double, beat: Double, bar: Double): Moment? {
        val feature = featureAt(cursor, beat)
        return analysis.moments
            .asSequence()
            .filter { it.kind in CUT_FOR && it !in used }
            .filterNot { it.kind.isFeature && it.start < bandPhaseEnd - 1e-6 }
            .filterNot { it.interrupts(feature, beat) }
            .filter { it.start - anticipation(it, beat) >= cursor + hold }
            .filter { it.start <= cursor + targetLength + bar / 2 }
            .filter { isAllowedNow(it, it.start) }
            .filter { m -> m.subjects.any { analysis.isOnStageThroughout(it, m.start, m.end) } }
            .minByOrNull { it.start }
    }

    /** Whether [moment] can be filmed at [time] without filming too many fills, hits or entrances in a row. */
    private fun isAllowedNow(moment: Moment, time: Double): Boolean = when (moment.kind) {
        MomentKind.Fill -> time - lastFillShot >= FILL_SPACING * pacing
        MomentKind.Hit -> time - lastHitShot >= HIT_SPACING * pacing
        MomentKind.Entrance -> time - lastEntranceShot >= ENTRANCE_SPACING * pacing && isWorthy(moment)
        else -> true
    }

    /**
     * How long before [moment] to cut to it. A player about to start gets a beat's warning. A fill is joined a few
     * seconds early, so the viewer sees it build to the crash it lands on. A hit is cut on.
     */
    private fun anticipation(moment: Moment, beat: Double): Double = when (moment.kind) {
        MomentKind.Hit -> 0.0
        MomentKind.Fill -> ceil((FILL_LEAD - (moment.end - moment.start) - beat).coerceAtLeast(0.0) / beat) * beat
        else -> beat
    }

    /** Chooses what to film from [cursor] to [end], and says why. */
    private fun chooseShot(active: Moment?, cursor: Double, end: Double): Pair<ShotSpec, String> {
        val onStage = active?.subjects
            ?.filter { analysis.isOnStageThroughout(it, cursor, end) && !analysis.movesDuring(it, cursor, end) }
            .orEmpty()
        val isClimax = active != null && active in dollyMoments && active !in dollied &&
            onStage.isNotEmpty() && !looksSame(shots.lastOrNull()?.spec?.subjects.orEmpty(), onStage.take(1))
        // While the band that came in together is still settling in, the shots are of the band.
        if (active == null && cursor < bandPhaseEnd - 1e-6) {
            featureStreak = 0
            return band(cursor, end)
        }
        // The band at full strength, or dropping out, sets the mood of a stretch of shots rather than its subject.
        if (active == null || onStage.isEmpty() || (active.kind.isMood && !isClimax)) {
            featureStreak = 0
            return general(cursor, end, active?.takeIf { it.kind.isMood })
        }

        if (active.kind.isOneShot) used += active
        if (active != featured) {
            featured = active
            featureStreak = 0
        }

        if (isClimax) {
            dollied += active!!
            return shot(onStage.take(1), ShotSize.Medium, Move.DollyZoom, LensChoice.Normal) to "climax: $active"
        }

        return when (active.kind) {
            MomentKind.Fill -> {
                lastFillShot = cursor
                val size = pick(ShotSize.CloseUp to 0.5, ShotSize.Medium to 0.35, ShotSize.Insert to 0.15)
                shot(onStage, size, pick(Move.Overhead to 0.35, Move.Static to 0.45, Move.PushIn to 0.2)) to
                    "fill: $active"
            }

            MomentKind.Hit -> {
                lastHitShot = cursor
                val size = pick(ShotSize.Establishing to 0.4, ShotSize.Wide to 0.6)
                val subjects = if (size == ShotSize.Establishing) emptyList() else onStage.take(4)
                shot(subjects, size, pick(Move.CraneUp to 0.3, Move.PushIn to 0.2, Move.Static to 0.5)) to "hit: $active"
            }

            MomentKind.Entrance -> {
                lastEntranceShot = cursor
                when {
                    // The band coming in: the whole stage, so every newcomer can be seen. Cutting from one shot of the
                    // stage to another stutters, so after one, the newcomers are filmed as a group.
                    onStage.size >= BAND_ARRIVAL && shots.lastOrNull()?.spec?.size != ShotSize.Establishing ->
                        establishing(pick(Move.PullOut to 0.4, Move.CraneUp to 0.3, Move.Static to 0.3)) to
                            "arrival: $active"

                    onStage.size > 1 ->
                        shot(onStage, ShotSize.Wide, pick(Move.Static to 0.4, Move.PushIn to 0.3, Move.CraneDown to 0.3)) to
                            "arrival: $active"

                    else -> {
                        val size = pick(ShotSize.Medium to 0.6, ShotSize.CloseUp to 0.4)
                        val move = pick(
                            Move.Static to 0.4, Move.PushIn to 0.35, Move.CraneDown to 0.15, Move.TrackLeft to 0.05,
                            Move.TrackRight to 0.05,
                        )
                        shot(onStage, size, move) to "entrance: $active"
                    }
                }
            }

            MomentKind.Solo -> solo(active, onStage, cursor, end)

            MomentKind.Duet -> if (featureStreak++ >= 1) {
                // A long duet is intercut with the band, like a long solo.
                featureStreak = 0
                cutaway(active, onStage, cursor, end)
            } else if (onStage.size >= 2 && random.nextDouble() < 0.6) {
                val move = pick(Move.Static to 0.3, Move.TrackLeft to 0.2, Move.TrackRight to 0.2, Move.PushIn to 0.3)
                shot(onStage, ShotSize.Medium, move) to "duet: $active"
            } else {
                // Alternate between the two players.
                val last = shots.lastOrNull()?.spec?.subjects.orEmpty()
                val next = onStage.firstOrNull { !looksSame(listOf(it), last) }
                if (next == null) {
                    shot(onStage, ShotSize.Medium, generalMove()) to "duet: $active"
                } else {
                    shot(listOf(next), pick(ShotSize.Medium to 0.5, ShotSize.CloseUp to 0.5), generalMove()) to
                        "duet: $active"
                }
            }

            MomentKind.Tutti, MomentKind.Breakdown -> general(cursor, end, active)
        }
    }


    /**
     * A shot of a soloist. A long solo is intercut with the rest of the band: cutting from the soloist straight back
     * to the soloist looks like a stutter, so the shot after one of the soloist looks elsewhere.
     */
    private fun solo(moment: Moment, onStage: List<Int>, cursor: Double, end: Double): Pair<ShotSpec, String> {
        if (looksSame(shots.lastOrNull()?.spec?.subjects.orEmpty(), onStage.take(1))) {
            return cutaway(moment, onStage.take(1), cursor, end)
        }
        featureStreak++
        val size = pick(ShotSize.CloseUp to 0.45, ShotSize.Medium to 0.4, ShotSize.Insert to 0.15)
        val move = pick(
            Move.Static to 0.3, Move.PushIn to 0.3, Move.ArcLeft to 0.1, Move.ArcRight to 0.1,
            Move.TrackLeft to 0.08, Move.TrackRight to 0.08, Move.CraneDown to 0.04,
        )
        return shot(onStage.take(1), size, move) to "solo: $moment"
    }

    /** A shot of someone other than the [featured] players of [moment], or of the stage if no one else will do. */
    private fun cutaway(moment: Moment, featured: List<Int>, cursor: Double, end: Double): Pair<ShotSpec, String> {
        val others = filmable(cursor, end).filter { id -> featured.none { looksSame(listOf(id), listOf(it)) } }
        if (others.isEmpty()) return establishing(generalMove()) to "cutaway: $moment"
        return shot(listOf(weightedSubject(others, cursor, end)), ShotSize.Medium, generalMove()) to
            "cutaway: $moment"
    }

    /**
     * A shot when no one player stands out: an interesting player, a group, or the whole stage.
     *
     * A [mood] shapes the shot. With the band at full strength ([MomentKind.Tutti]), the camera goes wide and moves
     * big. When the band drops out ([MomentKind.Breakdown]), it comes in close and slows down.
     */
    /**
     * The players who can be filmed from [cursor] to [end]: on stage and staying put throughout, playing as the shot
     * begins, and not the instrument just filmed.
     */
    private fun filmable(cursor: Double, end: Double): List<Int> {
        val previous = shots.lastOrNull()?.spec?.subjects.orEmpty()
        return analysis.subjects.map { it.id }.filter { id ->
            analysis.isOnStageThroughout(id, cursor, end) &&
                !analysis.movesDuring(id, cursor, end) &&
                analysis.playsDuring(id, cursor, minOf(end, cursor + MIN_HOLD)) &&
                !(previous.size == 1 && looksSame(previous, listOf(id)))
        }
    }

    /**
     * A shot of the band that came in together: its most interesting players that can be filmed, as a group, or the
     * whole stage now and then. No one is singled out, however much they stand out in the first moments.
     */
    private fun band(cursor: Double, end: Double): Pair<ShotSpec, String> {
        val players = analysis.subjects.map { it.id }
            .filter {
                analysis.isOnStageThroughout(it, cursor, end) && !analysis.movesDuring(it, cursor, end) &&
                    analysis.playsDuring(it, cursor, end)
            }
            .sortedByDescending { analysis.interestOver(it, cursor, end) }
            .take(BAND_ARRIVAL)
        val lastWasEstablishing = shots.lastOrNull()?.spec?.size == ShotSize.Establishing
        if (players.isEmpty() || (!lastWasEstablishing && random.nextDouble() < 0.3)) {
            return establishing(approaching(), LensChoice.Wide) to "opening: the band"
        }
        val size = if (players.size > 1) ShotSize.Wide else ShotSize.Medium
        return shot(players, size, approaching(), LensChoice.Wide) to "opening: the band"
    }

    private fun general(cursor: Double, end: Double, mood: Moment? = null): Pair<ShotSpec, String> {
        val candidates = filmable(cursor, end)
        val role = analysis.sectionAt(cursor)?.role
        val full = mood?.kind == MomentKind.Tutti || role == SectionRole.Peak
        val sparse = mood?.kind == MomentKind.Breakdown || role == SectionRole.Breakdown
        val label = when (mood?.kind) {
            MomentKind.Tutti -> "tutti"
            MomentKind.Breakdown -> "breakdown"
            else -> null
        }
        val establishingChance = when {
            full -> 0.25
            sparse -> 0.05
            else -> 0.1
        }
        val lastWasEstablishing = shots.lastOrNull()?.spec?.size == ShotSize.Establishing

        if (candidates.isEmpty() || (!lastWasEstablishing && random.nextDouble() < establishingChance)) {
            val move = pick(
                Move.StageSweep to 0.1, Move.CraneUp to 0.2, Move.TrackLeft to 0.15, Move.TrackRight to 0.15,
                Move.PullOut to 0.25, Move.Static to 0.15,
            )
            return establishing(move) to "${label ?: "wide"}: the whole stage"
        }

        if (full && candidates.size >= 3 && random.nextDouble() < 0.35) {
            val group = candidates.sortedByDescending { analysis.interestOver(it, cursor, end) }.take(3)
            val move = pick(
                Move.CraneUp to 0.25, Move.TrackLeft to 0.2, Move.TrackRight to 0.2, Move.ArcLeft to 0.15,
                Move.ArcRight to 0.15, Move.PushIn to 0.05,
            )
            return shot(group, ShotSize.Wide, move) to "${label ?: "group"}: $group"
        }

        // Only a player who holds their own against everyone on stage, including whoever was just filmed. If no one
        // does, the stage makes a better shot than someone with nothing much to watch.
        val bar = grid.secondsPerBarAt(cursor)
        val best = analysis.subjects.map { it.id }
            .filter { analysis.isOnStageThroughout(it, cursor, cursor + bar) }
            .maxOfOrNull { analysis.interestOver(it, cursor, cursor + bar) } ?: 0f
        val contenders = candidates.filter { analysis.interestOver(it, cursor, cursor + bar) >= CONTENDER * best }
        if (contenders.isEmpty() && !lastWasEstablishing) {
            return establishing(pick(Move.CraneUp to 0.3, Move.PullOut to 0.3, Move.Static to 0.4)) to
                "${label ?: "wide"}: the whole stage"
        }
        val subject = weightedSubject(contenders.ifEmpty { candidates }, cursor, end)
        val name = "${label ?: "interest"}: #$subject"
        if (sparse) {
            val size = pick(ShotSize.CloseUp to 0.45, ShotSize.Medium to 0.4, ShotSize.Insert to 0.15)
            val lens = pick(LensChoice.Long to 0.5, LensChoice.Normal to 0.5)
            val move = pick(Move.Static to 0.4, Move.PushIn to 0.4, Move.Pedestal to 0.2)
            return shot(listOf(subject), size, move, lens) to name
        }
        // A single player in a wide shot is one small thing among many: it's hard to tell what the shot is about.
        val size = pick(ShotSize.Medium to 0.5, ShotSize.CloseUp to 0.35, ShotSize.Insert to 0.15)
        return shot(listOf(subject), size, generalMove()) to name
    }

    /**
     * Picks one of [candidates], favouring the most interesting, those not filmed recently, and those who have had
     * less screen time: over a song, everyone worth watching should be seen.
     */
    private fun weightedSubject(candidates: List<Int>, cursor: Double, end: Double): Int {
        val beats = (grid.beatAt(end) - grid.beatAt(cursor)).coerceAtLeast(1)
        val recent = shots.takeLast(2).flatMap { it.spec.subjects }.toSet()
        return pickWeighted(
            candidates.map { id ->
                val interest = analysis.interestOver(id, cursor, end) / beats
                val screenTime = shots.filter { id in it.spec.subjects }.sumOf { it.length }
                val weight = (interest + 0.05).pow(2) *
                    (if (id in recent) 0.35 else 1.0) /
                    (1.0 + screenTime / SCREEN_TIME_HALVING)
                id to weight
            }
        )
    }

    /**
     * A move for the opening: the camera is coming to the scene, so it holds still or draws in, never away. The
     * opening is also filmed on a wide lens, which takes in the same view from closer, so the camera isn't left
     * standing off from the stage.
     */
    private fun approaching(): Move = pick(Move.PushIn to 0.45, Move.CraneDown to 0.3, Move.Static to 0.25)

    private fun generalMove(): Move = pick(
        Move.Static to 0.35, Move.PushIn to 0.15, Move.PullOut to 0.08, Move.TrackLeft to 0.09, Move.TrackRight to 0.09,
        Move.ArcLeft to 0.07, Move.ArcRight to 0.07, Move.CraneUp to 0.05, Move.CraneDown to 0.05,
    )

    private fun establishing(move: Move, lens: LensChoice? = null): ShotSpec =
        shot(emptyList(), ShotSize.Establishing, move, lens ?: pick(LensChoice.Wide to 0.3, LensChoice.Normal to 0.7))

    /**
     * Fills in the angle and lens of a shot of [subjects]. Cutting from one shot of an instrument to another of the
     * same instrument at the same size looks like a stutter, so the size changes.
     */
    private fun shot(subjects: List<Int>, wantedSize: ShotSize, move: Move, lens: LensChoice? = null): ShotSpec {
        val previous = shots.lastOrNull()?.spec
        val sameInstruments = previous != null && subjects.isNotEmpty() && looksSame(previous.subjects, subjects)
        val size = if (sameInstruments && previous!!.size == wantedSize) {
            when (wantedSize) {
                ShotSize.Medium -> pick(ShotSize.CloseUp to 0.5, ShotSize.Insert to 0.5)
                ShotSize.CloseUp, ShotSize.Insert -> ShotSize.Medium
                ShotSize.Wide -> ShotSize.Medium
                ShotSize.Establishing -> ShotSize.Wide
            }
        } else {
            wantedSize
        }
        val pitch = when (size) {
            ShotSize.Establishing -> random.nextFloat() * 10f + 16f
            ShotSize.Wide -> random.nextFloat() * 10f + 12f
            ShotSize.Medium -> random.nextFloat() * 10f + 6f
            ShotSize.CloseUp -> random.nextFloat() * 10f + 2f
            ShotSize.Insert -> random.nextFloat() * 20f + 25f
        }
        val chosenLens = lens ?: when (size) {
            ShotSize.Establishing -> pick(LensChoice.Wide to 0.3, LensChoice.Normal to 0.7)
            ShotSize.Wide -> pick(LensChoice.Normal to 0.7, LensChoice.Wide to 0.3)
            ShotSize.Medium -> pick(LensChoice.Normal to 0.5, LensChoice.Long to 0.5)
            ShotSize.CloseUp -> pick(LensChoice.Normal to 0.35, LensChoice.Long to 0.65)
            ShotSize.Insert -> LensChoice.Normal
        }
        val draft = ShotSpec(subjects, size, 0f, pitch, chosenLens, move)
        return draft.copy(yaw = chooseYaw(draft))
    }

    /**
     * Chooses the direction to film [draft] from. Cutting to the same subjects at nearly the same size needs a change
     * of angle of at least [MIN_ANGLE_CHANGE], or the cut reads as a jump.
     */
    private fun chooseYaw(draft: ShotSpec): Float {
        val previous = shots.lastOrNull()?.spec
        val needsNewAngle = previous != null &&
            looksSame(previous.subjects, draft.subjects) &&
            abs(previous.size.ordinal - draft.size.ordinal) < MIN_SIZE_CHANGE
        val options = if (needsNewAngle) {
            val previousYaw = ShotPlanner.endYaw(previous!!, shots.last().length)
            fun change(yaw: Float) = abs(ShotPlanner.startYaw(draft.copy(yaw = yaw), draftLength) - previousYaw)
            YAWS.filter { change(it) >= MIN_ANGLE_CHANGE }
                .ifEmpty { listOf(EXTREME_YAWS.maxBy { change(it) }) }
        } else {
            YAWS
        }
        // Drums and keyboards read best from slightly above and in front.
        val preferFront = draft.subjects.singleOrNull()?.let { analysis.kindOf(it) } in
            setOf(SubjectKind.Drums, SubjectKind.Keys)
        return if (preferFront) options.minBy { abs(it) * 0.5f + random.nextFloat() * 30f } else options.random(random)
    }

    /** Whether shots of [a] and [b] show the same instruments, or instruments that look the same. */
    private fun looksSame(a: List<Int>, b: List<Int>): Boolean =
        a.map(analysis::nameOf).sorted() == b.map(analysis::nameOf).sorted()

    /**
     * Picks one option with probability in proportion to its weight. A move just used is less likely, as is one
     * that slides the same way across the stage, and a move in the opposite direction to the one just used is never
     * picked if anything else will do: cutting from one to the other jars.
     */
    private fun <T> pick(vararg options: Pair<T, Double>): T {
        val previousMove = shots.lastOrNull()?.spec?.move
        val weighted = options.map { (value, weight) ->
            value to when {
                value is Move && OPPOSITE_MOVES.any { value in it && previousMove in it && value != previousMove } -> 0.0
                value is Move && SIDEWAYS_MOVES.any { value in it && previousMove in it } -> weight * 0.25
                value == previousMove -> weight * 0.5
                else -> weight
            }
        }
        return pickWeighted(if (weighted.any { it.second > 0 }) weighted else options.toList())
    }

    private fun <T> pickWeighted(options: List<Pair<T, Double>>): T {
        val total = options.sumOf { it.second }
        var roll = random.nextDouble() * total
        options.forEach { (value, weight) ->
            roll -= weight
            if (roll <= 0) return value
        }
        return options.last().first
    }

    private fun snapToBar(time: Double): Double =
        grid.nearestBarLine(time, grid.secondsPerBarAt(time) / 2) ?: grid.nearestBeat(time)

    private fun snapToBeat(time: Double): Double = grid.nearestBeat(time)

    private fun floorBeat(time: Double): Double = grid.timeOf(grid.beatAt(time))

    private fun ceilBeat(time: Double): Double {
        val beat = grid.beatAt(time)
        return if (grid.timeOf(beat) >= time - 1e-9) grid.timeOf(beat) else grid.timeOf(beat + 1)
    }

    private val MomentKind.isFeature: Boolean get() = this == MomentKind.Solo || this == MomentKind.Duet

    private val MomentKind?.isAccent: Boolean get() = this == MomentKind.Fill || this == MomentKind.Hit

    /** Moments that set the mood of a stretch of shots rather than choosing their subject. */
    private val MomentKind.isMood: Boolean get() = this == MomentKind.Tutti || this == MomentKind.Breakdown

    private val MomentKind.isOneShot: Boolean
        get() = this == MomentKind.Fill || this == MomentKind.Hit || this == MomentKind.Entrance

    private companion object {
        /** The moments worth ending a shot early for. */
        val CUT_FOR = setOf(MomentKind.Fill, MomentKind.Hit, MomentKind.Entrance, MomentKind.Solo, MomentKind.Duet)
    }
}
