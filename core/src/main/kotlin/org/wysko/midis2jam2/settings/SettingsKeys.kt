package org.wysko.midis2jam2.settings

object SettingsKeys {
    object General {
        const val LOCALE = "general.locale"
        const val THEME = "general.theme"
    }

    object Graphics {
        const val IS_FULLSCREEN = "graphics.isFullscreen"

        object Resolution {
            const val IS_USE_DEFAULT_RESOLUTION = "graphics.resolution.isUseDefaultResolution"
            const val RESOLUTION_X = "graphics.resolution.resolutionX"
            const val RESOLUTION_Y = "graphics.resolution.resolutionY"
        }

        object Shadows {
            const val IS_USE_SHADOWS = "graphics.shadows.isUseShadows"
            const val SHADOWS_QUALITY = "graphics.shadows.shadowsQuality"
        }

        object Antialiasing {
            const val IS_USE_ANTIALIASING = "graphics.antialiasing.isUseAntialiasing"
            const val ANTIALIASING_QUALITY = "graphics.antialiasing.antialiasingQuality"
        }
    }

    object Background {
        const val CONFIGURATION_TYPE = "background.configurationType"
        const val REPEATED_CUBE_MAP_TEXTURE = "background.repeatedCubeMapTexture"
        const val UNIQUE_CUBE_MAP_TEXTURES = "background.uniqueCubeMapTextures"
        const val COLOR = "background.color"
    }

    object Controls {
        const val IS_SPEED_MODIFIER_KEYS_STICKY = "controls.isSpeedModifierKeysSticky"
        const val IS_LOCK_CURSOR = "controls.isLockCursor"
    }

    object Playback {
        object MidiSpecificationReset {
            const val IS_SEND_SPECIFICATION_RESET_MESSAGE = "playback.midiSpecificationReset.isSendSpecificationResetMessage"
            const val SPECIFICATION = "playback.midiSpecificationReset.specification"
        }

        object Soundbanks {
            const val SOUNDBANKS = "playback.soundbanks.soundbanks"
        }

        object Synthesizer {
            const val IS_USE_CHORUS = "playback.synthesizer.isUseChorus"
            const val IS_USE_REVERB = "playback.synthesizer.isUseReverb"
        }
    }

    object OnScreenElements {
        const val IS_SHOW_HEADS_UP_DISPLAY = "onScreenElements.isShowHeadsUpDisplay"

        object Lyrics {
            const val IS_SHOW_LYRICS = "onScreenElements.lyrics.isShowLyrics"
        }
    }

    object Camera {
        const val IS_START_AUTOCAM_WITH_SONG = "camera.isStartAutoCamWithSong"
        const val IS_SMOOTH_FREECAM = "camera.isSmoothFreecam"
        const val IS_CLASSIC_AUTOCAM = "camera.isClassicAutoCam"
    }
}