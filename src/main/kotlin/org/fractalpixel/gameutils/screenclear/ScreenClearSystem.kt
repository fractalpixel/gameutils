package org.fractalpixel.gameutils.screenclear

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import org.entityflakes.system.SystemBase
import org.fractalpixel.gameutils.rendering.RenderingSystem

/**
 * Simple system that just clears the screen each frame.  Should be added before other rendering processors.
 */
class ScreenClearSystem(var backgroundColor: Color = Color(Color.BLACK),
                        var clearColorBuffer: Boolean = true,
                        var clearDepthBuffer: Boolean = true,
                        var clearStencilBuffer: Boolean = true): SystemBase(), RenderingSystem {

    override fun render() {
        // Set background color
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)

        // Clear other buffers
        var buffersToClear = 0
        if (clearColorBuffer) buffersToClear = buffersToClear or GL20.GL_COLOR_BUFFER_BIT
        if (clearDepthBuffer) buffersToClear = buffersToClear or GL20.GL_DEPTH_BUFFER_BIT
        if (clearStencilBuffer) buffersToClear = buffersToClear or GL20.GL_STENCIL_BUFFER_BIT
        Gdx.gl.glClear(buffersToClear)
    }
}