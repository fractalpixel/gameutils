package org.fractalpixel.gameutils

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import org.entityflakes.World
import org.fractalpixel.gameutils.terrain.voxel.distancefunction.DistanceFun
import org.fractalpixel.gameutils.terrain.voxel.distancefunction.NoiseFun
import org.fractalpixel.gameutils.terrain.voxel.distancefunction.SphereFun

class VoxelTerrainDemo: Game("Voxel Terrain Demo") {

    val distanceFunction: DistanceFun = SphereFun(
        radius = 10.0
    ).add(
        NoiseFun(
            scale = 1.0,
            amplitude = 1.0
        )
    )

    override fun createProcessors(world: World) {


    }

    override fun setupWorld(world: World) {


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
