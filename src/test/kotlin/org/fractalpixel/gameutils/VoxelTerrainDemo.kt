package org.fractalpixel.gameutils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.entityflakes.World
import org.fractalpixel.gameutils.appearance3d.ModelAppearance
import org.fractalpixel.gameutils.camera.CameraSystem
import org.fractalpixel.gameutils.captionservice.CaptionSystem
import org.fractalpixel.gameutils.controls.InputControlSystem
import org.fractalpixel.gameutils.libgdxutils.GdxColorType
import org.fractalpixel.gameutils.lights.LightSystem
import org.fractalpixel.gameutils.lights.SphericalLight
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.space.BruteForceEntitySpace
import org.fractalpixel.gameutils.space.EntitySpace
import org.fractalpixel.gameutils.space.Location
import org.fractalpixel.gameutils.space.rendering.EntitySpaceRenderer3D
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.*
import org.fractalpixel.gameutils.voxel.renderer.VoxelRendererLayer
import org.kwrench.color.colorspace.HSLColorSpace
import org.kwrench.color.nextColor
import org.kwrench.geometry.double3.ImmutableDouble3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.math.Tau
import org.kwrench.math.abs
import org.kwrench.random.Rand
import org.kwrench.strings.toSymbol
import java.lang.Math.cos
import java.lang.Math.sin

// CLEANUP: Bit of a mess currently, clean up later.
class VoxelTerrainDemo: Game("Voxel Terrain Demo") {

    lateinit var cameraSystem: CameraSystem
    lateinit var lightSystem: LightSystem
    lateinit var space: BruteForceEntitySpace


    // TODO: Add coordinate transformation ops (scale & translate & maybe rotate - should probably be able to take function params.)

    val planetRadius = 40_000.0
    val mediumAmplitudeNoise = NoiseFun(1.0/1975.12, 30.0, seed=23189).abs().pow(ConstantFun(2.0))
    val planetFunction: DistanceFun =
        SphereFun(planetRadius, ImmutableDouble3(0.0, -planetRadius, 0.0)).add(
            NoiseFun(
                1.0/2131.32, 400.0, seed=3713
            )
        ).add(
            ModulatedNoiseFun(
                ConstantFun(1.0 / 173.63), mediumAmplitudeNoise, seed=83123
            )
        ).add(
            NoiseFun(
                1.0/ 6.7, 0.72, seed=4211
            )
        ).add(
            NoiseFun(
                1.0/ 1.5, 0.22, seed=7413
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


    lateinit var terrain: VoxelTerrain

    override fun createProcessors(world: World) {

        /*
        // DEBUG: Show signed distance function preview
        println((planetFunction as CompilingDistanceFun).previewCode())
        */

        world.addSystem(CaptionSystem())
        cameraSystem = world.addSystem(CameraSystem(PerspectiveCamera()))
        lightSystem = world.addSystem(LightSystem())
        world.addSystem(InputControlSystem())

        terrain = VoxelTerrain(planetFunction, lightSystem)

        val cameraPosition = Vector3(0f, 5f, 30f)
        val lookAt = Vector3(0f, 0f, 0f)
        cameraSystem.set(cameraPosition, lookAt)

        cameraSystem.nearClippingPlane = 0.001f
        cameraSystem.farClippingPlane = 100_000f

        // TODO: Mouse & keyboard controlled camera
        // Rotate camera
        val radius = 10000f
        val speed = 0.035f
        world.addSystem { _, time ->
            //val speed = 1f * ((sin(time.secondsSinceStart*Tau / 30).toFloat() + 0.4f))
            cameraPosition.x = speed * (radius * sin(time.secondsSinceStart*0.037) + radius * -cos(-0.082* time.secondsSinceStart)).toFloat()
            cameraPosition.z = speed * (radius * cos(time.secondsSinceStart*0.043) + radius * sin(-0.113 * time.secondsSinceStart)).toFloat()
            cameraPosition.y = speed * cos(time.secondsSinceStart*0.037).toFloat() * 300f + 100f

            lookAt.x = 2000f * -cos(0.0011 * time.secondsSinceStart * Tau).toFloat()
            lookAt.y = 500f * sin(0.0019 * time.secondsSinceStart * Tau).toFloat() -500f
            lookAt.z = 1200f * -sin(0.013 * time.secondsSinceStart * Tau).toFloat()

            cameraSystem.set(cameraPosition, lookAt)
        }
    }

    override fun setupWorld(world: World) {
        // REFACTOR: Make it easier to use the same camera and/or shaders in several layers - set defaults in LayerManager?  Or group layers?
        // REFACTOR: Pass near and far clipping plane to shader.  Use near clipping plane as C (?).

        // Voxel terrain
        val voxelRendererLayer = VoxelRendererLayer(terrain)
        voxelRendererLayer.context.camera = cameraSystem.camera
        voxelRendererLayer.depth = 1.0
        voxelRendererLayer.clearColorBufferToColor = Color.DARK_GRAY
        initRenderingContext(voxelRendererLayer.context, world)
        world.createEntity(voxelRendererLayer)

        // Entity space & rendering of the entities in it
        space = BruteForceEntitySpace()
//        val entitySpaceRenderer3D = EntitySpaceRenderer3D()
        val entitySpaceRenderer3D = EntitySpaceRenderer3D(shaderProvider = voxelRendererLayer.context.shaderProvider)
        entitySpaceRenderer3D.context.camera = cameraSystem.camera
        entitySpaceRenderer3D.clearDepthBuffer = false
        entitySpaceRenderer3D.depth = 2.0
        val spaceEntity = world.createEntity(space, entitySpaceRenderer3D)
        world.tagEntity(spaceEntity, "space".toSymbol())

        addLights(world)
    }

    private fun addLights(world: World) {
        val count = 1000
        val random = Rand.createDefault()
        for (i in 0 until count) {
            // Light
            val color = random.nextColor(
                GdxColorType,
                ImmutableDouble3(0.0, 0.0, 0.1),
                ImmutableDouble3(1.0, 0.9, 0.9),
                colorSpace = HSLColorSpace
            )
            val lightRadius = random.nextGaussian(200.0, 100.0).abs() + 50.0
            val pointLight = SphericalLight(lightRadius,color)

            // Location
            val range = 1000.0
            val location = Location(random.nextDouble(-range, range), random.nextDouble(-range, range), random.nextDouble(-range, range), lightRadius*2.0, space)

            // Appearance
            val size = (lightRadius / (lightRadius + 10.0) * 10.0).toFloat()
            val material = Material(ColorAttribute.createDiffuse(color))
            val attributes = VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal
            val model = ModelBuilder().createSphere(size, size, size, 12, 12, material, attributes.toLong())
            val appearance = ModelAppearance(model)

            // Entity
            world.createEntity(location, pointLight, appearance)
        }
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
    config.depth = 16
    config.stencil = 8
    LwjglApplication(VoxelTerrainDemo(), config)
}
