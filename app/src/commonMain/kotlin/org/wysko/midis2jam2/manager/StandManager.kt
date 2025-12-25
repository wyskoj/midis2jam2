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

package org.wysko.midis2jam2.manager

import com.charleskorn.kaml.Yaml
import com.jme3.app.Application
import com.jme3.scene.Spatial
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.wysko.midis2jam2.datastructure.Vector3fAsStruct
import org.wysko.midis2jam2.util.*

class StandManager : BaseManager() {
    @Serializable
    private data class StandConfiguration(
        val modelName: String,
        val textureName: String,
        val position: Vector3fAsStruct,
        val rotation: Vector3fAsStruct,
        val scale: Float,
        val instrumentType: KClassAsString
    )

    private lateinit var configurations: List<StandConfiguration>
    private val stands: MutableMap<StandConfiguration, Spatial> = mutableMapOf()

    override fun initialize(app: Application) {
        super.initialize(app)

        configurations = Yaml.default.decodeFromString<List<StandConfiguration>>(resourceToString("/stands.yaml"))
        for (configuration in configurations) {
            stands[configuration] = app.assetManager.loadDiffuseModel(
                modelName = configuration.modelName,
                textureName = configuration.textureName,
            ).apply {
                loc = configuration.position
                rot = configuration.rotation
                scale(configuration.scale)
            }
        }
    }

    override fun update(tpf: Float) {
        configurations.forEach { config ->
            stands[config]?.let { spatial ->
                setStandVisibility(config, spatial)
            }
        }
    }

    override fun onEnable() {
        stands.forEach { (_, spatial) -> app.rootNode.attachChild(spatial) }
    }

    override fun onDisable() {
        stands.forEach { (_, spatial) -> app.rootNode.detachChild(spatial) }
    }

    private fun setStandVisibility(configuration: StandConfiguration, stand: Spatial) {
        stand.cullHint = context.instruments.any { it::class == configuration.instrumentType && it.isVisible }.ch
    }
}