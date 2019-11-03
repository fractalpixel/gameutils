package org.fractalpixel.gameutils.rendering

import org.entityflakes.system.System

/**
 * Interface for processors that need to render something to the screen.
 */
interface RenderingSystem: System {

    /**
     * Do any rendering to the screen.
     * Called each frame.
     */
    fun render()

}