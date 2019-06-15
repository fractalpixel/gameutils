package org.fractalpixel.gameutils.layer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import org.entityflakes.ComponentBase
import org.entityflakes.Entity
import org.entityflakes.World

abstract class LayerBase : ComponentBase(), Layer {

    var clearDepthBuffer: Boolean = false
    var clearStencilBuffer: Boolean = false
    var clearColorBufferToColor: Color? = null

    override var depth: Double = 0.0

    override var visible: Boolean = true

    final override fun render() {
        clearBuffers()
        doRender()
    }

    private fun clearBuffers() {
        // Set background color
        val color = clearColorBufferToColor
        if (color != null) {
            Gdx.gl.glClearColor(color.r,
                                color.g,
                                color.b,
                                color.a)
        }

        // Clear other buffers
        var buffersToClear = 0
        if (color != null) buffersToClear = buffersToClear or GL20.GL_COLOR_BUFFER_BIT
        if (clearDepthBuffer) buffersToClear = buffersToClear or GL20.GL_DEPTH_BUFFER_BIT
        if (clearStencilBuffer) buffersToClear = buffersToClear or GL20.GL_STENCIL_BUFFER_BIT
        Gdx.gl.glClear(buffersToClear)
    }

    abstract fun doRender()

    override fun initLayer(world: World) {
        context.init(world)
    }

    override fun doInit(entity: Entity) {
    }

    override fun doDispose() {
        context.dispose()
    }
}