package org.fractalpixel.gameutils.layer

import org.fractalpixel.gameutils.rendering.Camera2D
import org.fractalpixel.gameutils.rendering.DefaultRenderingContext2D
import org.fractalpixel.gameutils.rendering.RenderingContext2D

/**
 * Base class for 2D rendering layers
 */
abstract class Layer2D : LayerBase() {

    override var context: RenderingContext2D = DefaultRenderingContext2D()

    /**
     * The camera used by this layer.
     */
    val camera: Camera2D get() = context.camera2D

    override final fun doRender() {
        context.begin()
        render(context)
        context.end()
    }

    protected abstract fun render(context: RenderingContext2D)


}