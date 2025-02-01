package org.wysko.midis2jam2

import kotlin.time.Duration
import kotlin.time.DurationUnit

val Duration.dSeconds: Double
    get() = this.toDouble(DurationUnit.SECONDS)