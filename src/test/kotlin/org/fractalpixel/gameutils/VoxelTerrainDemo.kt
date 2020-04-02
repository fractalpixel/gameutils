package org.fractalpixel.gameutils

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
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.fractalpixel.gameutils.voxel.distancefunction.NoiseFun
import org.fractalpixel.gameutils.voxel.distancefunction.SphereFun
import org.fractalpixel.gameutils.voxel.renderer.VoxelRendererLayer
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.math.Tau
import java.lang.Math.cos
import java.lang.Math.sin

// TODO: Bit of a mess currently, clean up later.
class VoxelTerrainDemo: Game("Voxel Terrain Demo") {

    lateinit var cameraSystem: CameraSystem

    val distanceFunction: DistanceFun = (SphereFun(
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


    val terrain = VoxelTerrain(distanceFunction)

    override fun createProcessors(world: World) {
        world.addSystem(CaptionSystem())
        cameraSystem = world.addSystem(CameraSystem(PerspectiveCamera()))
        world.addSystem(InputControlSystem())

        val cameraPosition = Vector3(0f, 5f, 30f)
        val lookAt = Vector3(0f, 0f, 0f)
        cameraSystem.set(cameraPosition, lookAt)

        // Rotate camera
        val radius = 25f
        val speed = 0.05f
        world.addSystem { _, time ->
            cameraPosition.x = (radius * -cos(speed * time.secondsSinceStart * Tau)).toFloat()
            cameraPosition.z = (radius * sin(speed * time.secondsSinceStart * Tau)).toFloat()
            cameraSystem.set(cameraPosition, lookAt)
        }
    }

    override fun setupWorld(world: World) {
        val voxelRendererLayer = VoxelRendererLayer(terrain)
        voxelRendererLayer.context = createRenderingContext(world)
        world.createEntity(voxelRendererLayer)
    }

    private fun createRenderingContext(world: World): RenderingContext3D {
        val renderingContext: RenderingContext3D = DefaultRenderingContext3D()
        renderingContext.init(world)
        val environment = Environment()
        renderingContext.environment = environment
        val light = DirectionalLight()
        light.set(Color(0.9f, 0.98f, 0.75f, 1f), Vector3(0.62f, -1.5f, 0.1f))
        val light2 = DirectionalLight()
        light2.set(Color(0.5f, 0.1f, 0.25f, 1f), Vector3(-0.3f, 0.03f, -0.1f))
        environment.add(light)
        environment.add(light2)
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.1f, 0.1f, 0.2f, 1f))
        val pointLight = PointLight()
        pointLight.intensity

        renderingContext.camera = cameraSystem.camera
        return renderingContext
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
    LwjglApplication(VoxelTerrainDemo(), config)
}
