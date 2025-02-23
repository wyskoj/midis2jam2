package org.wysko.midis2jam2.instrument.common

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.application.resource
import org.wysko.midis2jam2.jme3ktdsl.control
import org.wysko.midis2jam2.jme3ktdsl.vec3

private typealias KeysState = List<Boolean>
private typealias PressedKeysTable = Map<Byte, KeysState>

class PressedKeyControl(
    instrument: String,
    private val keySpatialName: (Int) -> String = {
        "key-${it + 1}"
    },
    private val keyTransformation: Spatial.(Boolean) -> Unit = {
        localTranslation = vec3(0, if (it) -0.5 else 0, 0)
    },
) : AbstractControl() {
    private val table = Yaml.default.decodeFromStream<PressedKeysTable>(resource("/Instrument/$instrument.yml"))
    private var state = table.values.first().map { false }

    override fun controlUpdate(tpf: Float) {
        spatial.control<ArcControl>()?.currentArc?.let { arc ->
            table[arc.note]?.let { keysState ->
                setKeysState(keysState)
            }
        }
    }

    private fun setKeysState(keysState: KeysState) {
        if (keysState != state) {
            repeat(state.size) {
                (spatial as Node).getChild(keySpatialName(it)).keyTransformation(keysState[it])
            }
        }
        state = keysState
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
}
