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

package org.wysko.midis2jam2.manager.camera.cinematic

import com.jme3.asset.AssetManager
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.MomentKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SectionRole
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SongAnalysis
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotPlan
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How many instruments get an interest line: the ones that are most interesting over the song. */
private const val MAX_SERIES = 8

/** The interest that fills the height of the plot. Interest spikes past 1 during fills and entrances. */
private const val PLOT_CEILING = 1.5f

/** How much of the song the scrolling window shows before and after the playhead, in seconds. */
private const val SECONDS_BEHIND = 8.0
private const val SECONDS_AHEAD = 16.0
private const val WINDOW_SECONDS = SECONDS_BEHIND + SECONDS_AHEAD

/** The gap between time labels on the axis, in seconds. */
private const val AXIS_STEP = 4.0

private const val OVERVIEW_HEIGHT = 16f
private const val AXIS_HEIGHT = 14f
private const val SHOT_LANE_HEIGHT = 12f
private const val MOMENT_ROW_HEIGHT = 6f
private const val PLOT_HEIGHT = 140f
private const val SECTION_LANE_HEIGHT = 10f
private const val LANE_GAP = 4f

private val SERIES_COLORS = listOf(
    ColorRGBA(1f, 0.45f, 0.45f, 1f),
    ColorRGBA(0.45f, 0.8f, 1f, 1f),
    ColorRGBA(0.55f, 1f, 0.55f, 1f),
    ColorRGBA(1f, 0.85f, 0.35f, 1f),
    ColorRGBA(0.85f, 0.55f, 1f, 1f),
    ColorRGBA(1f, 0.65f, 0.3f, 1f),
    ColorRGBA(0.4f, 1f, 0.85f, 1f),
    ColorRGBA(1f, 0.6f, 0.85f, 1f),
)

private fun SectionRole.color(): ColorRGBA = when (this) {
    SectionRole.Intro, SectionRole.Outro -> ColorRGBA(0.45f, 0.55f, 0.75f, 0.7f)
    SectionRole.Build -> ColorRGBA(1f, 0.6f, 0.2f, 0.7f)
    SectionRole.Peak -> ColorRGBA(1f, 0.25f, 0.25f, 0.7f)
    SectionRole.Breakdown -> ColorRGBA(0.2f, 0.75f, 0.75f, 0.7f)
    SectionRole.Groove -> ColorRGBA(0.55f, 0.55f, 0.55f, 0.7f)
}

private fun MomentKind.color(): ColorRGBA = when (this) {
    MomentKind.Solo -> ColorRGBA.Yellow
    MomentKind.Entrance -> ColorRGBA.Green
    MomentKind.Fill -> ColorRGBA.Magenta
    MomentKind.Duet -> ColorRGBA.Orange
    MomentKind.Tutti -> ColorRGBA.White
    MomentKind.Breakdown -> ColorRGBA.Cyan
    MomentKind.Hit -> ColorRGBA.Red
}

/**
 * Collects coloured lines and rectangles, in pixels, into meshes. Anything beyond [minX] to [maxX] is clipped away,
 * since a GUI node can't clip its children.
 */
private class ShapeBuilder(private val minX: Float, private val maxX: Float) {
    private val linePositions = ArrayList<Float>()
    private val lineColors = ArrayList<Float>()
    private val fillPositions = ArrayList<Float>()
    private val fillColors = ArrayList<Float>()

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, color: ColorRGBA) {
        if ((x0 < minX && x1 < minX) || (x0 > maxX && x1 > maxX)) return
        var from = 0f
        var to = 1f
        val dx = x1 - x0
        if (dx != 0f) {
            val a = (minX - x0) / dx
            val b = (maxX - x0) / dx
            from = max(from, min(a, b))
            to = min(to, max(a, b))
            if (from >= to) return
        }
        linePositions += listOf(x0 + dx * from, y0 + (y1 - y0) * from, 0f, x0 + dx * to, y0 + (y1 - y0) * to, 0f)
        repeat(2) { lineColors += listOf(color.r, color.g, color.b, color.a) }
    }

    fun rect(x0: Float, y0: Float, x1: Float, y1: Float, color: ColorRGBA) {
        val left = max(x0, minX)
        val right = min(x1, maxX)
        if (right <= left) return
        fillPositions += listOf(
            left, y0, 0f, right, y0, 0f, right, y1, 0f,
            left, y0, 0f, right, y1, 0f, left, y1, 0f,
        )
        repeat(6) { fillColors += listOf(color.r, color.g, color.b, color.a) }
    }

    fun lines(): Mesh = mesh(Mesh.Mode.Lines, linePositions, lineColors)

    fun fills(): Mesh = mesh(Mesh.Mode.Triangles, fillPositions, fillColors)

    private fun mesh(mode: Mesh.Mode, positions: List<Float>, colors: List<Float>): Mesh {
        // A mesh with no vertices can't be drawn; a single degenerate shape stands in for nothing.
        val empty = positions.isEmpty()
        val count = if (mode == Mesh.Mode.Lines) 2 else 3
        return Mesh().apply {
            this.mode = mode
            setBuffer(VertexBuffer.Type.Position, 3, if (empty) FloatArray(3 * count) else positions.toFloatArray())
            setBuffer(VertexBuffer.Type.Color, 4, if (empty) FloatArray(4 * count) else colors.toFloatArray())
            updateBound()
            updateCounts()
        }
    }
}

