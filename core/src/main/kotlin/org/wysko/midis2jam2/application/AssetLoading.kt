package org.wysko.midis2jam2.application

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.jme3ktdsl.assetManager
import org.wysko.midis2jam2.jme3ktdsl.vec3

private const val MODEL_PREFIX = "Assets/Models/"
private const val TEXTURE_PREFIX = "Assets/Textures/"
private const val MATERIAL_PREFIX = "Assets/Materials/"
private const val DEFAULT_FRESNEL = 0.18

fun PerformanceAppState.model(model: String, material: String? = null, texture: String? = null): Spatial =
    assetManager.loadModel(prefix(model, AssetType.Model)).apply {
        material?.let { material ->
            setMaterial(
                assetManager.loadMaterial(prefix(material, AssetType.Material)).apply {
                    texture?.let { setTexture("DiffuseMap", assetManager.loadTexture(prefix(it, AssetType.Texture))) }
                }
            )
        }
        texture?.let {
            this as Geometry
            this.material.setTexture("DiffuseMap", assetManager.loadTexture(prefix(it, AssetType.Texture)))
        }
    }

fun PerformanceAppState.modelD(model: String, texture: String? = null): Spatial {
    val modelPath = prefix(model, AssetType.Model)

    return assetManager.loadModel(modelPath).also { spatial ->
        if (!modelPath.endsWith(".j3o")) {
            val texturePath = texture?.let { prefix(it, AssetType.Texture) } ?: "Assets/Textures/null.png"
            spatial.setMaterial(
                diffuseMaterial(texturePath)
            )
        }
    }
}

fun PerformanceAppState.diffuseMaterial(texturePath: String): Material {
    return Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
        setTexture("DiffuseMap", assetManager.loadTexture(prefix(texturePath, AssetType.Texture)))
    }
}

fun PerformanceAppState.modelR(model: String, texture: String? = null): Spatial {
    val modelPath = prefix(model, AssetType.Model)

    return assetManager.loadModel(modelPath).also { spatial ->
        if (!modelPath.endsWith(".j3o")) {
            val texturePath = texture?.let { prefix(it, AssetType.Texture) } ?: "Assets/Textures/null.png"
            spatial.setMaterial(reflectiveMaterial(texturePath))
        }
    }
}

fun PerformanceAppState.reflectiveMaterial(texturePath: String): Material {
    @Suppress("NAME_SHADOWING")
    val texturePath = prefix(texturePath, AssetType.Texture)
    return Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
        setVector3("FresnelParams", vec3(DEFAULT_FRESNEL, DEFAULT_FRESNEL, DEFAULT_FRESNEL))
        setBoolean("EnvMapAsSphereMap", true)
        setTexture("EnvMap", assetManager.loadTexture(texturePath))
        setTexture("DiffuseMap", assetManager.loadTexture("Assets/Textures/black.png"))
    }
}

fun PerformanceAppState.modelD(model: String, color: ColorRGBA): Spatial {
    val modelPath = prefix(model, AssetType.Model)

    return assetManager.loadModel(modelPath).also { spatial ->
        if (!modelPath.endsWith(".j3o")) {
            spatial.setMaterial(
                Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
                    setBoolean("UseMaterialColors", true)
                    setColor("Diffuse", color)
                }
            )
        }
    }
}

private fun prefix(name: String, type: AssetType): String = when {
    name.startsWith(type.prefix) -> name
    else -> "${type.prefix}$name"
}

private enum class AssetType(val prefix: String) {
    Model(MODEL_PREFIX),
    Texture(TEXTURE_PREFIX),
    Material(MATERIAL_PREFIX)
}
