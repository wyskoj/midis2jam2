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

package org.wysko.midis2jam2.starter

import com.jme3.system.AppSettings
import org.wysko.midis2jam2.starter.configuration.Resolution
import org.wysko.midis2jam2.util.isMacOs
import java.awt.GraphicsEnvironment
import java.lang.invoke.MethodHandles
import javax.imageio.ImageIO

internal actual fun AppSettings.applyIcons() {
    if (isMacOs()) return // Do not set icons on macOS

    icons = arrayOf("/ico/icon16.png", "/ico/icon32.png", "/ico/icon128.png", "/ico/icon256.png")
        .map { ImageIO.read(this::class.java.getResource(it)) }
        .toTypedArray()
}

internal actual fun AppSettings.applyScreenFrequency() {
    GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.maxOf { it.displayMode.refreshRate }.let {
        frequency = it
    }
}

internal actual fun getScreenResolution(): Resolution.CustomResolution? =
    with(GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode) {
        Resolution.CustomResolution(width, height)
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
internal fun installGlfwJoystickCallbackWorkaround() {
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