package org.wysko.midis2jam2.scene.camera

import com.jme3.app.Application
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.input.FlyByCamera
import com.jme3.renderer.Camera
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.vec3

class ExtensibleFlyByCameraState : AbstractAppState() {
    private lateinit var flyByCamera: FlyByCamera
    private lateinit var cameraProxy: Camera
    private lateinit var app: Application

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        this.app = app
        this.cameraProxy = Camera(0, 0).apply { isParallelProjection = false }
        this.flyByCamera = FlyByCamera(cameraProxy).apply {
            moveSpeed = 100f
            zoomSpeed = -20f
            isDragToRotate = true
        }.also { it.registerWithInput(app.inputManager) }

        // Set initial location
        val applyTransformation: Camera.() -> Unit = {
            location = vec3(-2, 92, 134)
            rotation = vec3(18, 180, 0).quat()
            fov = 45f
        }
        app.camera.apply(applyTransformation)
        cameraProxy.apply(applyTransformation)
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        app.camera.location = app.camera.location.interpolateLocal(cameraProxy.location, tpf * 5)
        app.camera.rotation = app.camera.rotation.also { it.slerp(cameraProxy.rotation, tpf * 5) }
        app.camera.fov = interpTo(app.camera.fov, cameraProxy.fov, tpf, 5f)
    }
}
