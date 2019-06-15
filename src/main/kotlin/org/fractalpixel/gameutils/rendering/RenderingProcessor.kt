package org.fractalpixel.gameutils.rendering

import org.entityflakes.processor.Processor

/**
 * Interface for processors that need to render something to the screen.
 */
interface RenderingProcessor: Processor {

    /**
     * Do any rendering to the screen.
     * Called each frame.
     */
    fun render()

}