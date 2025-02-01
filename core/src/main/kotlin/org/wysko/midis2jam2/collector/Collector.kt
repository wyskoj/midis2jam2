package org.wysko.midis2jam2.collector

import kotlin.time.Duration

interface Collector<T> {
    fun seek(time: Duration)
    fun peek(): T?
    fun prev(): T?
}