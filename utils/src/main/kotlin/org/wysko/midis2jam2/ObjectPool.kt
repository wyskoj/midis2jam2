package org.wysko.midis2jam2

import com.jme3.scene.Spatial

class ObjectPool(
    private val source: Spatial,
) {
    private val pool = mutableListOf<Spatial>()
    private val _inUse = mutableSetOf<Spatial>()
    val inUse: Set<Spatial> get() = _inUse

    fun obtain(): Spatial = when {
        pool.isEmpty() -> source.clone()
        else -> pool.removeAt(pool.size - 1)
    }.also {
        _inUse.add(it)
    }

    fun free(spatial: Spatial) {
        pool.add(spatial)
        _inUse.remove(spatial)
    }

    fun tick() {
        for (spatial in _inUse) {
            free(spatial)
        }
    }
}