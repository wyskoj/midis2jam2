package org.wysko.midis2jam2.application

object ResourceLoading

fun resource(path: String) = ResourceLoading::class.java.getResourceAsStream(path)!!
