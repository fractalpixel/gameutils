package org.fractalpixel.gameutils.layer.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import org.entityflakes.World
import org.fractalpixel.gameutils.layer.Layer2D
import org.fractalpixel.gameutils.libgdxutils.FontInfo
import org.fractalpixel.gameutils.rendering.RenderingContext2D

/**
 * Shows a text in the center of the screen.
 * Suitable for Game Over and similar messages.
 */
class CenteredTextLayer(var text: String = "",
                        val font: FontInfo = FontInfo(sizePixelsHigh = 20),
                        var color: Color = Color(Color.WHITE),
                        val disposeFontOnDispose: Boolean = true): Layer2D() {

    init {
        // Render on top of 3D things
        clearDepthBuffer = true
        depth = 1.0
    }

    override fun initLayer(world: World) {
        super.initLayer(world)

        // Ensure the font is created, to avoid hickups later.
        font.getFont()
    }

    override fun doDispose() {
        // Dispose the font as no longer needed.
        // Usually ok, except if we create and destroy a lot of bigtextlayers with the same fontinfo often.
        if (disposeFontOnDispose) font.disposeFont()
    }

    var textHorizontalExtent = 0.8f

    override fun render(context: RenderingContext2D) {
        val spriteBatch = context.spriteBatch
        // spriteBatch.color = color // This won't apply to fonts....
        val fontObj = font.getFont()
        val oldColor = fontObj.color
        fontObj.color = color

        fontObj.draw(spriteBatch,
                     text,
                            0.5f * (1f - textHorizontalExtent) * Gdx.graphics.width,
                            0.5f * Gdx.graphics.height + 0.5f * font.sizePixelsHigh,
                            textHorizontalExtent * Gdx.graphics.width,
                     Align.center,
                     true)

        // Restore previous color..
        fontObj.color = oldColor
    }
}