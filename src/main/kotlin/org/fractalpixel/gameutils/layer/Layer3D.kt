package org.fractalpixel.gameutils.layer

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import org.fractalpixel.gameutils.rendering.DefaultRenderingContext3D
import org.fractalpixel.gameutils.rendering.RenderingContext3D

/**
 * Base class for 3D rendering layers
 */
abstract class Layer3D(shaderProvider: ShaderProvider? = null) : LayerBase() {

    override var context: RenderingContext3D = DefaultRenderingContext3D(shaderProvider)

    /**
     * The camera used by this layer.
     */
    val camera: Camera get() = context.camera

    override final fun doRender() {
        renderBegin(context)
        render(context)
        renderEnd(context)
    }

    /**
     * Called when rendering starts for this layer.
     * Calls context.begin(), if you override this, either call it yourself or call the super method.
     */
    protected open fun renderBegin(context: RenderingContext3D) {
        context.begin()
    }

    /**
     * Called when rendering ends for this layer.
     * Calls context.end(), if you override this, either call it yourself or call the super method.
     */
    protected open fun renderEnd(context: RenderingContext3D) {
        context.end()
    }


    protected abstract fun render(context: RenderingContext3D)


}