#include <jni.h>
#include <string>
#include "include/fluidsynth.h"
#include <unistd.h>
#include <android/log.h>
#include <sys/resource.h>

struct FluidSynthHolder {
    fluid_settings_t *settings;
    fluid_synth_t *synth;
    fluid_audio_driver_t *adriver;
};

extern "C" JNIEXPORT jlong JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_initFluidSynth(
        JNIEnv *env, jobject thiz, jstring jSoundfontPath) {
    setpriority(PRIO_PROCESS, 0, -19); // Highest priority
    const char *soundfontPath = env->GetStringUTFChars(jSoundfontPath, nullptr);
    FluidSynthHolder *holder = new FluidSynthHolder();
    holder->settings = new_fluid_settings();

    // OPTIMIZATION: Reduce buffer size for lower MIDI latency
    fluid_settings_setint(holder->settings, "audio.period-size", 64);  // Reduced from default 64-512
    fluid_settings_setint(holder->settings, "audio.periods", 2);       // Default is 16

    // OPTIMIZATION: Enable parallel rendering
    fluid_settings_setint(holder->settings, "synth.parallel-render", 1);

    fluid_settings_setint(holder->settings, "synth.reverb.active", 0);
    fluid_settings_setint(holder->settings, "synth.chorus.active", 0);


    holder->synth = new_fluid_synth(holder->settings);
    holder->adriver = new_fluid_audio_driver(holder->settings, holder->synth);
    int sf_id = fluid_synth_sfload(holder->synth, soundfontPath, 1);
    if (sf_id == FLUID_FAILED) {
        __android_log_print(ANDROID_LOG_ERROR, "FluidSynthNative", "Failed to load soundfont: %s",
                            soundfontPath);
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "FluidSynthNative",
                            "Soundfont loaded successfully, id: %d", sf_id);
    }
    env->ReleaseStringUTFChars(jSoundfontPath, soundfontPath);
    return reinterpret_cast<jlong>(holder);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_noteOn(
        JNIEnv *, jobject, jlong ptr, jint channel, jint note, jint velocity) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    fluid_synth_noteon(holder->synth, channel, note, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_noteOff(
        JNIEnv *, jobject, jlong ptr, jint channel, jint note) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    fluid_synth_noteoff(holder->synth, channel, note);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_controlChange(
        JNIEnv *, jobject, jlong ptr, jint channel, jint controller, jint value) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    fluid_synth_cc(holder->synth, channel, controller, value);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_programChange(
        JNIEnv *, jobject, jlong ptr, jint channel, jint program) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    fluid_synth_program_change(holder->synth, channel, program);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_pitchBend(
        JNIEnv *, jobject, jlong ptr, jint channel, jint value) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    fluid_synth_pitch_bend(holder->synth, channel, value);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_channelPressure(
        JNIEnv *, jobject, jlong ptr, jint channel, jint pressure) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    fluid_synth_channel_pressure(holder->synth, channel, pressure);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_polyPressure(
        JNIEnv *, jobject, jlong ptr, jint channel, jint note, jint pressure) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    // FluidSynth does not have a direct polyphonic aftertouch, so this is a placeholder
    // You may need to implement this with custom logic if needed
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_sendSysex(
        JNIEnv *, jobject, jlong ptr, jbyteArray data) {
    // FluidSynth does not support arbitrary sysex directly, so this is a placeholder
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_setChorusActiveImpl(
    JNIEnv *, jobject, jlong ptr, jboolean isChorusActive
) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    int value;
    if (isChorusActive) {
        value = 1;
    } else {
        value = 0;
    }
    fluid_settings_setint(holder->settings, "synth.chorus.active", value);
    __android_log_print(ANDROID_LOG_DEBUG, "FluidSynthNative", "Chorus: %d", value);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_setReverbActiveImpl(
    JNIEnv *, jobject, jlong ptr, jboolean isReverbActive
) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    int value;
    if (isReverbActive) {
        value = 1;
    } else {
        value = 0;
    }
    fluid_settings_setint(holder->settings, "synth.reverb.active", value);
    __android_log_print(ANDROID_LOG_DEBUG, "FluidSynthNative", "Reverb: %d", value);
}

extern "C" JNIEXPORT void JNICALL
Java_org_wysko_midis2jam2_domain_FluidSynthBridge_closeFluidSynth(
        JNIEnv *, jobject, jlong ptr) {
    auto *holder = reinterpret_cast<FluidSynthHolder *>(ptr);
    if (holder) {
        delete_fluid_audio_driver(holder->adriver);
        delete_fluid_synth(holder->synth);
        delete_fluid_settings(holder->settings);
        delete holder;
    }
}