/**
 * A scrolling strip chart of what the cinematic camera made of the song, drawn on the debug screen (F3), so the
 * analysis can be read against what is on screen.
 *
 * The big chart shows [SECONDS_BEHIND] seconds before the playhead and [SECONDS_AHEAD] after it, scrolling as the
 * song plays. The planner cuts to things a little before they happen, so what is coming matters as much as what
 * has been. From bottom to top: the time axis, the shots (alternating shades), one row per kind of moment, the plot
 * (white is the band's energy, coloured lines are each player's interest, faint lines are bar lines), and the
 * song's sections. A thin overview of the whole song runs along the very bottom, with a tick at the playhead.
 *
 * The overview is built once for an analysis and plan; the chart is rebuilt as time moves.
 */
class CinematicDebugGraph(
    private val assetManager: AssetManager,
    analysis: SongAnalysis,
    val plan: ShotPlan,
    private val width: Float,
) {
    /** The analysis this graph draws. */
    val analysisDrawn: SongAnalysis = analysis

    /** Everything the graph draws, with its origin at the bottom left. */
    val node = Node("CinematicDebugGraph")

    private val start = plan.shots.first().start
    private val span = (plan.shots.last().end - start).coerceAtLeast(1e-3)

    private val chartBase = OVERVIEW_HEIGHT + 2 * LANE_GAP
    private val shotsBase = chartBase + AXIS_HEIGHT
    private val momentsBase = shotsBase + SHOT_LANE_HEIGHT + LANE_GAP
    private val plotBase = momentsBase + MomentKind.entries.size * MOMENT_ROW_HEIGHT + LANE_GAP
    private val sectionsBase = plotBase + PLOT_HEIGHT + LANE_GAP
    private val top = sectionsBase + SECTION_LANE_HEIGHT

    private val chartMaterial = material()
    private val chartFills = Geometry("GraphFills", Mesh()).also { it.material = chartMaterial }
    private val chartLines = Geometry("GraphLines", Mesh()).also {
        it.material = material(lineWidth = 1.5f)
        it.localTranslation.z = 0.5f
    }
    private val overviewHead: Geometry
    private val axisFont: BitmapFont = assetManager.loadFont("Interface/Fonts/Console.fnt")
    private val axisLabels = List((WINDOW_SECONDS / AXIS_STEP).toInt() + 2) {
        BitmapText(axisFont).apply {
            size = 12f
            color = ColorRGBA(1f, 1f, 1f, 0.8f)
        }
    }

    private val series: List<Pair<Int, ColorRGBA>> = analysis.features.values
        .sortedByDescending { f -> f.interest.sum() }
        .take(MAX_SERIES)
        .mapIndexed { i, f -> f.subject.id to SERIES_COLORS[i % SERIES_COLORS.size] }

    private var lastTime = Double.NaN

    init {
        node.attachChild(chartFills)
        node.attachChild(chartLines)
        axisLabels.forEach { label ->
            label.setLocalTranslation(0f, chartBase + AXIS_HEIGHT - 2f, 1f)
            node.attachChild(label)
        }

        // The playhead holds still while the song scrolls past it.
        val playheadX = (SECONDS_BEHIND / WINDOW_SECONDS * width).toFloat()
        val head = ShapeBuilder(-1f, width + 1f).apply { line(playheadX, chartBase, playheadX, top, ColorRGBA.White) }
        node.attachChild(
            Geometry("GraphPlayhead", head.lines()).also {
                it.material = material(lineWidth = 2.5f)
                it.localTranslation.z = 1f
            },
        )

        val overview = ShapeBuilder(0f, width)
        drawOverview(overview)
        node.attachChild(Geometry("OverviewFills", overview.fills()).also { it.material = chartMaterial })
        node.attachChild(
            Geometry("OverviewLines", overview.lines()).also {
                it.material = material(lineWidth = 1f)
                it.localTranslation.z = 0.5f
            },
        )
        val tick = ShapeBuilder(-1f, 1f).apply { line(0f, 0f, 0f, OVERVIEW_HEIGHT, ColorRGBA.White) }
        overviewHead = Geometry("OverviewPlayhead", tick.lines()).also {
            it.material = material(lineWidth = 2.5f)
            it.localTranslation.z = 1f
        }
        node.attachChild(overviewHead)

        drawLegend()
    }

    private fun material(lineWidth: Float? = null) = Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
        setBoolean("VertexColor", true)
        additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        lineWidth?.let { additionalRenderState.lineWidth = it }
    }

    /** Moves the playhead to [time], in seconds, scrolling the chart. */
    fun update(time: Double) {
        // Through the setter: changing the vector in place doesn't tell jME the transform needs refreshing.
        overviewHead.setLocalTranslation((((time - start) / span).coerceIn(0.0, 1.0) * width).toFloat(), 0f, 1f)
        if (time == lastTime) return
        lastTime = time
        drawChart(time)
    }

    private fun drawOverview(shapes: ShapeBuilder) {
        fun x(time: Double) = (((time - start) / span) * width).toFloat()
        analysisDrawn.sections.forEach { section ->
            shapes.rect(x(section.start), 0f, x(section.end), OVERVIEW_HEIGHT, section.role.color())
            shapes.line(x(section.start), 0f, x(section.start), OVERVIEW_HEIGHT, ColorRGBA(1f, 1f, 1f, 0.6f))
        }
        val grid = analysisDrawn.grid
        for (b in 0 until grid.beatCount - 1) {
            shapes.line(
                x(grid.timeOf(b)), analysisDrawn.energy[b] * OVERVIEW_HEIGHT,
                x(grid.timeOf(b + 1)), analysisDrawn.energy[b + 1] * OVERVIEW_HEIGHT,
                ColorRGBA.White,
            )
        }
    }

    private fun drawChart(time: Double) {
        val from = time - SECONDS_BEHIND
        val to = time + SECONDS_AHEAD
        fun x(t: Double) = (((t - from) / WINDOW_SECONDS) * width).toFloat()
        val shapes = ShapeBuilder(0f, width)
        val grid = analysisDrawn.grid

        plan.shots.forEachIndexed { i, shot ->
            if (shot.end < from || shot.start > to) return@forEachIndexed
            val shade = if (i % 2 == 0) 0.35f else 0.5f
            shapes.rect(x(shot.start), shotsBase, x(shot.end), shotsBase + SHOT_LANE_HEIGHT, ColorRGBA(shade, shade, shade, 0.8f))
            shapes.line(x(shot.start), shotsBase, x(shot.start), shotsBase + SHOT_LANE_HEIGHT, ColorRGBA.White)
        }

        analysisDrawn.moments.forEach { moment ->
            if (moment.end < from || moment.start > to) return@forEach
            val y = momentsBase + MomentKind.entries.indexOf(moment.kind) * MOMENT_ROW_HEIGHT
            // A one-beat moment would be a hair; keep every moment visible.
            val right = max(x(moment.end), x(moment.start) + 3f)
            val color = moment.kind.color().clone().also { it.a = 0.4f + 0.6f * moment.strength.coerceIn(0f, 1f) }
            shapes.rect(x(moment.start), y + 1f, right, y + MOMENT_ROW_HEIGHT - 1f, color)
        }

        analysisDrawn.sections.forEach { section ->
            if (section.end < from || section.start > to) return@forEach
            shapes.rect(x(section.start), sectionsBase, x(section.end), top, section.role.color())
            shapes.line(x(section.start), sectionsBase, x(section.start), top, ColorRGBA.White)
        }

        // The plot: gridlines, bar lines, then each player's interest and the band's energy.
        for (level in listOf(0f, 0.5f, 1f)) {
            val y = plotBase + level * PLOT_HEIGHT / PLOT_CEILING
            shapes.line(0f, y, width, y, ColorRGBA(1f, 1f, 1f, if (level == 0f) 0.6f else 0.2f))
        }
        grid.barTimes.filter { it in from..to }.forEach { bar ->
            shapes.line(x(bar), plotBase, x(bar), plotBase + PLOT_HEIGHT, ColorRGBA(1f, 1f, 1f, 0.12f))
        }
        if (grid.beatCount > 1) {
            val first = (grid.beatAt(from.coerceAtLeast(0.0)) - 1).coerceAtLeast(0)
            val last = (grid.beatAt(to.coerceAtLeast(0.0)) + 1).coerceAtMost(grid.beatCount - 1)
            fun polyline(values: FloatArray, color: ColorRGBA) {
                for (b in first until last) {
                    shapes.line(
                        x(grid.timeOf(b)), plotBase + values[b].coerceIn(0f, PLOT_CEILING) * PLOT_HEIGHT / PLOT_CEILING,
                        x(grid.timeOf(b + 1)),
                        plotBase + values[b + 1].coerceIn(0f, PLOT_CEILING) * PLOT_HEIGHT / PLOT_CEILING,
                        color,
                    )
                }
            }
            series.forEach { (id, color) -> polyline(analysisDrawn.features.getValue(id).interest, color) }
            polyline(analysisDrawn.energy, ColorRGBA.White)
        }

        chartFills.mesh = shapes.fills()
        chartLines.mesh = shapes.lines()
        chartFills.updateModelBound()
        chartLines.updateModelBound()
        drawAxis(from, to, ::x)
    }

    /** Puts a time label every [AXIS_STEP] seconds of the song that is in view. */
    private fun drawAxis(from: Double, to: Double, x: (Double) -> Float) {
        val first = ceil(from / AXIS_STEP) * AXIS_STEP
        axisLabels.forEachIndexed { i, label ->
            val t = first + i * AXIS_STEP
            if (t > to) {
                label.removeFromParent()
                return@forEachIndexed
            }
            if (label.parent == null) node.attachChild(label)
            val seconds = t.roundToInt()
            val text = (if (seconds < 0) "-" else "") + "${abs(seconds) / 60}:${"%02d".format(abs(seconds) % 60)}"
            if (label.text != text) label.text = text
            label.setLocalTranslation(x(t) - label.lineWidth / 2f, chartBase + AXIS_HEIGHT - 2f, 1f)
        }
    }

    /**
     * Three rows of labels above the chart, each in the colour it explains: the players' lines, then the sections,
     * then the moments. Section colours are drawn translucent in the chart, so their labels are opaque.
     */
    private fun drawLegend() {
        fun row(index: Int, title: String, items: List<Pair<String, ColorRGBA>>) {
            var cursor = 0f
            fun label(text: String, color: ColorRGBA) {
                val bitmap = BitmapText(axisFont).apply {
                    this.text = text
                    this.color = color
                    size = 14f
                    setLocalTranslation(cursor, top + 20f + index * 18f, 1f)
                }
                node.attachChild(bitmap)
                cursor += bitmap.lineWidth + 14f
            }
            label(title, ColorRGBA(0.7f, 0.7f, 0.7f, 1f))
            items.forEach { (text, color) -> label(text, color) }
        }

        row(0, "Interest:", listOf("energy" to ColorRGBA.White) +
            series.map { (id, color) -> "${analysisDrawn.nameOf(id).take(14)}#$id" to color })
        row(
            1,
            "Sections:",
            listOf(
                "Intro/Outro" to SectionRole.Intro,
                "Build" to SectionRole.Build,
                "Peak" to SectionRole.Peak,
                "Breakdown" to SectionRole.Breakdown,
                "Groove" to SectionRole.Groove,
            ).map { (name, role) -> name to role.color().clone().also { it.a = 1f } },
        )
        row(
            2,
            "Moments (brighter = stronger):",
            MomentKind.entries.map { it.name to it.color() },
        )
    }
}
