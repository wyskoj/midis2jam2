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

package org.wysko.midis2jam2.record

import com.jme3.app.Application
import com.jme3.post.SceneProcessor
import com.jme3.profile.AppProfiler
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.texture.FrameBuffer
import com.jme3.texture.FrameBuffer.FrameBufferTarget
import com.jme3.texture.Image
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.OUTRO
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.util.logger
import java.io.File

/**
 * Hears how a recording is going.
 */
interface RecordingListener {
    /** [framesCaptured] of about [expectedFrames] frames are in the video. */
    fun onProgress(framesCaptured: Long, expectedFrames: Long)

    /** The video is complete and written to [file]. */
    fun onFinished(file: File)

    /** The performance was closed before the video was complete; the partial file has been deleted. */
    fun onCancelled()

    /** The recording failed; the partial file has been deleted. */
    fun onFailed(exception: RecordingException)
}

/**
 * Records the performance to a video file.
 *
 * The scene and the GUI are drawn into an off-screen frame buffer the size of the video, which is read back each
 * frame and then copied to the window as a preview. Reading back our own frame buffer rather than the window's means
 * the video is right even when the window is covered or partly off-screen.
 *
 * This must be attached with a [FixedStepTimer] installed and the window sized to the video, and [complete] called
 * when playback ends.
 */
class RecordingManager(
    private val options: RecordOptions,
    private val sequence: TimeBasedSequence,
    private val synthesizer: OfflineSynthesizer,
    private val listener: RecordingListener,
) : BaseManager() {
    private val output = File(options.outputPath)
    private lateinit var timeline: RecordingTimeline
    private lateinit var frameBuffer: FrameBuffer
    private var pipeline: EncodingPipeline? = null
    private var isCaptureFrame = false
    private var isDone = false

    override fun initialize(app: Application) {
        super.initialize(app)
        val playback = app.stateManager.getState(PlaybackManager::class.java)
        timeline = RecordingTimeline(options.fps, playback.duration + OUTRO)

        frameBuffer = FrameBuffer(options.width, options.height, 1).apply {
            addColorTarget(FrameBufferTarget.newTarget(Image.Format.RGBA8))
            setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth))
        }
        // Before the first frame, so the filter post-processor picks it up as its output when it initializes.
        this.app.viewPort.outputFrameBuffer = frameBuffer
        this.app.guiViewPort.outputFrameBuffer = frameBuffer
        this.app.guiViewPort.addProcessor(Capture())
    }

    override fun update(tpf: Float) {
        if (isDone) return
        val time = app.stateManager.getState(PlaybackManager::class.java).time
        isCaptureFrame = timeline.onFrame(time)
        if (!isCaptureFrame) return

        if (pipeline == null) startEncoding()
        val expected = timeline.expectedFrames ?: return
        if (timeline.framesCaptured % options.fps == 1L) {
            listener.onProgress(timeline.framesCaptured, expected)
            val percent = (timeline.framesCaptured * 100 / expected).coerceAtMost(100)
            app.context.setTitle("midis2jam2 — Recording $percent%")
        }
    }

    /**
     * Finishes the video. Call when playback ends, then stop the application.
     */
    fun complete() {
        if (isDone) return
        isDone = true
        val pipeline = pipeline
        if (pipeline == null) {
            discard(RecordingException("The performance ended before anything was recorded."))
            return
        }
        try {
            pipeline.finish()
            listener.onFinished(output)
        } catch (e: RecordingException) {
            discard(e)
        }
    }

    override fun cleanup(app: Application?) = cancel()

    /**
     * Abandons the recording, if it hasn't finished, and deletes the partial file. Happens when the performance is
     * closed early.
     */
    fun cancel() {
        if (isDone) return
        isDone = true
        pipeline?.abort()
        output.delete()
        listener.onCancelled()
    }

    private fun startEncoding() {
        synthesizer.queue(sequence)
        val audio = synthesizer.withLeadingSilence(timeline.audioDelay)
        pipeline = EncodingPipeline(options.width, options.height) {
            VideoEncoder(output, options.width, options.height, options.fps, options.quality.crf, audio)
        }
    }

    private fun fail(exception: RecordingException) {
        isDone = true
        discard(exception)
        app.stop()
    }

    private fun discard(exception: RecordingException) {
        logger().error("Recording failed.", exception)
        pipeline?.abort()
        output.delete()
        listener.onFailed(exception)
    }

    /** Reads each finished frame back for the encoder, then shows it in the window. */
    private inner class Capture : SceneProcessor {
        private lateinit var renderManager: RenderManager

        override fun initialize(rm: RenderManager, vp: ViewPort) {
            renderManager = rm
        }

        override fun isInitialized(): Boolean = ::renderManager.isInitialized

        override fun postFrame(out: FrameBuffer?) {
            val renderer = renderManager.renderer
            if (isCaptureFrame && !isDone) {
                try {
                    pipeline?.submit { renderer.readFrameBuffer(frameBuffer, it) }
                } catch (e: RecordingException) {
                    fail(e)
                }
            }
            renderer.copyFrameBuffer(frameBuffer, null, true, false)
        }

        override fun reshape(vp: ViewPort, w: Int, h: Int) = Unit
        override fun preFrame(tpf: Float) = Unit
        override fun postQueue(rq: RenderQueue) = Unit
        override fun cleanup() = Unit
        override fun setProfiler(profiler: AppProfiler?) = Unit
    }
}
