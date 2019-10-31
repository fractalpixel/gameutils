package org.fractalpixel.gameutils.appearance3d

import org.entityflakes.Disposable
import org.entityflakes.Entity
import org.entityflakes.World
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.space.Facing
import org.fractalpixel.gameutils.space.Location


/**
 * Renders an entity on a 3D view
 */
interface EntityRenderer3D : Disposable {

    /**
     * Do any necessary initialization
     */
    fun init(world: World) {}

    override fun dispose() {}

    /**
     * Render the specified entity to the context.
     * The location of the entity is already extracted so is provided.
     */
    fun render(context: RenderingContext3D,
               entity: Entity,
               location: Location)

}