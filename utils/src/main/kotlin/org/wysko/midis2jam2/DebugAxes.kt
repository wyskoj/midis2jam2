package org.wysko.midis2jam2

import com.jme3.app.state.BaseAppState
import com.jme3.material.Material
import com.jme3.math.ColorRGBA.*
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.debug.Arrow
import org.wysko.midis2jam2.jme3ktdsl.*

@Suppress("unused")
fun debugAxes(context: BaseAppState): Node {
    val axes = mapOf(
        Vector3f.UNIT_X to Red,
        Vector3f.UNIT_Y to Green,
        Vector3f.UNIT_Z to Blue
    )
    val node = node {
        axes.forEach { (t, u) ->
            this += Geometry("Coordinate axis", Arrow(t.mult(10f))).apply {
                material = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                    setColor("Color", u)
                }
            }
        }
    }
    return node
}