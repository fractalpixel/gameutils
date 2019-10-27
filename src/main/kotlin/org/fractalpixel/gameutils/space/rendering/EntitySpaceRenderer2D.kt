package org.fractalpixel.gameutils.space.rendering

import org.entityflakes.World
import org.fractalpixel.gameutils.appearance2d.EntityRenderer2D
import org.fractalpixel.gameutils.layer.Layer2D
import org.fractalpixel.gameutils.rendering.RenderingContext2D
import org.fractalpixel.gameutils.space.EntitySpace
import org.fractalpixel.gameutils.space.Location
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.geometry.volume.Volume

/**
 * Renders a 2D view of an entity space.
 */
class EntitySpaceRenderer2D(val entityRenderer: EntityRenderer2D,
                            var entitySpace: EntitySpace? = null): Layer2D() {

    private val visibleWorld = MutableVolume()
    private val entityRenderList = ArrayList<Location>()

    override fun initLayer(world: World) {
        super.initLayer(world)
        entityRenderer.init(world)
    }

    override fun doDispose() {
        entityRenderer.dispose()
        super.doDispose()
    }

    override fun render(context: RenderingContext2D) {
        // Get visible world
        context.camera2D.getVisibleWorldVolume(visibleWorld)

        // Render all entities in the visible volume
        renderEntitiesInVolume(context, visibleWorld)
    }

    private fun renderEntitiesInVolume(context: RenderingContext2D,
                                       volume: Volume
    ) {
        val space = entitySpace
        if (space != null) {
            // Get all entities in volume
            space.getEntities(volume, entityRenderList)

            // Sort entities by depth
            // TODO: Cleaner depth function
            entityRenderList.sortBy { it.z * 10000 + it.y }

            // Render entities
            for (location in entityRenderList) {
                entityRenderer.render(context, location.entity!!, location)
            }
        }
    }
}