package org.wysko.midis2jam2.gui.components.settings.card

sealed class ListPosition {
    data object First : ListPosition()
    data object Last : ListPosition()
    data object Other : ListPosition()
    data object Only : ListPosition()

    companion object {
        fun fromIndex(index: Int, size: Int): ListPosition = when {
            size == 1 -> Only
            index == 0 -> First
            index == size - 1 -> Last
            else -> Other
        }
    }
}