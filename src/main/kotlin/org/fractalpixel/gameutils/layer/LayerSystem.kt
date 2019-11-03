package org.fractalpixel.gameutils.layer

import org.entityflakes.Entity
import org.entityflakes.entitymanager.ComponentRef
import org.entityflakes.system.EntitySystemBase
import org.fractalpixel.gameutils.rendering.RenderingSystem
import org.kwrench.time.Time
import org.kwrench.updating.strategies.VariableTimestepStrategy


/**
 * Keeps track of entities with Layers in the world, and renders the Layers in depth order.
 */
class LayerSystem : EntitySystemBase(VariableTimestepStrategy(), true, Layer::class), RenderingSystem {

    private val layers = ArrayList<Layer>()

    private val layerRef = ComponentRef(Layer::class)

    override fun onEntityAdded(entity: Entity) {
        val layer = layerRef[entity]
        layers.add(0, layer)

        // Initialize the layer
        layer.initLayer(world)
    }

    override fun onEntityRemoved(entity: Entity) {
        val layer = layerRef[entity]
        layers.remove(layer)
    }

    override fun updateEntity(entity: Entity, time: Time) {
        // Nothing to update
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