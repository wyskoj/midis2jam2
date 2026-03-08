/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

@file:Suppress("TooGenericExceptionCaught")

package org.wysko.midis2jam2.starter

import Platform
import com.jme3.app.SimpleApplication
import com.jme3.system.lwjgl.LwjglContext
import org.koin.mp.KoinPlatformTools
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.DesktopPerformanceManager
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.domain.Jme3ExceptionHandler
import org.wysko.midis2jam2.manager.MidiDeviceManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.DesktopCameraManager
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.world.AssetLoader
import java.lang.invoke.MethodHandles
import javax.sound.midi.Synthesizer

internal actual class Midis2jam2Application(
    private val sequence: TimeBasedSequence,
    private val fileName: String,
    private val configurations: Collection<Configuration>,
    private val onFinish: () -> Unit,
    private val sequencer: JwSequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()

    actual fun execute() {
        try {
            applyConfigurations(configurations)
            start()
        } catch (e: Exception) {
            e.printStackTrace()
            errorLogService.addError(
                message = "There was an error applying configurations.",
                stackTrace = e.stackTraceToString()
            )
            onFinish()
        }
    }

    actual override fun simpleInitApp() {
        installGlfwJoystickCallbackWorkaround()
        Jme3ExceptionHandler.setup {
            stop()
            sequencer.stop()
            sequencer.close()
        }
        setupState(configurations, platform = Platform.Desktop)
        stateManager.attach(AssetLoader())
        val performanceAppState = DesktopPerformanceManager(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            fileName = fileName,
            configs = configurations,
        )
        stateManager.attach(performanceAppState)
        rootNode.attachChild(performanceAppState.root)
        addManagers(configurations, sequence, sequencer)
        stateManager.attach(MidiDeviceManager(configurations, midiDevice))
    }

    actual override fun stop() {
        onFinish()
        super.stop()
    }

    actual override fun destroy() {
        rootNode.detachAllChildren()
        assetManager.clearCache()
        renderer.invalidateState()
        inputManager.clearMappings()
        (context as LwjglContext).systemListener = null
        super.destroy()
    }

    /**
     * Installs a null-safe wrapper around jME3's GLFW joystick callback.
     *
     * This works around a jME3 bug where `LwjglContext.joyInput`
     * can be null when a controller disconnect event fires, causing an NPE that propagates
     * through [org.lwjgl.glfw.GLFW.glfwPollEvents] and kills the render thread.
     *
     * Implemented via reflection so that macOS builds (which use the LWJGL2 backend and
     * therefore do not have GLFW on the classpath) still compile and run normally; the
     * [ReflectiveOperationException] is silently ignored on those platforms.
     *
     * The proxy delegates all default interface methods (including `address()` from
     * [org.lwjgl.system.CallbackI], which creates the libffi native trampoline) via
     * [MethodHandles.privateLookupIn] so that LWJGL's callback registration mechanism works
     * correctly.
     */
    private fun installGlfwJoystickCallbackWorkaround() {
        try {
            val glfwClass = Class.forName("org.lwjgl.glfw.GLFW")
            val callbackIClass = Class.forName("org.lwjgl.glfw.GLFWJoystickCallbackI")
            val setCallbackMethod = glfwClass.getMethod("glfwSetJoystickCallback", callbackIClass)
            val invokeMethod = callbackIClass.getMethod("invoke", Integer.TYPE, Integer.TYPE)

            // Single-element array lets the lambda capture a mutable reference.
            val previousCallback = arrayOfNulls<Any>(1)

            val safeCallback = java.lang.reflect.Proxy.newProxyInstance(
                callbackIClass.classLoader,
                arrayOf(callbackIClass),
            ) { proxy, method, args ->
                when (method.name) {
                    "invoke" -> {
                        if (args != null) {
                            try {
                                previousCallback[0]?.let { prev -> invokeMethod.invoke(prev, args[0], args[1]) }
                            } catch (e: java.lang.reflect.InvocationTargetException) {
                                val cause = e.cause
                                if (cause !is NullPointerException) throw cause ?: e
                                // Swallow the NPE: known jME3/LWJGL3 bug where joyInput is null
                                // when a controller disconnects.
                            }
                        }
                        null
                    }
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> args != null && args.isNotEmpty() && proxy === args[0]
                    "toString" -> "${proxy.javaClass.name}@${Integer.toHexString(System.identityHashCode(proxy))}"
                    else -> {
                        // Delegate default interface methods (address(), callback(), getCallInterface())
                        // to their default implementations so the proxy functions as a valid LWJGL
                        // callback with a proper native function pointer.
                        if (method.isDefault) {
                            MethodHandles.privateLookupIn(method.declaringClass, MethodHandles.lookup())
                                .unreflectSpecial(method, method.declaringClass)
                                .bindTo(proxy)
                                .invokeWithArguments(args?.toList() ?: emptyList<Any?>())
                        } else {
                            null
                        }
                    }
                }
            }

            previousCallback[0] = setCallbackMethod.invoke(null, safeCallback)
        } catch (_: ReflectiveOperationException) {
            // GLFW is not on the classpath (macOS uses the LWJGL2 backend) or its API differs;
            // skip this optional workaround and leave normal startup unaffected.
        } catch (_: LinkageError) {
            // LWJGL classes cannot be linked (e.g., incompatible native libraries);
            // skip this optional workaround and leave normal startup unaffected.
        }
    }
}

internal actual fun getCameraManager(): CameraManager = DesktopCameraManager()