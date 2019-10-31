package org.fractalpixel.gameutils.appearance3d

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import org.entityflakes.Entity
import org.fractalpixel.gameutils.rendering.RenderingContext3D

/**
 * Appearance using a model to render the entity.
 * Creates a local ModelInstance.
 */
class ModelAppearance(val model: Model): Appearance3D {

    private lateinit var instance: ModelInstance

    override fun init(entity: Entity) {
        instance = ModelInstance(model)
    }

    override fun render(pos: Vector3,
                        dir: Quaternion,
                        renderContext: RenderingContext3D
    ) {
        // Set location and direction
        instance.transform.set(pos, dir)

        // Render
        renderContext.modelBatch.render(instance, renderContext.environment)
    }

    override fun dispose() {
    }
}