package org.fractalpixel.gameutils.lights

import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.libgdxutils.toFloatArray
import org.fractalpixel.gameutils.utils.CubeMapPos
import org.fractalpixel.gameutils.utils.normalize
import org.fractalpixel.gameutils.utils.projectToCube
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import java.lang.IllegalArgumentException

/**
 * Set of small light-sampling spheres located somewhere in space, possibly associated with a surface, and located in
 * a scene with lights and possibility to raymarch towards lights or other directions.
 *
 * Each light sampler consist of one spheremap collecting the incoming direct light, and any reflection / refraction
 * of that light in other directions, and one spheremap containing incoming indirect light, and the reflection of it.
 *
 * The direct light is sampled by raymarching towards all known pointlights and directional lights in range whenever
 * the lights move or the sampler positions are updated.  The indirect light is sampled when requested (typically
 * every X frames) and sends a ray marching in a random direction for the pixel last updated on a spheremap, updating
 * the pixels on the spheremap using a linear interpolation function (using the specified amount of the new value).
 *
 * For reasons of cache efficiency, the sphere maps are stored in float arrays instead of as separate objects,
 * as they are frequently updated in a batch.
 */
// TODO: Calculate irradiance for a direction from the light spheres
class LightProbes(val capacity: Int = 16) {

    val data = FloatArray(capacity * probeSize)
    private var nextFreeIndex = 0

    /**
     * Adds a probe at the specified world coordinate, and returns the index of the probe.
     */
    fun addProbe(worldPos: Vector3, normal: Vector3): Int {
        if (nextFreeIndex >= capacity) throw IllegalArgumentException("No more light probes can be added to this LightProbes container, it is already at max capacity ($capacity)")
        val id = nextFreeIndex++

        // Store pos
        worldPos.toFloatArray(data, id * probeSize)

        // TODO: Store normal?

        // TODO: Need other surface properties like average color, brdf function etc?

        return id
    }

    fun calculateDirectLights(lightProvider: LightProvider) {
        val pos = MutableDouble3()
        val direction = MutableDouble3()
        val tempCubePos = CubeMapPos()
        for (i in 0 until nextFreeIndex) {
            var index = i * probeSize
            pos.x = data[index++].toDouble()
            pos.y = data[index++].toDouble()
            pos.z = data[index++].toDouble()
            lightProvider.forEachPointLightInRange(pos) { entity, location, light, squaredDistance ->
                // TODO: Raymarch from light location to probe position, applying atmosphere and checking for occlusion on the way.

                direction.set(location).sub(pos)

                val intensity = (light.intensity / squaredDistance).toFloat()
                val r = intensity * light.color.r
                val g = intensity * light.color.g
                val b = intensity * light.color.b

                addLight(index, DIRECT_LIGHT_MAP, direction, r, g, b, tempCubePos)

                // TODO: Add reflection based on surface parameters
            }
        }
    }

    private fun addLight(index: Int, sphereMap: Int, direction: Double3, r: Float, g: Float, b: Float, tempCubePos: CubeMapPos) {
        // Project to sphere map
        direction.projectToCube(tempCubePos)

        // Get index in data
        val pixelIndex = index + sphereMap * spheremapSize + tempCubePos.toCubeTextureIndex(spheremapResolution) * colorChannelCount

        // Store color
        data[pixelIndex+0] = r
        data[pixelIndex+1] = g
        data[pixelIndex+2] = b
    }



    companion object {
        val spheremapResolution = 3
        val posSize = 3
        val colorChannelCount = 3
        val spheremapSize = spheremapResolution * spheremapResolution * 6 * colorChannelCount
        val probeSize = posSize + spheremapSize * 2

        private val DIRECT_LIGHT_MAP = 0
        private val INDIRECT_LIGHT_MAP = 1
    }
}