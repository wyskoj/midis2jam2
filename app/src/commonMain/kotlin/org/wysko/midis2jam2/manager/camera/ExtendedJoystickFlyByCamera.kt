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

package org.wysko.midis2jam2.manager.camera

import com.jme3.input.*
import com.jme3.input.controls.KeyTrigger
import com.jme3.input.controls.MouseAxisTrigger
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.input.event.*
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/** How many frames of raw axis samples to average when establishing each axis's at-rest value. */
private const val CALIBRATION_FRAMES = 15

/** Deviation from an axis's calibrated rest value below which input is ignored. */
private const val STICK_DEADZONE = 0.2f

/**
 * If an axis's calibrated rest value sits further than this from zero, it's assumed to be a
 * unipolar control (e.g. an analog trigger that reports its "unpressed" state at -1, per the
 * common DirectInput/XInput/GLFW convention on Windows) rather than a spring-centered stick.
 */
private const val TRIGGER_REST_THRESHOLD = 0.6f

/** Upper bound on the tracked frame delta, so a hitch or a pause doesn't produce a huge jump. */
private const val MAX_FRAME_DELTA_SECONDS = 0.1f

/** Multiplier for right-stick look rotation. Rotation reads much less "responsive" than movement
 *  at the same nominal sensitivity, since [FlyByCamera]'s rotation speed is tuned for the tiny
 *  per-pixel deltas mouse-look produces, not a full-range analog stick - so this is tuned higher
 *  than [JOYSTICK_RISE_SENSITIVITY]. */
private const val JOYSTICK_ROTATE_SENSITIVITY = 2.5f

/** Multiplier for trigger-driven rise/lower. */
private const val JOYSTICK_RISE_SENSITIVITY = 0.5f

/** Multiplier for left-stick movement. 1.0 matches a fully-held movement key. */
private const val JOYSTICK_MOVE_SENSITIVITY = 1.0f

/**
 * Exponent applied to a joystick axis's post-deadzone deviation (`deviation^EXPONENT`, sign
 * preserved) before it drives the camera. A stick or trigger's usable range is small deflections
 * most of the time, so a linear response leaves little room to make fine adjustments - values
 * above 1 flatten the response near center (fine control) while still reaching full speed at full
 * deflection. Applies only to joystick input; keyboard and mouse are untouched.
 */
private const val RESPONSE_CURVE_EXPONENT = 2.5f

private const val LEFT_STICK_X_AXIS = 0
private const val LEFT_STICK_Y_AXIS = 1

private val MAPPINGS: Array<String> = arrayOf(
    CameraInput.FLYCAM_LEFT,
    CameraInput.FLYCAM_RIGHT,
    CameraInput.FLYCAM_UP,
    CameraInput.FLYCAM_DOWN,
    CameraInput.FLYCAM_STRAFELEFT,
    CameraInput.FLYCAM_STRAFERIGHT,
    CameraInput.FLYCAM_FORWARD,
    CameraInput.FLYCAM_BACKWARD,
    CameraInput.FLYCAM_ZOOMIN,
    CameraInput.FLYCAM_ZOOMOUT,
    CameraInput.FLYCAM_ROTATEDRAG,
    CameraInput.FLYCAM_RISE,
    CameraInput.FLYCAM_LOWER,
    CameraInput.FLYCAM_INVERTY,
)

private val INTERRUPTIBLE_ACTIONS = arrayOf(
    CameraInput.FLYCAM_ROTATEDRAG,
    CameraInput.FLYCAM_STRAFELEFT,
    CameraInput.FLYCAM_STRAFERIGHT,
    CameraInput.FLYCAM_FORWARD,
    CameraInput.FLYCAM_BACKWARD,
    CameraInput.FLYCAM_RISE,
    CameraInput.FLYCAM_LOWER,
)

/**
 * A [FlyByCamera] that additionally drives the camera from a gamepad, if one is connected and
 * [isGamepadEnabled] is `true`.
 *
 * jME3's LWJGL3/GLFW joystick backend only reliably identifies the first two raw axes (the left
 * stick) the same way across different controllers/drivers; axes beyond that have no dependable
 * cross-vendor meaning, and on many controllers the analog triggers report their "unpressed" rest
 * position at -1 rather than 0. Naively binding those axes by raw index (as earlier versions of
 * this class did) made the camera spin or drift on its own whenever such a controller was
 * connected, since a persistently nonzero reading gets applied every frame forever.
 *
 * To use axes beyond the left stick safely without knowing the controller's exact layout ahead of
 * time, this class briefly samples every other axis right after the joystick is grabbed to learn
 * its rest value, classifies it as a stick (rest near zero) or a trigger (rest near an extreme),
 * and only then starts treating *deviation from that rest value* as input - the first two
 * stick-like axes found (in index order) drive rotation, and up to two trigger-like axes drive
 * rise/lower. This works out to the correct assignment for essentially any controller layout,
 * since a right stick's X axis is universally reported before its Y axis, and a mis-resting axis
 * simply calibrates itself out rather than causing runaway motion.
 *
 * All joystick axes - including the left stick - are read directly from raw input events rather
 * than through jME's mapping system, so a response curve ([RESPONSE_CURVE_EXPONENT]) can be
 * applied to make small deflections easier to control without sacrificing full speed at full
 * deflection. Keyboard and mouse camera control are unaffected by any of this - they still go
 * through the normal mapping-based [onAnalog].
 */
