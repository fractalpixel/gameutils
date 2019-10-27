package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.math.Vector2
import org.entityflakes.Entity
import org.entityflakes.entitymanager.ComponentRef
import org.fractalpixel.gameutils.appearance2d.Appearance2D
import org.fractalpixel.gameutils.layer.Layer2D
import org.fractalpixel.gameutils.space.EntitySpace
import org.fractalpixel.gameutils.space.Location
import org.kwrench.geometry.volume.MutableVolume

/**
 * A layer that renders the specified EntitySpace
 */
class RenderingLayer2D(var entitySpace: EntitySpace,
                       override var depth: Double = 0.0): Layer2D() {

    private val entityLocationsToShow = ArrayList<Location>()
    private val visibleVolume = MutableVolume()
    private val appearanceRef = ComponentRef(Appearance2D::class)

    override fun doInit(entity: Entity) {

    }

    override fun doDispose() {
        super.doDispose()
    }

    override fun render(context: RenderingContext2D) {
        // Get entities in visible volume
        context.camera2D.getVisibleWorldVolume(visibleVolume)
        entityLocationsToShow.clear()
        entitySpace.getEntities(visibleVolume, entityLocationsToShow)

        // Sort by depth
        entityLocationsToShow.sortBy { it.z }

        // Render entities
        val screenScale = context.camera2D.projectSizeToScreen()
        val screenPosTemp = Vector2()
        for (worldPos in entityLocationsToShow) {
            val entity = worldPos.entity
            if (entity != null) {
                val appearance = appearanceRef.getOrNull(entity)
                if (appearance != null) {
                    // Get screen position of entity
                    context.camera2D.projectToScreen(worldPos, screenPosTemp)

                    // Render
                    appearance.render(context, screenPosTemp, screenScale, worldPos)
                }
            }
        }
    }
}