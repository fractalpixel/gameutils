package org.fractalpixel.gameutils.lights

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import org.entityflakes.ComponentBase
import org.entityflakes.Entity
import org.entityflakes.entitymanager.ComponentRef
import org.fractalpixel.gameutils.libgdxutils.toVector3
import org.fractalpixel.gameutils.space.Location
import kotlin.math.pow

/**
 * Indicates that this entity emits light of the specified [color] (or white) around it in a [lightRadius] meter
 * radius (default 10).  The light attenuates to zero at the edge of the radius.
 * The light [intensity] defaults to the square of the radius.
 * The entity must also have a Location for this light to be visible.
 */
class SphericalLight (
    var lightRadius: Double = 10.0,
    var color: Color = Color(Color.WHITE),
    var intensity: Double = lightRadius.pow(2.0)): ComponentBase() {

    override fun doInit(entity: Entity) {
    }

    override fun doDispose() {
    }

    fun createGdxLight(out: PointLight = PointLight()): PointLight {
        locationRef[entity!!].toVector3(out.position)
        out.intensity = intensity.toFloat()
        out.color.set(color)
        return out
    }

    companion object {
        val locationRef = ComponentRef(Location::class)
    }
}