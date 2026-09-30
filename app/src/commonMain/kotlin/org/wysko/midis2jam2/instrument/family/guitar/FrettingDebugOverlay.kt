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

package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.bounding.BoundingBox
import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.BillboardControl
import com.jme3.scene.shape.Quad
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingReadout
import org.wysko.midis2jam2.manager.FrettingDebugManager
import org.wysko.midis2jam2.manager.PerformanceManager
import kotlin.math.tan
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * The live fretting readout: text floating above a fretted instrument showing what the fretting engine inferred
 * for the whole part (tuning, capo) and what it is doing now (lead or rhythm, hand position, frets, costs).
 *
 * It hangs off the instrument's [root][org.wysko.midis2jam2.instrument.Instrument.root], so it follows the
 * instrument around the stage. It always faces the camera and is scaled with its distance from the camera, so the
 * text stays readable from any camera angle. [FrettingDebugManager] toggles it (F4); while it is hidden, no text is
 * built.
 *
 * @param context The context to the main class.
 * @param root The instrument's root node, which the readout is attached to.
 * @param geometry The instrument's geometry, which the readout floats above.
 * @param plan What the fretting engine decided.
 */
class FrettingDebugOverlay(
    private val context: PerformanceManager,
    private val root: Node,
    private val geometry: Node,
    private val plan: FrettingPlan,
) {
    /** The node holding the readout; culled while the readout is hidden. */
    val node: Node = Node("FrettingReadout").apply {
        cullHint = Spatial.CullHint.Always
        addControl(BillboardControl().apply { alignment = BillboardControl.Alignment.Screen })
    }

    private val content = Node("FrettingReadoutContent").also { node.attachChild(it) }

    /** The text of the readout. Built the first time the readout is shown, so hidden readouts cost nothing. */
    val text: BitmapText by lazy {
        BitmapText(context.app.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
            color = ColorRGBA.White
            queueBucket = RenderQueue.Bucket.Transparent
        }.also { content.attachChild(it) }
    }

    private val backgroundQuad by lazy { Quad(1f, 1f) }
    private val background by lazy {
        Geometry("FrettingReadoutBackground", backgroundQuad).apply {
            material = Material(context.app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                setColor("Color", ColorRGBA(0f, 0f, 0f, BACKGROUND_ALPHA))
                additionalRenderState.blendMode = RenderState.BlendMode.Alpha
            }
            queueBucket = RenderQueue.Bucket.Transparent
        }.also { content.attachChild(it) }
    }

    private var isAnchored = false

    init {
        root.attachChild(node)
    }

    /** Whether the readout is showing. */
    val isShowing: Boolean get() = node.cullHint != Spatial.CullHint.Always

    /** Shows or hides the readout per [FrettingDebugManager], and refreshes it for [time] when shown. */
    fun update(time: Duration) {
        val visible = context.app.stateManager.getState(FrettingDebugManager::class.java)?.isReadoutVisible == true
        if (!visible) {
            node.cullHint = Spatial.CullHint.Always
            return
        }
        if (!isAnchored) anchor()
        node.cullHint = Spatial.CullHint.Inherit
        text.text = FrettingReadout.format(plan.solution, time.toDouble(SECONDS))

        val width = text.lineWidth
        val height = text.height
        backgroundQuad.updateGeometry(width + 2 * PADDING, height + 2 * PADDING)
        background.setLocalTranslation(-PADDING, -height - PADDING, -BACKGROUND_DEPTH)
        // Bottom-centre the block on the anchor.
        content.setLocalTranslation(-width / 2, height + PADDING, 0f)
        keepLegible()
    }

    /**
     * Scales the readout with its distance from the camera, so that its text stays the same, readable size on screen
     * however near or far the camera is.
     */
    private fun keepLegible() {
        val camera = context.app.camera
        val distance = camera.location.distance(node.worldTranslation)
        val worldPerPixel = 2f * distance * tan(camera.fov * FastMath.DEG_TO_RAD / 2f) / camera.height
        node.setLocalScale(worldPerPixel * PIXELS_PER_FONT_UNIT / root.worldScale.y.coerceAtLeast(1e-6f))
    }

    /**
     * Places the readout above the middle of the instrument, from the instrument's bounds. (Not above its top: a
     * guitar's neck reaches far above its body.)
     */
    private fun anchor() {
        root.updateGeometricState()
        val bound = geometry.worldBound as? BoundingBox ?: return
        val above = Vector3f(bound.center.x, bound.center.y + bound.yExtent * ABOVE_CENTRE, bound.center.z)
        node.localTranslation = root.worldToLocal(above, null)
        isAnchored = true
    }

    private companion object {
        const val PADDING = 6f
        const val BACKGROUND_ALPHA = 0.6f
        const val BACKGROUND_DEPTH = 0.5f
        const val ABOVE_CENTRE = 0.4f

        /** How many screen pixels each unit of the font takes; the font is drawn at its native size. */
        const val PIXELS_PER_FONT_UNIT = 1.5f
    }
}
