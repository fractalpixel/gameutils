package org.fractalpixel.gameutils.layer.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.math.Vector3
import org.entityflakes.Entity
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.libgdxutils.SingleShaderProvider
import org.fractalpixel.gameutils.rendering.RenderingContext3D


/**
 * Simple layer that just draws a screen-sized quad and uses the specified [shader] to render it.
 * The [shader] is disposed when this layer is disposed.
 */
open class ShaderLayer(val shader: Shader): Layer3D(SingleShaderProvider(shader)) {

    private lateinit var screenQuad: ModelInstance

    override fun doInit(entity: Entity) {
        // Build screen-covering quad
        val builder = ShapeBuilder()
        builder.addVertex(Vector3(-1f, 1f, 0f))
        builder.addVertex(Vector3(1f, 1f, 0f))
        builder.addVertex(Vector3(1f, -1f, 0f))
        builder.addVertex(Vector3(-1f, -1f, 0f))
        builder.addQuad(0, 1, 2, 3, updateNormals = false)
        screenQuad = ModelInstance(builder.createModel(Material(), normalizeNormals = false))
    }

    override fun render(context: RenderingContext3D) {
        context.modelBatch.render(screenQuad)
    }

    override fun doDispose() {
        screenQuad.model.dispose()
        shader.dispose()
    }

}