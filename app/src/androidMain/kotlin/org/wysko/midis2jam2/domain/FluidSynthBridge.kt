package org.wysko.midis2jam2.domain

/**
 * The native FluidSynth, with [channels] MIDI channels: 16 for each MIDI port of the file it plays.
 */
class FluidSynthBridge(soundfontPath: String, channels: Int) {
    internal var synthPtr: Long = 0

    init {
        synthPtr = initFluidSynth(soundfontPath, channels)
    }

    fun setChorusActive(isChorusActive: Boolean) {
        setChorusActiveImpl(synthPtr, isChorusActive)
    }

    fun setReverbActive(isReverbActive: Boolean) {
        setReverbActiveImpl(synthPtr, isReverbActive)
    }

    external fun noteOn(synthPtr: Long, channel: Int, note: Int, velocity: Int)
    external fun noteOff(synthPtr: Long, channel: Int, note: Int)
    external fun controlChange(synthPtr: Long, channel: Int, controller: Int, value: Int)
    external fun programChange(synthPtr: Long, channel: Int, program: Int)
    external fun pitchBend(synthPtr: Long, channel: Int, value: Int)
    external fun channelPressure(synthPtr: Long, channel: Int, pressure: Int)
    external fun polyPressure(synthPtr: Long, channel: Int, note: Int, pressure: Int)
    external fun sendSysex(synthPtr: Long, data: ByteArray)

    private external fun setChorusActiveImpl(synthPtr: Long, isChorusActive: Boolean)
    private external fun setReverbActiveImpl(synthPtr: Long, isReverbActive: Boolean)

    private external fun initFluidSynth(soundfontPath: String, channels: Int): Long
    private external fun closeFluidSynth(synthPtr: Long)

    fun close() {
        closeFluidSynth(synthPtr)
        synthPtr = 0
    }
}
