package org.fractalpixel.gameutils.layer

import org.entityflakes.Entity
import org.entityflakes.World
import org.entityflakes.entitygroup.EntityGroup
import org.entityflakes.entitygroup.EntityGroupListener
import org.entityflakes.entitymanager.ComponentRef
import org.entityflakes.processor.ProcessorWithEntitiesBase
import org.fractalpixel.gameutils.rendering.RenderingProcessor
import org.mistutils.updating.strategies.VariableTimestepStrategy


/**
 * Keeps track of entities with Layers in the world, and renders the Layers in depth order.
 */
class LayerProcessor : ProcessorWithEntitiesBase(VariableTimestepStrategy(), Layer::class), RenderingProcessor {

    private val layers = ArrayList<Layer>()

    private val layerRef = ComponentRef(Layer::class)

    // NOTE: For some reason calling 'active' from layerListener results in an error..  Workaround by tracking initialization status
    private var initCalled: Boolean = false
    private val worldForInit: World get() = world

    private val layerListener: EntityGroupListener = object : EntityGroupListener {
        override fun onEntityAdded(entity: Entity) {
            val layer = layerRef[entity]
            layers.add(0, layer)

            // Initialize the layer if we are already initialized
            if (initCalled) layer.initLayer(worldForInit)
        }

        override fun onEntityRemoved(entity: Entity) {
            val layer = layerRef[entity]
            layers.remove(layer)
        }
    }

    override fun doInit(world: World, entities: EntityGroup) {
        // Init any layers that were previously added.
        for (layer in layers) {
            layer.initLayer(world)
        }

        // Listen to added and removed layers
        entities.addListener(layerListener)

        initCalled = true
    }

    override fun doDispose() {
        entities.removeListener(layerListener)
    }

    override fun render() {
        // Order by depth
        layers.sortBy { it.depth }

        // Render
        for (layer in layers) {
            if (layer.visible) {
                layer.render()
            }
        }

    }


}