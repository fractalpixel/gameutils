package org.fractalpixel.gameutils.rendering

import org.entityflakes.Disposable
import org.entityflakes.World


/**
 *
 */
interface RenderingContext: Disposable {

    /**
     * Called when the rendering context is initialized.
     */
    fun init(world: World)

    /**
     * Begin rendering in a frame with this context.
     */
    fun begin()

    /**
     * Stop rendering in a frame with this context.
     */
    fun end()


}