package org.wysko.midis2jam2.instrument.common

import com.jme3.scene.Spatial
import com.jme3.scene.control.Control

interface HitAwareControl : Control {
    fun hit(velocity: Number)
}

fun Spatial.invokeHitControls(velocity: Number) {
    repeat(numControls) { i ->
        getControl(i).let {
            if (it is HitAwareControl) {
                it.hit(velocity)
            }
        }
    }
}