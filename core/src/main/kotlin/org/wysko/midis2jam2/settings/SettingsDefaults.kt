package org.wysko.midis2jam2.settings

import java.io.File

object SettingsDefaults {

    object Graphics {
        const val IS_FULLSCREEN = false

        object Resolution {
            const val IS_USE_DEFAULT_RESOLUTION = true
            const val RESOLUTION_X = 640
            const val RESOLUTION_Y = 480
        }

        object Shadows {
            const val IS_USE_SHADOWS = true
            val SHADOWS_QUALITY = ShadowsQuality.Medium
        }

        object Antialiasing {
            const val IS_USE_ANTIALIASING = false
            val ANTIALIASING_QUALITY = AntialiasingQuality.Low
        }
    }

    object Background {
        val CONFIGURATION_TYPE = BackgroundConfigurationType.Default
        const val REPEATED_CUBE_MAP_TEXTURE = ""
        val UNIQUE_CUBE_MAP_TEXTURES: List<String> = List(6) { "" }
        const val COLOR = -16777216 // Black
    }

    object Controls {
        const val IS_SPEED_MODIFIER_KEYS_STICKY = false
        const val IS_LOCK_CURSOR = false
    }

    object Playback {
        object MidiSpecificationReset {
            const val IS_SEND_SPECIFICATION_RESET_MESSAGE = false
            val SPECIFICATION = MidiSpecification.GeneralMidi
        }

        object Soundbanks {
            val SOUNDBANKS_LIST: List<File> = listOf()
        }

        object Synthesizer {
            const val IS_USE_CHORUS = true
            const val IS_USE_REVERB = true
        }
    }

    object OnScreenElements {
        object HeadsUpDisplay {
            const val IS_SHOW_HEADS_UP_DISPLAY = true
        }

        object Lyrics {
            const val IS_SHOW_LYRICS = true
            const val LYRICS_SIZE = 1.0
        }
    }

    object Camera {
        const val IS_START_AUTOCAM_WITH_SONG = false
        const val IS_SMOOTH_FREECAM = true
        const val IS_CLASSIC_AUTOCAM = false
    }
}
