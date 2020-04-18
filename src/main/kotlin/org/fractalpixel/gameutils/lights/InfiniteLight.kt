package org.fractalpixel.gameutils.lights

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import org.entityflakes.ComponentBase
import org.entityflakes.Entity
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.ImmutableDouble3
import kotlin.math.pow

/**
 * A directed light is associated with this entity, considered to be at infinite distance and uniform intensity.
 * The entity emits light of the specified [color] (or white) in the specified [direction], with the specified [intensity].
 * The light is assumed to come from outside the scene (sun / moonlight etc)
 */
class InfiniteLight (
    var direction: Vector3 = Vector3(0f, -1f, 0f),
    var color: Color = Color(Color.WHITE),
    var intensity: Double = 10.0): ComponentBase() {

    override fun doInit(entity: Entity) {
    }

    override fun doDispose() {
    }

    fun createGdxLight(out: DirectionalLight = DirectionalLight()): DirectionalLight {
        // CHECK: How is intensity conveyed to GDX?  If it isn't stored, we'll have to bypass their structures
        out.direction.set(direction)
        out.color.set(color)
        return out
    }
}