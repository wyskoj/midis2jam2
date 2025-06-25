package org.wysko.midis2jam2.domain

class FluidSynthBridge(soundfontPath: String) {
    internal var synthPtr: Long = 0

    init {
        synthPtr = initFluidSynth(soundfontPath)
    }

    external fun noteOn(synthPtr: Long, channel: Int, note: Int, velocity: Int)
    external fun noteOff(synthPtr: Long, channel: Int, note: Int)
    external fun controlChange(synthPtr: Long, channel: Int, controller: Int, value: Int)
    external fun programChange(synthPtr: Long, channel: Int, program: Int)
    external fun pitchBend(synthPtr: Long, channel: Int, value: Int)
    external fun channelPressure(synthPtr: Long, channel: Int, pressure: Int)
    external fun polyPressure(synthPtr: Long, channel: Int, note: Int, pressure: Int)
    external fun sendSysex(synthPtr: Long, data: ByteArray)

    private external fun initFluidSynth(soundfontPath: String): Long
    private external fun closeFluidSynth(synthPtr: Long)

    fun close() {
        closeFluidSynth(synthPtr)
        synthPtr = 0
    }
} 