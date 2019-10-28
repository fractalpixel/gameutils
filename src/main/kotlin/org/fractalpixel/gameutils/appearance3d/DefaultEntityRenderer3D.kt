package org.fractalpixel.gameutils.appearance3d

import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import org.entityflakes.Entity
import org.entityflakes.entitymanager.ComponentRef
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.space.Location

/**
 *
 */
class DefaultEntityRenderer3D: EntityRenderer3D {

    private val appearance3DRef = ComponentRef(Appearance3D::class)
    private val tempPos = Vector3()
    private val tempQuat = Quaternion()

    override fun render(context: RenderingContext3D, entity: Entity, entityLocation: Location) {
        val appearance3D = appearance3DRef.getOrNull(entity)
        if (appearance3D != null) {
            tempPos.set(entityLocation.x.toFloat(),
                        entityLocation.y.toFloat(),
                        entityLocation.z.toFloat())
            appearance3D.render(tempPos, tempQuat, context)
        }
    }
}