package org.fractalpixel.gameutils.utils

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import org.kwrench.collections.bag.Bag
import org.kwrench.math.max
import org.kwrench.math.round

/**
 * Maintains pool of meshes, provides method to obtain a mesh with specified size of vertex and index arrays.
 * [vertexAttributes] are specified for the whole pool, so if different attributes are needed, use several pools.
 * By default the position and normal attributes are included.
 *
 * Some minimum vertex and index counts are used when creating meshes, so that small objects don't clog up the pool,
 * but they are somewhat use-case specific, so tune them with [minVertexCount] and [minIndexCount].
 */
class MeshPool(val vertexAttributes: VertexAttributes = VertexAttributes(VertexAttribute.Position(), VertexAttribute.Normal()),
               val maxSize: Int = 1000,
               val minVertexCount: Int = 16,
               val minIndexCount: Int = 16,
               val vertexMargin: Double = 1.0,
               val indexMargin: Double = 1.0) {

    private val pool = Bag<Mesh>()

    /**
     * Number of pooled meshes at the moment
     */
    val poolSize: Int get() = pool.size()

    // TODO: Could add methods to get total pooled vertexes and indexes too, using some variables to keep track, for memory debugging.  Maybe just bytes in pool?

    /**
     * Retrieve or create a Mesh with at least the space for the specified number of vertexes and indexes.
     */
    fun obtain(requiredVertexCount: Int, requiredIndexCount: Int): Mesh {
        val smallestMatch: Mesh? = getSmallestSatisfyingMesh(requiredVertexCount, requiredIndexCount)
        return if (smallestMatch != null) {
            // Found sufficient mesh
            smallestMatch
        } else {
            // All pooled meshes were too small, remove smallest one to avoid hoarding collection of minimal meshes
            disposeSmallest()

            // Create and return new mesh
            createMesh(requiredVertexCount, requiredIndexCount)
        }
    }

    /**
     * Release a mesh to this pool, allowing reusing of it later.
     * If the pool is full, the largest mesh is disposed to free up space.
     */
    fun release(mesh: Mesh) {
        pool.add(mesh)

        if (pool.size() > maxSize) {
            disposeLargest()
        }
    }

    /**
     * Disposes and removes all pooled meshes.
     */
    fun disposeAll() {
        for (mesh in pool) {
            mesh.dispose()
        }
        pool.clear()
    }

    private fun createMesh(requiredVertexCount: Int,requiredIndexCount: Int): Mesh {
        // Determine vertex and index count to use when creating the mesh
        val vertexCount = max((requiredVertexCount * vertexMargin).round(), minVertexCount, requiredVertexCount)
        val indexCount = max((requiredIndexCount * indexMargin).round(), minIndexCount, requiredIndexCount)

        // Create mesh, use common vertex attributes
        return Mesh(false, vertexCount, indexCount, vertexAttributes)
    }

    fun disposeSmallest() {
        getSmallestSatisfyingMesh(0, 0)?.dispose()
    }

    fun disposeLargest() {
        // Find it
        var largestMatch: Mesh? = null
        var maxVertexCount: Int = 0
        var maxIndexCount: Int = 0
        var largestIndex = -1

        for (i in 0 until pool.size()) {
            val pooledMesh = pool[i]
            if (pooledMesh.maxVertices > maxVertexCount ||
                pooledMesh.maxIndices > maxIndexCount
            ) {
                maxVertexCount = pooledMesh.maxVertices
                maxIndexCount = pooledMesh.maxIndices
                largestMatch = pooledMesh
                largestIndex = i
            }
        }

        if (largestIndex >= 0 && largestMatch != null) {
            // Remove it
            pool.remove(largestIndex)

            // Dispose it
            largestMatch.dispose()
        }
    }

    private fun getSmallestSatisfyingMesh(requiredVertexes: Int, requiredIndexes: Int): Mesh? {
        // Find it
        var smallestMatch: Mesh? = null
        var minVertexCount: Int = Int.MAX_VALUE
        var minIndexCount: Int = Int.MAX_VALUE
        var smallestIndex = -1

        for (i in 0 until pool.size()) {
            val pooledMesh = pool[i]
            if ((pooledMesh.maxVertices in requiredVertexes until minVertexCount) &&
                (pooledMesh.maxIndices in requiredIndexes until minIndexCount)
            ) {

                minVertexCount = pooledMesh.maxVertices
                minIndexCount = pooledMesh.maxIndices
                smallestMatch = pooledMesh
                smallestIndex = i
            }
        }

        // Remove it from the pool
        if (smallestIndex >= 0 && smallestMatch != null) {
            pool.remove(smallestIndex)
        }

        return smallestMatch
    }

}