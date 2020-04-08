package org.fractalpixel.gameutils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.math.Vector3
import org.entityflakes.World
import org.fractalpixel.gameutils.camera.CameraSystem
import org.fractalpixel.gameutils.captionservice.CaptionSystem
import org.fractalpixel.gameutils.controls.InputControlSystem
import org.fractalpixel.gameutils.rendering.DefaultRenderingContext3D
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.*
import org.fractalpixel.gameutils.voxel.renderer.VoxelRendererLayer
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.ImmutableDouble3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.math.Tau
import java.lang.Math.cos
import java.lang.Math.sin

// TODO: Bit of a mess currently, clean up later.
class VoxelTerrainDemo: Game("Voxel Terrain Demo") {

    lateinit var cameraSystem: CameraSystem


    // TODO: Add coordinate transformation ops (scale & translate & maybe rotate - should probably be able to take function params.)

    val planetRadius = 40_000.0
    val mediumAmplitudeNoise = NoiseFun(1.0/1975.12, 20.0, seed=23189).abs().pow(ConstantFun(2.0))
    val planetFunction: DistanceFun =
        SphereFun(planetRadius, ImmutableDouble3(0.0, -planetRadius, 0.0)).add(
            NoiseFun(
                1.0/2131.32, 400.0, seed=3713
            )
        ).add(
            ModulatedNoiseFun(
                ConstantFun(1.0 / 113.63), mediumAmplitudeNoise, seed=83123
            )
        ).add(
            NoiseFun(
                1.0/21.2, 5.3
            ).smoothIntersection(
                ModulatedNoiseFun(
                    ConstantFun(1.0/63.31),
                    NoiseFun(1.0/1117.7, 15.0, 8.0),
                    seed=8411
                ),
                3.0
            )
        )

    val asteroidFunction: DistanceFun = (SphereFun(
        radius = 14.0
    ).add(
        NoiseFun(
            scale = 0.1,
            amplitude = 3.2
        )
    ).smoothDifference(
        NoiseFun(
            scale = 0.3,
            amplitude = 1.0
        ),
        1.1
    )).smoothIntersection(
        NoiseFun(
            scale = 0.15,
            amplitude = 2.6,
            offset = -0.5
        ),
        3.0
    ).perturb(MutableDouble3(0.2, 0.3, 0.2), MutableDouble3(2.0, 1.0, 2.0))


    val terrain = VoxelTerrain(planetFunction)

    override fun createProcessors(world: World) {
        world.addSystem(CaptionSystem())
        cameraSystem = world.addSystem(CameraSystem(PerspectiveCamera()))
        world.addSystem(InputControlSystem())

        val cameraPosition = Vector3(0f, 5f, 30f)
        val lookAt = Vector3(0f, 0f, 0f)
        cameraSystem.set(cameraPosition, lookAt)

        cameraSystem.nearClippingPlane = 0.01f
        cameraSystem.farClippingPlane = 100_000f

        // TODO: Mouse & keyboard controlled camera
        // Rotate camera
        val radius = 5000f
        var pos = 0f
        val speed = 100f
        world.addSystem { _, time ->
            //val speed = 1f * ((sin(time.secondsSinceStart*Tau / 30).toFloat() + 0.4f))
            pos += speed * time.currentStepElapsedSeconds.toFloat()
            cameraPosition.x = (radius * sin(time.secondsSinceStart*0.1) + -cos(-pos * Tau)).toFloat()
            cameraPosition.z = (radius * cos(time.secondsSinceStart*0.01) + sin(-pos * Tau)).toFloat()
            cameraPosition.y = cos(time.secondsSinceStart*0.03).toFloat() * 100f +50f

            lookAt.x = 2000f * -cos(0.001 * time.secondsSinceStart * Tau).toFloat()
            lookAt.y = 500f * sin(0.001 * time.secondsSinceStart * Tau).toFloat() -500f
            lookAt.z = 200f * -sin(0.01 * time.secondsSinceStart * Tau).toFloat()

            cameraSystem.set(cameraPosition, lookAt)
        }
    }

    override fun setupWorld(world: World) {
        val voxelRendererLayer = VoxelRendererLayer(terrain)
        initRenderingContext(voxelRendererLayer.context, world)
        world.createEntity(voxelRendererLayer)
    }

    private fun initRenderingContext(renderingContext: RenderingContext3D, world: World) {
        renderingContext.init(world)
        val environment = Environment()
        renderingContext.environment = environment
        val light = DirectionalLight()
        light.set(Color(0.9f, 0.98f, 0.75f, 1f), Vector3(0.62f, -0.2f, 0.1f))
        val light2 = DirectionalLight()
        light2.set(Color(0.5f, 0.1f, 0.25f, 1f), Vector3(-0.4f, 0.03f, -0.1f))
        environment.add(light)
        environment.add(light2)
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.1f, 0.1f, 0.2f, 1f))
        val pointLight = PointLight()
        pointLight.intensity

        // Bump priority of OpenGL thread to the max, to avoid UI freezes while calculation is ongoing in other threads.
        Thread.currentThread().priority = Thread.MAX_PRIORITY

        // TODO: Remove later?
        // println("Buffer format: " + Gdx.graphics.bufferFormat)

        renderingContext.camera = cameraSystem.camera
    }
}


/**
 * Main entrypoint.
 */
fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.title = "Voxel Terrain Demo"
    config.width = 1200
    config.height = 800
    config.depth = 24
    LwjglApplication(VoxelTerrainDemo(), config)
}
