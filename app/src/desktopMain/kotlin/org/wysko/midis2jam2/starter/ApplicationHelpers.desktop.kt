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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
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

internal actual fun getScreenResolution(): Resolution? =
    with(GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode) {
        Resolution(width, height)
    }

/**
 * Strips the window decorations (title bar, borders) from the running application's window and snaps
 * it to exactly cover the primary monitor, so it behaves like a borderless-fullscreen window: no real
 * display-mode switch happens, so Alt-Tab and other monitors are unaffected.
 *
 * The exact-monitor-bounds part matters beyond cosmetics: Windows only auto-hides the taskbar behind a
 * window (the way exclusive-fullscreen games get it out of the way) when that window's rectangle
 * precisely matches the monitor's own bounds. jME sizes the window from AWT's idea of the resolution
 * (see [getScreenResolution]), which does not reliably agree with GLFW's, so any mismatch leaves a
 * sliver of the taskbar visible. Re-querying and re-applying the bounds through GLFW itself, right
 * before removing the decorations, avoids that.
 *
 * jME's [com.jme3.system.AppSettings] has no borderless-window option at all, so this reaches past it
 * into GLFW directly, the same way [installGlfwJoystickCallbackWorkaround] does. It's implemented via
 * reflection so that macOS builds (which use the LWJGL2 backend and have neither `LwjglWindow` nor GLFW
 * on the classpath) still compile.
 *
 * Callers only invoke this on Windows: it's the only platform where a borderless window reliably gets
 * the same "taskbar tucks itself away" treatment as a real fullscreen window (see the class-level note
 * in [org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.WindowMode.BorderlessFullscreen]).
 * On Wayland, `glfwSetWindowPos`/`glfwSetWindowSize` are no-ops by design (clients can't place
 * themselves), and no Linux desktop environment hides its panel just because a window's bounds match
 * the monitor's the way Windows does. macOS never reaches this function at all — it falls back to
 * exclusive fullscreen before this is called.
 */
internal fun applyBorderlessWindow(context: Any) {
    try {
        val lwjglWindowClass = Class.forName("com.jme3.system.lwjgl.LwjglWindow")
        if (!lwjglWindowClass.isInstance(context)) return

        val windowHandle = lwjglWindowClass.getMethod("getWindowHandle").invoke(context) as Long
        val glfwClass = Class.forName("org.lwjgl.glfw.GLFW")

        val monitor = glfwClass.getMethod("glfwGetPrimaryMonitor").invoke(null) as Long

        val monitorX = directIntBuffer()
        val monitorY = directIntBuffer()
        glfwClass.getMethod(
            "glfwGetMonitorPos",
            Long::class.javaPrimitiveType,
            IntBuffer::class.java,
            IntBuffer::class.java,
        ).invoke(null, monitor, monitorX, monitorY)

        val videoMode = glfwClass.getMethod("glfwGetVideoMode", Long::class.javaPrimitiveType).invoke(null, monitor)
            ?: return
        val videoModeClass = videoMode.javaClass
        val monitorWidth = videoModeClass.getMethod("width").invoke(videoMode) as Int
        val monitorHeight = videoModeClass.getMethod("height").invoke(videoMode) as Int

        val decoratedAttrib = glfwClass.getField("GLFW_DECORATED").getInt(null)
        val glfwFalse = glfwClass.getField("GLFW_FALSE").getInt(null)
        glfwClass.getMethod(
            "glfwSetWindowAttrib",
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).invoke(null, windowHandle, decoratedAttrib, glfwFalse)

        glfwClass.getMethod(
            "glfwSetWindowPos",
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).invoke(null, windowHandle, monitorX.get(0), monitorY.get(0))
        glfwClass.getMethod(
            "glfwSetWindowSize",
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).invoke(null, windowHandle, monitorWidth, monitorHeight)
    } catch (_: ReflectiveOperationException) {
        // GLFW/LwjglWindow is not on the classpath (macOS uses the LWJGL2 backend);
        // leave the window decorated and rely on exclusive fullscreen instead.
    } catch (_: LinkageError) {
        // As above.
    }
}

/** A direct, native-order [IntBuffer] of one element, suitable for an LWJGL out-parameter. */
private fun directIntBuffer(): IntBuffer =
    ByteBuffer.allocateDirect(Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).asIntBuffer()

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