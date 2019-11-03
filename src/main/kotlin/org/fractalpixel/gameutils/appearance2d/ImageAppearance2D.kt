package org.fractalpixel.gameutils.appearance2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import org.entityflakes.ComponentBase
import org.entityflakes.Entity
import org.fractalpixel.gameutils.libgdxutils.draw
import org.fractalpixel.gameutils.rendering.RenderingContext2D
import org.fractalpixel.gameutils.texture.TextureSystem
import org.kwrench.geometry.double3.Double3

/**
 * A 2D appearance that draws an image from a texture atlas.
 */
class ImageAppearance2D(initialTextureName: String?,
                        var color: Color = Color(Color.WHITE),
                        var sizeX: Float = 1f,
                        var sizeY: Float = 1f,
                        var scaleWithImageSize: Boolean = true,
                        var rotationTurns: Float = 0f,
                        var originX: Float = 0.5f,
                        var originY: Float = 0.5f,
                        var flipX: Boolean = false,
                        var flipY: Boolean = false): ComponentBase(), Appearance2D {

    private var texture: TextureRegion? = null

    var textureName: String? = initialTextureName
        set(newName) {
            field = newName
            texture = null
        }

    override fun doInit(entity: Entity) {
    }

    override fun doDispose() {
    }

    override fun render(context: RenderingContext2D, screenPos: Vector2, screenScale: Vector2, worldPos: Double3) {
        initializeTexture()
        val t = texture
        if (t != null) {
            val scaleX = screenScale.x
            val scaleY = screenScale.y
            val sx = sizeX * if (scaleWithImageSize) t.regionWidth.toFloat() else 1f
            val sy = sizeY * if (scaleWithImageSize) t.regionHeight.toFloat() else 1f
            val ox = originX * sx
            val oy = originY * sy

            context.spriteBatch.draw(t, screenPos.x, screenPos.y, ox, oy, sx, sy, scaleX, scaleY, rotationTurns * 360f, flipX, flipY, color)
        }
    }

    private fun initializeTexture() {
        val name = textureName
        if (texture == null && name != null) {
            val textureService = entity!!.world[TextureSystem::class]
            texture = textureService.getTexture(name)
        }
    }
}

