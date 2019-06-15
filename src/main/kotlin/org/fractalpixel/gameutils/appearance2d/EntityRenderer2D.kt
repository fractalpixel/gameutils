package org.fractalpixel.gameutils.appearance2d

import org.entityflakes.Disposable
import org.entityflakes.Entity
import org.entityflakes.World
import org.fractalpixel.gameutils.rendering.RenderingContext2D
import org.fractalpixel.gameutils.space.Location


/**
 * Renders an entity on a 2D view
 */
interface EntityRenderer2D : Disposable {

    /**
     * Do any necessary initialization
     */
    fun init(world: World) {}

    override fun dispose() {}

    /**
     * Render the specified entity to the context.
     */
    fun render(context: RenderingContext2D,
               entity: Entity,
               entityLocation: Location
    )

}