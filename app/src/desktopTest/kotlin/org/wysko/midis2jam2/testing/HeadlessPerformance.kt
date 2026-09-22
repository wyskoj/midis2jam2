/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.testing

import com.jme3.app.Application
import com.jme3.app.FlyCamAppState
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AppStateManager
import com.jme3.system.JmeContext
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.InstrumentAssignment
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.CollectorsManager
import org.wysko.midis2jam2.manager.DrumSetVisibilityManager
import org.wysko.midis2jam2.manager.ManagerProfile
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.instantiateManagers
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.world.AssetLoader
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.jme3.system.AppSettings as JmeAppSettings

/**
 * Runs a real performance with no display attached.
 *
 * The engine boots on jMonkeyEngine's headless context, which supplies a working asset
 * manager, state manager, scene graph and input manager, and a renderer that draws nothing.
 * That is enough to build the band from a MIDI file - loading every model, texture and data
 * table on the way - and to step its animation.
 *
 * Time is driven explicitly rather than by the engine's own loop, so a run is deterministic:
 * [stepInstruments] advances the clock by exactly the delta it is given, however long the test
 * machine actually takes.
 *
 * Every wait here is bounded. An exception on the engine thread stops the update loop, and an
 * unbounded wait would then hang the whole suite rather than report the failure.
 */
