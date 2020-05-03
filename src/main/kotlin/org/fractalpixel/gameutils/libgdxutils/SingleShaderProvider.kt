package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider

/**
 * Uses the specified shader for rendering everything that this shader provider is used for.
 */
class SingleShaderProvider(val shader: Shader): BaseShaderProvider() {

    override fun createShader(renderable: Renderable): Shader {
        return shader
    }

}