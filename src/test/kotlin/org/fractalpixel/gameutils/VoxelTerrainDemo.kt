package org.fractalpixel.gameutils

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import org.entityflakes.World
import org.fractalpixel.gameutils.rendering.DefaultRenderingContext3D
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.fractalpixel.gameutils.voxel.distancefunction.NoiseFun
import org.fractalpixel.gameutils.voxel.distancefunction.SphereFun
import org.fractalpixel.gameutils.voxel.renderer.VoxelRendererLayer

class VoxelTerrainDemo: Game("Voxel Terrain Demo") {

    val distanceFunction: DistanceFun = SphereFun(
        radius = 10.0
    ).add(
        NoiseFun(
            scale = 1.0,
            amplitude = 1.0
        )
    )

    val terrain = VoxelTerrain(distanceFunction)

    override fun createProcessors(world: World) {
    }

    override fun setupWorld(world: World) {

        val renderingContext: RenderingContext3D = DefaultRenderingContext3D()
        renderingContext.init(world)
        val environment = Environment()
        renderingContext.environment = environment
        val light = DirectionalLight()
        light.set(Color(0.998f, 0.817f, 0.75f, 1f), Vector3(0.62f, -0.5f, -0.1f))
        environment.add(light)


        val voxelRendererLayer = VoxelRendererLayer(terrain)
        voxelRendererLayer.context = renderingContext


        world.createEntity(voxelRendererLayer)
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