class HeadlessPerformance private constructor(
    val app: SimpleApplication,
    val performance: TestPerformanceManager,
    private val engineFailure: AtomicReference<Throwable?>,
    /** The sequencer the performance was given, for asserting what playback asked it to do. */
    val sequencer: NoOpSequencer,
) : AutoCloseable {

    /** The instruments the assignment built for this file. */
    val instruments: List<Instrument> get() = performance.instruments

    /** The managers attached to this performance. */
    val managers: List<BaseManager> get() = performance.attachedManagers

    /** The current animation clock, as the instruments see it. */
    var time: Duration = Duration.ZERO
        private set

    /**
     * Advances the animation by [frames] frames of [delta] each, ticking every instrument
     * exactly as the performance does on screen.
     *
     * Runs on the engine thread, because instruments touch the scene graph.
     */
    fun stepInstruments(frames: Int = 1, delta: Duration = FRAME) {
        repeat(frames) {
            time += delta
            val at = time
            onEngineThread {
                performance.instruments.forEach { it.tick(at, delta) }
            }
        }
    }

    /**
     * Runs the instrument assignment for [sequence] against this already-booted performance.
     *
     * Booting the engine is the slow part, so a test that needs to know what many different
     * files produce reuses one performance and asks it about each file in turn.
     */
    fun assignFor(sequence: TimeBasedSequence): List<Instrument> = onEngineThread {
        InstrumentAssignment.assign(performance, sequence)
    }

    /** Runs [block] on the engine thread and waits for it, rethrowing whatever it throws. */
    fun <T> onEngineThread(block: () -> T): T {
        throwIfEngineFailed()
        return app.enqueue(Callable { block() }).get(ENGINE_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /** Fails with whatever the engine thread threw, if anything has. */
    fun throwIfEngineFailed() {
        engineFailure.get()?.let {
            throw AssertionError("The engine thread failed during the performance: ${it.message}", it)
        }
    }

    override fun close() {
        stopBounded(app)
    }

    /**
     * A performance that does not tick its own instruments.
     *
     * The shipped managers drive animation from the engine's wall clock; here the test drives
     * it, so that a slow or busy machine cannot change the result.
     */
    class TestPerformanceManager(
        sequence: TimeBasedSequence,
        fileName: String,
        configs: Collection<Configuration>,
    ) : PerformanceManager(sequence, fileName, configs) {

        override val onLoadingProgress: (Float) -> Unit = {}

        /** The managers attached alongside this performance, if any were asked for. */
        var attachedManagers: List<BaseManager> = emptyList()
            internal set

        override fun initialize(stateManager: AppStateManager, app: Application) {
            super.initialize(stateManager, app)
        }
    }

    companion object {

        /** A sixtieth of a second, the frame budget the app targets. */
        val FRAME: Duration = (1.0 / 60.0).seconds

        private const val BOOT_TIMEOUT_SECONDS = 180L
        private const val ENGINE_CALL_TIMEOUT_SECONDS = 180L
        private const val STOP_TIMEOUT_MILLIS = 15_000L

        /**
         * Boots a headless performance of [sequence] and waits until it is fully initialised.
         *
         * [attachManagers] attaches the shipped manager set, as the real application does,
         * and is on by default because that is the configuration users actually run. Tests
         * that only need instruments to exist can turn it off for a quicker boot, but then
         * anything reading the playback clock - drum-set visibility, for one - is absent.
         */
        fun start(
            sequence: TimeBasedSequence,
            settings: AppSettings = AppSettings(),
            fileName: String = "fixture.mid",
            attachManagers: Boolean = true,
            sequencer: NoOpSequencer = NoOpSequencer(),
        ): HeadlessPerformance {
            val configurations = listOf(
                Configuration.HomeConfiguration(),
                Configuration.AppSettingsConfiguration(settings),
            )

            val performance = TestPerformanceManager(sequence, fileName, configurations)
            val ready = CountDownLatch(1)
            val failure = AtomicReference<Throwable?>(null)

            val app = object : SimpleApplication(FlyCamAppState()) {
                private var initialised = false

                override fun simpleInitApp() {
                    // The attach order mirrors the shipped application exactly, and it
                    // matters in both directions: instruments look up the asset loader and
                    // the collector registry while still in their constructors (the state
                    // manager finds states that are attached but not yet initialised), while
                    // the drum-set visibility manager reads the instrument list and so must
                    // initialise after the performance has built it.
                    stateManager.attach(AssetLoader())
                    stateManager.attach(performance)
                    rootNode.attachChild(performance.root)

                    val managers = when {
                        attachManagers -> instantiateManagers(
                            configurations = configurations,
                            sequence = sequence,
                            sequencer = sequencer,
                            profile = ManagerProfile.Headless,
                        )

                        // The minimum an instrument needs in order to exist and be shown.
                        else -> listOf(CollectorsManager(), DrumSetVisibilityManager())
                    }
                    performance.attachedManagers = managers
                    stateManager.attachAll(*managers.toTypedArray())
                }

                override fun simpleUpdate(tpf: Float) {
                    // By the time the first update body runs, every attached state has been
                    // initialised, so the instruments exist and the test can proceed.
                    if (!initialised) {
                        initialised = true
                        ready.countDown()
                    }
                }

                override fun handleError(errorMsg: String?, t: Throwable?) {
                    failure.compareAndSet(null, t ?: IllegalStateException(errorMsg))
                    ready.countDown()
                    // Deliberately not calling super: it would tear the context down from
                    // underneath a test that is still trying to read the failure.
                }
            }

            app.setSettings(
                JmeAppSettings(true).apply {
                    audioRenderer = null
                    frameRate = 60
                    setUseJoysticks(true)
                }
            )
            app.isShowSettings = false
            app.isPauseOnLostFocus = false
            app.setDisplayStatView(false)
            app.setDisplayFps(false)

            app.start(JmeContext.Type.Headless)

            val bootedInTime = ready.await(BOOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            failure.get()?.let {
                stopBounded(app)
                throw AssertionError("The headless performance failed to initialise: ${it.message}", it)
            }
            if (!bootedInTime) {
                stopBounded(app)
                error("The headless performance did not initialise within ${BOOT_TIMEOUT_SECONDS}s")
            }

            return HeadlessPerformance(app, performance, failure, sequencer)
        }

        /**
         * Stops the engine without ever blocking forever.
         *
         * A context whose update loop has already died will never acknowledge a waiting stop,
         * so the wait happens on a thread that the caller can abandon.
         */
        private fun stopBounded(app: SimpleApplication) {
            val stopper = Thread({ runCatching { app.stop(true) } }, "headless-performance-stop")
            stopper.isDaemon = true
            stopper.start()
            stopper.join(STOP_TIMEOUT_MILLIS)
            if (stopper.isAlive) {
                runCatching { app.stop(false) }
            }
        }
    }
}
