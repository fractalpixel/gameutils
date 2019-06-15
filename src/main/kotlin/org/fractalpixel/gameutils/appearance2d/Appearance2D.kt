package org.fractalpixel.gameutils.appearance2d

import com.badlogic.gdx.math.Vector2
import org.entityflakes.Component
import org.entityflakes.PolymorphicComponent
import org.fractalpixel.gameutils.rendering.RenderingContext2D
import org.mistutils.geometry.double3.Double3

/**
 *
 */
interface Appearance2D: PolymorphicComponent {

    override val componentCategory: Class<out Component> get() = Appearance2D::class.java

    /**
     * Render this appearance to the specified context at the specified screen / world position.
     * Do not store any reference to the positions or scales, as they may be reused / changed.
     *
     * @param context contains the sprite batch to render to, and the camera to use.
     * @param screenPos screen position for the specified world position
     * @param screenScale scale of a 1,1 world-square on the screen.  May be used for scaling graphics.
     * @param worldPos position in the world.  Provided for the sake of completeness, screenPos is already calculated based on this using the camera in the context.
     */
    fun render(context: RenderingContext2D, screenPos: Vector2, screenScale: Vector2, worldPos: Double3)

}