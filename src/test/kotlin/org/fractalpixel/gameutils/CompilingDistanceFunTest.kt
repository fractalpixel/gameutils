package org.fractalpixel.gameutils

import org.fractalpixel.gameutils.voxel.distancefunction.CompilingDistanceFun
import org.fractalpixel.gameutils.voxel.distancefunction.utils.CompilationContext
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.junit.Test
import org.kwrench.geometry.volume.Volume
import kotlin.test.assertEquals

class CompilingDistanceFunTest {

    @Test
    fun testCompile() {
        val cdf = TestDF()
        val value = cdf.get(0.0, 2.0, 0.0, 1.0)
        assertEquals(42.0, value, "Compilation should work")
    }

}

class TestDF(): CompilingDistanceFun() {
    override val name: String get() = "TestDF"

    override fun constructCode(codeOut: StringBuilder, context: CompilationContext) {
        codeOut.appendln("double #out = 40.0 + #y;")
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        TODO("not implemented")
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        TODO("not implemented")
    }
}