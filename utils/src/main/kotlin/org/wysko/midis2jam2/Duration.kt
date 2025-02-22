package org.wysko.midis2jam2

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

val Duration.dSeconds: Double
    get() = this.toDouble(DurationUnit.SECONDS)

val Duration.fSeconds: Float
    get() = this.toDouble(DurationUnit.SECONDS).toFloat()

val Float.seconds: Duration
    get() = this.toDouble().seconds