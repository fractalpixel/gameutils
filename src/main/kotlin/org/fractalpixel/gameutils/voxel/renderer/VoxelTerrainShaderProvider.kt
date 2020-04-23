package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider
import org.fractalpixel.gameutils.libgdxutils.loadShaderProgram
import org.fractalpixel.gameutils.libgdxutils.loadShaderProvider
import org.fractalpixel.gameutils.libgdxutils.loadTextFile

class VoxelTerrainShaderProvider(): BaseShaderProvider() {

    private val defaultVertexShader = loadTextFile("shaders/default.vertex.glsl")
    private val defaultFragmentShader = loadTextFile("shaders/default.fragment.glsl")
    private val defaultShaderConfig = DefaultShader.Config(defaultVertexShader, defaultFragmentShader)

    override fun createShader(renderable: Renderable): Shader {
        return when {
            VoxelTerrainShader.renders(renderable) -> {
                // It's part of the voxel terrain, use the voxel terrain shader.
                VoxelTerrainShader()
            }
            WireframeShader.renders(renderable) -> {
                // Wireframe for debugging, has only position and part color
                WireframeShader()
            }
            else -> {
                // Create a new default shader for this type of renderable
                // (need to pass in a config with the shader code, instead of the shader code and a default config,
                //  because the renderable type specific #defines are added based on the config and the attributes of
                //  the renderable (e.g. object diffuse color, etc.)).
                return DefaultShader(renderable, defaultShaderConfig)
            }
        }
    }
}