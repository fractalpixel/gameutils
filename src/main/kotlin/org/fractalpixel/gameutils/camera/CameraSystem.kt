package org.fractalpixel.gameutils.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import org.entityflakes.Entity
import org.entityflakes.World
import org.entityflakes.system.SystemBase
import org.fractalpixel.gameutils.libgdxutils.set
import org.fractalpixel.gameutils.space.Facing
import org.fractalpixel.gameutils.space.Location
import org.kwrench.geometry.double3.Double3
import org.kwrench.time.Time

/**
 * Keeps track of the main camera in the 3D scene, and provides methods for manipulating it.
 */
class CameraSystem(val camera: Camera): SystemBase() {

    var nearClippingPlane: Float = 0.1f
        set(value) {
            field = value
            camera.near = value
        }

    var farClippingPlane: Float = 2000.0f
        set(value) {
            field = value
            camera.far = value
        }

    /**
     * An entity that the camera should follow.
     * Uses the Location and Facing components of the entity, or defaults to origo / positive Z axis if not available.
     */
    var targetEntity: Entity? = null
        set(target) {
            field = target
            updateCamera()
        }

    private fun updateCamera() {
        val target = targetEntity
        if (target != null) {
            val location = target[Location::class]
            if (location != null) camera.position.set(location)

            camera.direction.set(0f, 0f, 1f)
            camera.direction.mul(target[Facing::class]?.direction ?: NO_ROTATION)

            camera.up.set(0f, 1f, 0f)
            camera.up.mul(target[Facing::class]?.direction ?: NO_ROTATION)

            //camera.normalizeUp()
        }

        camera.update()
    }

    fun set(cameraPosition: Vector3,
            lookAt: Vector3 = Vector3.Zero,
            up: Vector3 = Vector3.Y) {
        camera.position.set(cameraPosition)
        camera.lookAt(lookAt)
        camera.up.set(up)
        camera.update()
    }

    override fun doInit(world: World) {
        camera.near = nearClippingPlane
        camera.far = farClippingPlane
    }

    override fun doUpdate(time: Time) {
        updateCamera()
    }

    companion object{
        private val NO_ROTATION = Quaternion().idt()

    }

}