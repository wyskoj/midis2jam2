package org.wysko.midis2jam2.application

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.jme3ktdsl.assetManager
import org.wysko.midis2jam2.jme3ktdsl.vec3

private const val MODEL_PREFIX = "Assets/Models/"
private const val TEXTURE_PREFIX = "Assets/Textures/"

fun PerformanceAppState.modelD(model: String, texture: String? = null): Spatial {
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

fun PerformanceAppState.modelR(model: String, texture: String? = null): Spatial {
    val modelPath = prefix(model, AssetType.Model)

    return assetManager.loadModel(modelPath).also { spatial ->
        if (!modelPath.endsWith(".j3o")) {
            val texturePath = texture?.let { prefix(it, AssetType.Texture) } ?: "Assets/Textures/null.png"
            spatial.setMaterial(Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
                setVector3("FresnelParams", vec3(0.18, 0.18, 0.18))
                setBoolean("EnvMapAsSphereMap", true)
                setTexture("EnvMap", assetManager.loadTexture(texturePath))
                setTexture("DiffuseMap", assetManager.loadTexture("Assets/Textures/black.png"))
            })
        }
    }
}

fun PerformanceAppState.modelD(model: String, color: ColorRGBA): Spatial {
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