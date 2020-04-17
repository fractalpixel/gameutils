package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider
import org.fractalpixel.gameutils.libgdxutils.loadShaderProgram
import org.fractalpixel.gameutils.libgdxutils.loadShaderProvider

class VoxelTerrainShaderProvider(): BaseShaderProvider() {

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
                return DefaultShader(renderable, DefaultShader.Config(), loadShaderProgram("shaders/default"))
            }
        }
    }
}