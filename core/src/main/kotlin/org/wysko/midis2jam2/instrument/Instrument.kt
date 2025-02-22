package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.jme3ktdsl.node
import kotlin.time.Duration

abstract class Instrument {
    val root = node()

    abstract fun tick(time: Duration, delta: Duration)
}
