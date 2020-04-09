package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.translate
import org.fractalpixel.gameutils.voxel.distancefunction.utils.CompilationContext
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.abs
import org.kwrench.math.max
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand

/**
 * A distance function that perturbs the input position with noise to another distance function.
 */
class NoisePerturbFun(var distanceFun: DistanceFun,
                      var noiseScale: Double3 = Double3.ONES,
                      var noiseAmplitude: Double3 = Double3.ONES,
                      var noiseOffset: Double3 = Double3.ZEROES,
                      val seed: Long = Rand.default.nextLong()): CompilingDistanceFun() {

    override val name: String get() = "NoisePerturb"

    val noiseX = OpenSimplexNoise(seed + 13)
    val noiseY = OpenSimplexNoise(seed + 41)
    val noiseZ = OpenSimplexNoise(seed + 97)


    /*
    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        val xp = x + noiseX.noise(x * noiseScale.x, y * noiseScale.y, z * noiseScale.z) * noiseAmplitude.x + noiseOffset.x
        val yp = y + noiseY.noise(x * noiseScale.x, y * noiseScale.y, z * noiseScale.z) * noiseAmplitude.y + noiseOffset.y
        val zp = z + noiseZ.noise(x * noiseScale.x, y * noiseScale.y, z * noiseScale.z) * noiseAmplitude.z + noiseOffset.z
        return distanceFun.get(xp, yp, zp, calculatePerturbedSampleSize(sampleSize))
    }
    */

    override fun constructCode(codeOut: StringBuilder, context: CompilationContext) {
        context.parameter(codeOut, "noiseX", noiseX)
        context.parameter(codeOut, "noiseY", noiseY)
        context.parameter(codeOut, "noiseZ", noiseZ)

        // Store current coordiantes
        codeOut.appendln("double #tx = x;")
        codeOut.appendln("double #ty = y;")
        codeOut.appendln("double #tz = z;")
        codeOut.appendln("double #ts = sampleSize;")

        codeOut.appendln("x = x + #noiseX.noise(x * ${noiseScale.x}, y * ${noiseScale.y}, z * ${noiseScale.z}) * ${noiseAmplitude.x} + ${noiseOffset.x};")
        codeOut.appendln("y = y + #noiseY.noise(x * ${noiseScale.x}, y * ${noiseScale.y}, z * ${noiseScale.z}) * ${noiseAmplitude.y} + ${noiseOffset.y};")
        codeOut.appendln("z = z + #noiseX.noise(x * ${noiseScale.x}, y * ${noiseScale.y}, z * ${noiseScale.z}) * ${noiseAmplitude.z} + ${noiseOffset.z};")

        val perturbedScale = max(noiseScale.x.abs(), noiseScale.y.abs(), noiseScale.z.abs())
        if (perturbedScale > 0.0) {
            codeOut.appendln("sampleSize = sampleSize / $perturbedScale;")
        }

        context.createCall(codeOut, distanceFun, "out")

        // Restore coordinates
        codeOut.appendln("x = #tx;")
        codeOut.appendln("y = #ty;")
        codeOut.appendln("z = #tz;")
        codeOut.appendln("sampleSize = #ts;")
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        // The perturbation breaks this optimization, so we need to sample individual points
        calculateBlockUsingSamples(volume, block, blockPool)
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        // Create a new volume and scale it to cover the perturbed extent
        // NOTE: Assumes the noise function returns a value in the -1..1 range.
        val perturbedVolume = MutableVolume(volume)
        perturbedVolume.expand(noiseAmplitude)
        perturbedVolume.translate(noiseOffset)

        // Get bounds inside the expanded perturbed volume
        distanceFun.calculateBounds(perturbedVolume, calculatePerturbedSampleSize(sampleSize), bounds)
    }

    private fun calculatePerturbedSampleSize(sampleSize: Double): Double {
        val perturbedScale = max(noiseScale.x.abs(), noiseScale.y.abs(), noiseScale.z.abs())
        return if (perturbedScale <= 0.0) sampleSize else sampleSize / perturbedScale
    }

}