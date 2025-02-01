package org.wysko.midis2jam2.scene

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.light.DirectionalLight
import com.jme3.math.ColorRGBA
import com.jme3.post.FilterPostProcessor
import com.jme3.renderer.queue.RenderQueue
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import org.wysko.midis2jam2.jme3ktdsl.vec3

class ShadowsState(private val filterPostProcessor: FilterPostProcessor) : AbstractAppState() {
    private lateinit var app: SimpleApplication
    private lateinit var lightForShadows: DirectionalLight
    private lateinit var directionalLightShadowFilter: DirectionalLightShadowFilter

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        this.app = app as SimpleApplication
        this.lightForShadows = DirectionalLight().apply {
            color = ColorRGBA.Black
            direction = vec3(0.1, -1, 0.1)
        }
        this.directionalLightShadowFilter = DirectionalLightShadowFilter(app.assetManager, 1024, 1).apply {
            light = lightForShadows
            shadowIntensity = 0.3f
            edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
            edgesThickness = 10
        }

        with(this.app.rootNode) {
            shadowMode = RenderQueue.ShadowMode.CastAndReceive
            addLight(lightForShadows)
        }
        filterPostProcessor.addFilter(directionalLightShadowFilter)
    }
}