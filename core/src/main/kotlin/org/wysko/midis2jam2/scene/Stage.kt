package org.wysko.midis2jam2.scene

import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.root

fun PerformanceAppState.setupStage() {
    root += model("Stage.j3o")

    with(root) {
        createDirectionalLight(ColorRGBA(0.9f, 0.9f, 0.9f, 1f), Vector3f(0f, -1f, -1f))
        createDirectionalLight(ColorRGBA(0.1f, 0.1f, 0.3f, 1f), Vector3f(0f, 1f, 1f))
        createAmbientLight(ColorRGBA(0.5f, 0.5f, 0.5f, 1f))
    }
}

private fun Node.createDirectionalLight(colorRGBA: ColorRGBA, direction: Vector3f) =
    DirectionalLight().apply {
        this.direction = direction
        color = colorRGBA
    }.also { addLight(it) }

private fun Node.createAmbientLight(colorRGBA: ColorRGBA): AmbientLight =
    AmbientLight().apply { color = colorRGBA }.also { addLight(it) }