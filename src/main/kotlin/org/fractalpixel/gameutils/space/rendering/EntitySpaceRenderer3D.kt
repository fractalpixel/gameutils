package org.fractalpixel.gameutils.space.rendering

import org.entityflakes.World
import org.fractalpixel.gameutils.appearance3d.EntityRenderer3D
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.appearance3d.DefaultEntityRenderer3D
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.space.EntitySpace
import org.fractalpixel.gameutils.space.Location
import org.fractalpixel.gameutils.space.Space
import org.mistutils.geometry.volume.MutableVolume
import org.mistutils.geometry.volume.Volume


/**
 * Renders a 3D view of an entity space that is in the same entity as this renderer.
 */
class EntitySpaceRenderer3D(val entityRenderer: EntityRenderer3D = DefaultEntityRenderer3D(),
                            var viewRadius: Float = 1000f): Layer3D() {

    private val visibleWorld = MutableVolume()
    private val entityRenderList = ArrayList<Location>()

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
    }

    private fun renderEntitiesInVolume(context: RenderingContext3D,
                                       volume: Volume,
                                       space: EntitySpace) {
        // Get all entities in volume
        space.getEntities(volume, entityRenderList)

        // Render entities
        for (location in entityRenderList) {
            entityRenderer.render(context, location.entity!!, location)
        }
    }
}