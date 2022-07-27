package org.wysko.midis2jam2.midi.search

class GeneralMidiPatch(
    val value: Int,
    private val name: String
) {
    override fun toString(): String = name

    companion object {
        fun loadList(): Collection<GeneralMidiPatch> {
            return GeneralMidiPatch.javaClass.getResource("/instruments.txt").readText().split("\n")
                .mapIndexed { index, s ->
                    GeneralMidiPatch(index, s)
                }
        }
    }
}