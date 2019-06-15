package org.fractalpixel.gameutils.layer

import org.entityflakes.Component
import org.entityflakes.PolymorphicComponent
import org.entityflakes.World
import org.fractalpixel.gameutils.rendering.RenderingContext


/**
 * Renders something on the screen.
 * E.g. an UI, a 2D map, a background, whole-screen effects, a 3D scene.
 * Several Layers can be rendered at the same time, in a specific order.
 */
interface Layer: PolymorphicComponent {

    /**
     * Depth order of this layer, layers are rendered in highest depth first -order.
     */
    val depth: Double

    /**
     * True if the layer should be rendered, false if not.
     */
    var visible: Boolean

    /**
     * Rendering context used by this layer
     */
    val context: RenderingContext

    /**
     * Render this layer.
     */
    fun render()

    /**
     * Called when the layer is added to a LayerProcessor, or the LayerProcessor is initialized.
     */
    fun initLayer(world: World)


    override val componentCategory: Class<out Component> get() = Layer::class.java

}