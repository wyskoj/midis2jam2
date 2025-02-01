package org.wysko.midis2jam2.application

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.jme3ktdsl.assetManager

private const val MODEL_PREFIX = "Assets/Models/"
private const val TEXTURE_PREFIX = "Assets/Textures/"

fun PerformanceAppState.model(model: String, texture: String? = null): Spatial {
    val modelPath = prefix(model, AssetType.Model)

    return assetManager.loadModel(modelPath).also { spatial ->
        if (!modelPath.endsWith(".j3o")) {
            val texturePath = texture?.let { prefix(it, AssetType.Texture) } ?: "Assets/Textures/null.png"
            spatial.setMaterial(Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
                setTexture("DiffuseMap", assetManager.loadTexture(texturePath))
            })
        }
    }
}

fun PerformanceAppState.model(model: String, color: ColorRGBA): Spatial {
    val modelPath = prefix(model, AssetType.Model)

    return assetManager.loadModel(modelPath).also { spatial ->
        if (!modelPath.endsWith(".j3o")) {
            spatial.setMaterial(Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
                setBoolean("UseMaterialColors", true)
                setColor("Diffuse", color)
            })
        }
    }
}

private fun prefix(model: String, type: AssetType): String = when {
    model.startsWith(type.prefix) -> model
    else -> "${type.prefix}${model}"
}

private enum class AssetType(val prefix: String) {
    Model(MODEL_PREFIX),
    Texture(TEXTURE_PREFIX)
}