class ExtendedJoystickFlyByCamera(
    camera: Camera,
    val onInput: () -> Unit = {},
    private val isGamepadEnabled: Boolean = false,
) : FlyByCamera(camera), RawInputListener {

    private var calibrationFrame = 0
    private val axisValueSums = HashMap<Int, Float>()
    private var baselines: Map<Int, Float>? = null
    private var rotateXAxis = -1
    private var rotateYAxis = -1
    private val liftAxes = mutableListOf<Int>()
    private var listeningForJoystick = false

    // RawInputListener callbacks aren't given a tpf like onAnalog is, so it's tracked by hand -
    // otherwise the calibrated axes below would apply a full -1..1 magnitude every single
    // frame regardless of framerate, rather than a rate scaled to elapsed time.
    private var lastFrameNanos = System.nanoTime()
    private var frameDeltaSeconds = 0f

    override fun registerWithInput(inputManager: InputManager) {
        this.inputManager = inputManager


        // both mouse and button - rotation of cam
        inputManager.addMapping(
            CameraInput.FLYCAM_LEFT, MouseAxisTrigger(MouseInput.AXIS_X, true),
            KeyTrigger(KeyInput.KEY_LEFT)
        )

        inputManager.addMapping(
            CameraInput.FLYCAM_RIGHT, MouseAxisTrigger(MouseInput.AXIS_X, false),
            KeyTrigger(KeyInput.KEY_RIGHT)
        )

        inputManager.addMapping(
            CameraInput.FLYCAM_UP, MouseAxisTrigger(MouseInput.AXIS_Y, false),
            KeyTrigger(KeyInput.KEY_UP)
        )

        inputManager.addMapping(
            CameraInput.FLYCAM_DOWN, MouseAxisTrigger(MouseInput.AXIS_Y, true),
            KeyTrigger(KeyInput.KEY_DOWN)
        )


        // mouse only - zoom in/out with wheel, and rotate drag
        inputManager.addMapping(CameraInput.FLYCAM_ZOOMIN, MouseAxisTrigger(MouseInput.AXIS_WHEEL, false))
        inputManager.addMapping(CameraInput.FLYCAM_ZOOMOUT, MouseAxisTrigger(MouseInput.AXIS_WHEEL, true))
        inputManager.addMapping(CameraInput.FLYCAM_ROTATEDRAG, MouseButtonTrigger(MouseInput.BUTTON_LEFT))


        // keyboard only WASD for movement and WZ for rise/lower height
        inputManager.addMapping(CameraInput.FLYCAM_STRAFELEFT, KeyTrigger(KeyInput.KEY_A))
        inputManager.addMapping(CameraInput.FLYCAM_STRAFERIGHT, KeyTrigger(KeyInput.KEY_D))
        inputManager.addMapping(CameraInput.FLYCAM_FORWARD, KeyTrigger(KeyInput.KEY_W))
        inputManager.addMapping(CameraInput.FLYCAM_BACKWARD, KeyTrigger(KeyInput.KEY_S))
        inputManager.addMapping(CameraInput.FLYCAM_RISE, KeyTrigger(KeyInput.KEY_Q))
        inputManager.addMapping(CameraInput.FLYCAM_LOWER, KeyTrigger(KeyInput.KEY_Z))

        inputManager.addListener(this, *MAPPINGS)
        inputManager.isCursorVisible = dragToRotate || !isEnabled

        if (isGamepadEnabled) {
            inputManager.joysticks?.firstOrNull()?.let {
                calibrationFrame = 0
                axisValueSums.clear()
                baselines = null
                rotateXAxis = -1
                rotateYAxis = -1
                liftAxes.clear()
                listeningForJoystick = true
                inputManager.addRawInputListener(this)
            }
        }
    }

    override fun unregisterInput() {
        if (listeningForJoystick) {
            inputManager?.removeRawInputListener(this)
            listeningForJoystick = false
        }
        super.unregisterInput()
    }

    override fun onAnalog(name: String?, value: Float, tpf: Float) {
        if (!enabled) return
        if (name in INTERRUPTIBLE_ACTIONS) onInput()

        when (name) {
            CameraInput.FLYCAM_LEFT -> rotateCamera(value, initialUpVec)
            CameraInput.FLYCAM_RIGHT -> rotateCamera(-value, initialUpVec)
            CameraInput.FLYCAM_UP -> rotateCamera(-value * (if (invertY) -1 else 1), cam.left)
            CameraInput.FLYCAM_DOWN -> rotateCamera(value * (if (invertY) -1 else 1), cam.left)
            CameraInput.FLYCAM_FORWARD -> moveCamera(value, false)
            CameraInput.FLYCAM_BACKWARD -> moveCamera(-value, false)
            CameraInput.FLYCAM_STRAFELEFT -> moveCamera(value, true)
            CameraInput.FLYCAM_STRAFERIGHT -> moveCamera(-value, true)
            CameraInput.FLYCAM_RISE -> riseCamera(value)
            CameraInput.FLYCAM_LOWER -> riseCamera(-value)
            CameraInput.FLYCAM_ZOOMIN -> zoomCamera(value)
            CameraInput.FLYCAM_ZOOMOUT -> zoomCamera(-value)
        }
    }

    private fun joyMoveCamera(value: Float, sideways: Boolean) {
        moveCamera(value * JOYSTICK_MOVE_SENSITIVITY * frameDeltaSeconds, sideways)
    }

    private fun joyRiseCamera(value: Float) {
        riseCamera(value * JOYSTICK_RISE_SENSITIVITY * frameDeltaSeconds)
    }

    private fun joyRotateCamera(value: Float, axis: Vector3f) {
        val oldCanRotate = canRotate
        canRotate = true
        rotateCamera(value * JOYSTICK_ROTATE_SENSITIVITY * frameDeltaSeconds, axis)
        canRotate = oldCanRotate
    }

    /** Applies [STICK_DEADZONE] and [RESPONSE_CURVE_EXPONENT] to a raw axis deviation, preserving its sign. */
    private fun applyDeadzoneAndCurve(deviation: Float): Float {
        val magnitude = abs(deviation)
        if (magnitude < STICK_DEADZONE) return 0f
        val normalized = ((magnitude - STICK_DEADZONE) / (1f - STICK_DEADZONE)).coerceIn(0f, 1f)
        return normalized.pow(RESPONSE_CURVE_EXPONENT) * sign(deviation)
    }

    // region RawInputListener - used to safely discover and drive the second stick/triggers
    override fun beginInput() = Unit

    override fun endInput() {
        val now = System.nanoTime()
        frameDeltaSeconds = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, MAX_FRAME_DELTA_SECONDS)
        lastFrameNanos = now

        if (baselines != null) return
        calibrationFrame++
        if (calibrationFrame >= CALIBRATION_FRAMES) {
            finishCalibration()
        }
    }

    override fun onJoyAxisEvent(evt: JoyAxisEvent) {
        if (!isGamepadEnabled || !enabled) return

        val axisId = evt.axisIndex

        // The left stick is assumed centered at rest (true across vendors, unlike axes beyond it)
        // so it needs no baseline calibration - just deadzone/curve shaping, same as the other axes.
        if (axisId == LEFT_STICK_X_AXIS || axisId == LEFT_STICK_Y_AXIS) {
            val curved = applyDeadzoneAndCurve(evt.value)
            if (curved == 0f) return
            joyMoveCamera(-curved, sideways = axisId == LEFT_STICK_X_AXIS)
            onInput()
            return
        }

        val bases = baselines
        if (bases == null) {
            axisValueSums[axisId] = (axisValueSums[axisId] ?: 0f) + evt.value
            return
        }

        val baseline = bases[axisId] ?: return
        val curved = applyDeadzoneAndCurve(evt.value - baseline)
        if (curved == 0f) return

        when (axisId) {
            rotateXAxis -> {
                joyRotateCamera(-curved, initialUpVec)
                onInput()
            }

            rotateYAxis -> {
                joyRotateCamera(curved * (if (invertY) -1 else 1), cam.left)
                onInput()
            }

            in liftAxes -> {
                val sign = if (liftAxes.indexOf(axisId) == 0) 1f else -1f
                joyRiseCamera(sign * curved)
                onInput()
            }
        }
    }

    private fun finishCalibration() {
        val bases = axisValueSums.mapValues { (_, sum) -> sum / calibrationFrame }
        baselines = bases

        val sticks = mutableListOf<Int>()
        val triggers = mutableListOf<Int>()
        for (axisId in bases.keys.sorted()) {
            val baseline = bases.getValue(axisId)
            if (abs(baseline) > TRIGGER_REST_THRESHOLD) triggers += axisId else sticks += axisId
        }

        rotateXAxis = sticks.getOrElse(0) { -1 }
        rotateYAxis = sticks.getOrElse(1) { -1 }
        liftAxes.addAll(triggers.take(2))
    }

    override fun onJoyButtonEvent(evt: JoyButtonEvent) = Unit
    override fun onMouseMotionEvent(evt: MouseMotionEvent) = Unit
    override fun onMouseButtonEvent(evt: MouseButtonEvent) = Unit
    override fun onKeyEvent(evt: KeyInputEvent) = Unit
    override fun onTouchEvent(evt: TouchEvent) = Unit
    // endregion
}
