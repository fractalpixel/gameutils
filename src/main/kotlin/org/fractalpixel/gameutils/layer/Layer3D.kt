package org.fractalpixel.gameutils.layer

import com.badlogic.gdx.graphics.Camera
import org.fractalpixel.gameutils.rendering.DefaultRenderingContext3D
import org.fractalpixel.gameutils.rendering.RenderingContext3D

/**
 * Base class for 3D rendering layers
 */
abstract class Layer3D : LayerBase() {

    override var context: RenderingContext3D = DefaultRenderingContext3D()

    /**
     * The camera used by this layer.
     */
    val camera: Camera get() = context.camera

    override final fun doRender() {
        context.begin()
        render(context)
        context.end()
    }

    protected abstract fun render(context: RenderingContext3D)


}