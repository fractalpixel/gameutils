package org.fractalpixel.gameutils.space.rendering

import org.entityflakes.World
import org.entityflakes.entitymanager.ComponentRef
import org.fractalpixel.gameutils.appearance3d.EntityRenderer3D
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.appearance3d.DefaultEntityRenderer3D
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.space.EntitySpace
import org.fractalpixel.gameutils.space.Facing
import org.fractalpixel.gameutils.space.Location
import org.fractalpixel.gameutils.space.Space
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.geometry.volume.Volume


/**
 * Renders a 3D view of an entity space that is in the same entity as this renderer.
 */
class EntitySpaceRenderer3D(val entityRenderer: EntityRenderer3D = DefaultEntityRenderer3D(),
                            var viewRadius: Float = 10000f): Layer3D() {

    private val visibleWorld = MutableVolume()
    private val entityRenderList = ArrayList<Location>()

    /**
     * True when no EntitySpace was found in the same entity that this renderer is assigned to when trying to render the space.
     */
    var noEntitySpaceFound = false
        private set

    override fun initLayer(world: World) {
        super.initLayer(world)
    }

    override fun doDispose() {
        super.doDispose()
    }

    override fun render(context: RenderingContext3D) {
        // Get entity space
        val space = entity?.get(Space::class) as? EntitySpace

        if (space != null) {
            noEntitySpaceFound = false

            // Get visible world
            val cam = context.camera
            val x1 = cam.position.x - viewRadius
            val y1 = cam.position.y - viewRadius
            val z1 = cam.position.z - viewRadius
            val x2 = cam.position.x + viewRadius
            val y2 = cam.position.y + viewRadius
            val z2 = cam.position.z + viewRadius
            visibleWorld.set(x1.toDouble(), y1.toDouble(), z1.toDouble(),
                             x2.toDouble(), y2.toDouble(), z2.toDouble())

            // Render all entities in the visible volume
            renderEntitiesInVolume(context, visibleWorld, space)
        }
        else {
            // Log once
            if (!noEntitySpaceFound) {
                noEntitySpaceFound = true
                // TODO: Find out some easy to use logging solution that doesn't throw an exception if it can't find a xml config file...
                println("No EntitySpace assigned to the same entity that this EntitySpaceRenderer is assigned to, nothing to render!")
            }
        }
    }

    private fun renderEntitiesInVolume(context: RenderingContext3D,
                                       volume: Volume,
                                       space: EntitySpace) {
        // Get all entities in volume
        space.getEntities(volume, entityRenderList)

        // Render entities
        for (location in entityRenderList) {
            val entity = location.entity!!
            entityRenderer.render(context, entity, location)
        }
    }
}