package org.wysko.midis2jam2

import com.jme3.scene.Spatial
import com.jme3.scene.control.Control
import kotlin.reflect.KClass

class SpatialPool(
    private val source: Spatial,
    private val control: KClass<out Control>? = null,
) {
    private val pool = mutableListOf<Spatial>()
    private val _inUse = mutableSetOf<Spatial>()
    val inUse: Set<Spatial> get() = _inUse

    fun obtain(): Spatial = when {
        pool.isEmpty() -> source.clone()
        else -> pool.removeAt(pool.size - 1)
    }.also {
        _inUse.add(it)

        control?.let { control ->
            it.addControl(control.constructors.first().call())
        }
    }

    fun free(spatial: Spatial) {
        pool.add(spatial)
        _inUse.remove(spatial)
        control?.let { control ->
            spatial.removeControl(control.java)
        }
    }
}