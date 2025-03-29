package org.wysko.midis2jam2.instrument.common

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.jme3ktdsl.cull
import kotlin.math.floor

class AnimatedStringControl : AbstractControl() {
    private var animationTime = 0.0

    override fun controlUpdate(tpf: Float) {
        val children = (spatial as Node).children
        animationTime = (animationTime + tpf * 60) % children.size
        children.forEachIndexed { index, frame -> frame.cullHint = (index == floor(animationTime).toInt()).cull }
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
}
