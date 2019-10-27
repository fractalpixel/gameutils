package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.RenderableProvider

/**
 *
 */
interface RenderingContext3D: RenderingContext {

    var camera: Camera

    val modelBatch: ModelBatch

    var environment: Environment?

    /**
     * Render the specified renderable provider to the model batch of this context, using the environment of this context.
     */
    fun render(renderableProvider: RenderableProvider) {
        modelBatch.render(renderableProvider, environment)
    }
}