package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.model.NodePart
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.ShortArray


/**
 * Utility class that is used to construct meshes by adding vertexes and faces one by one.
 * Not thread safe (wrap in ThreadLocal or similar at the use site if thread safety is needed).
 */
// TODO: Add support for other vertex attributes.
class ShapeBuilder {

    /**
     * Contains the transformation to apply to newly added positions.
     * Transformations can be pushed and popped from this stack as this mesh builder is used to build different subparts of a mesh.
     */
    val transformStack = MatrixStack()

    // Number of floats per vertex in the vertex data array
    private val vertexEntrySize = 3 * 2
    private val posOffset = 0
    private val normalOffset = 3

    // Vertex data for the created mesh
    private val vertexData = FloatArray()

    // Triangle index data for the created mesh
    private val indexes = ShortArray()

    private var nextVertexId: Short = 0

    // Just keeps track of number of triangles created, for debugging purposes, or could be used to restrict model size as an emergency measure.
    private var triangleCount_ = 0

    private val tempPos = Vector3()
    private val v1 = Vector3()
    private val v2 = Vector3()
    private val v3 = Vector3()
    private val n = Vector3()

    val modelBuilder = ModelBuilder()

    /**
     * Number of triangles currently in the mesh being built
     */
    val triangleCount: Int get() = triangleCount_

    /**
     * Number of vertexes currently in the mesh being built
     */
    val vertexCount: Int get() = nextVertexId.toInt()

    /**
     * Adds data for the specified vertex to the vertex data array.
     * @param pos The position of the vertex. The transformation stack will be applied to it.
     * @return the index of the added vertex.
     */
    fun addVertex(pos: Vector3, normal: Vector3 = Vector3.Zero): Short {
        // Position
        tempPos.set(pos)
        transformStack.apply(tempPos)
        vertexData.add(tempPos.x)
        vertexData.add(tempPos.y)
        vertexData.add(tempPos.z)

        // Normal
        vertexData.add(normal.x)
        vertexData.add(normal.y)
        vertexData.add(normal.z)

        return nextVertexId++
    }

    fun getPos(index: Short, posOut: Vector3): Vector3 = getVertexVector3(index, posOffset, posOut)
    fun setPos(index: Short, pos: Vector3, transform: Boolean = false) {
        tempPos.set(pos)
        if (transform) transformStack.apply(tempPos)
        setVertexVector3(index, posOffset, tempPos)
    }
    fun getNormal(index: Short, normalOut: Vector3) = getVertexVector3(index, normalOffset, normalOut)
    fun setNormal(index: Short, normal: Vector3) = setVertexVector3(index, normalOffset, normal)

    fun addToNormal(index: Short, normalDelta: Vector3) {
        vertexData[index * vertexEntrySize + normalOffset+0] += normalDelta.x
        vertexData[index * vertexEntrySize + normalOffset+1] += normalDelta.y
        vertexData[index * vertexEntrySize + normalOffset+2] += normalDelta.z
    }

    fun getVertexValue(index: Short, offset: Int): Float = vertexData[index * vertexEntrySize + offset]
    fun getVertexVector3(index: Short, offset: Int, vectorOut: Vector3): Vector3 {
        vectorOut.set(
                vertexData[index * vertexEntrySize + offset + 0],
                vertexData[index * vertexEntrySize + offset + 1],
                vertexData[index * vertexEntrySize + offset + 2]
        )
        return vectorOut
    }

    fun setVertexValue(index: Short, offset: Int, value: Float) {
        vertexData[index * vertexEntrySize + offset] = value
    }
    fun setVertexVector3(index: Short, offset: Int, vector: Vector3) {
        vertexData[index * vertexEntrySize + offset] = vector.x
        vertexData[index * vertexEntrySize + offset+1] = vector.y
        vertexData[index * vertexEntrySize + offset+2] = vector.z
    }

    /**
     * Adds a quad between the specified vertex indexes
     */
    fun addQuad(a: Short, b: Short, c: Short, d: Short,
                twoSided: Boolean = false,
                invertFace: Boolean = false,
                updateNormals: Boolean = true) {

        addTriangle(a, d, c, twoSided, invertFace, updateNormals)
        addTriangle(b, a, c, twoSided, invertFace, updateNormals)
    }

    /**
     * Adds a triangle between the specified vertex indexes
     */
    fun addTriangle(a: Short, b: Short, c: Short,
                    twoSided: Boolean = false,
                    invertFace: Boolean = false,
                    updateNormals: Boolean = true) {

        if (a != b && a != c && b != c) {

            if (!invertFace || twoSided) {
                indexes.add(a)
                indexes.add(b)
                indexes.add(c)
            }

            if (invertFace || twoSided) {
                indexes.add(a)
                indexes.add(c)
                indexes.add(b)
            }

            if (updateNormals) {
                // Calculate an area weighted normal for this face
                getPos(a, v1)
                getPos(b, v2)
                getPos(c, v3)
                v1.sub(v2)
                v3.sub(v2)

                // Take cross product of the edges of the triangle.  This will result in a normal with twice the length of the area.
                v1.crs(v3)

                // Add the area-weighted normal to each vertex neighboring the triangle
                addToNormal(a, v1)
                addToNormal(b, v1)
                addToNormal(c, v1)
            }

            triangleCount_++
        }
    }

    /**
     * Creates a mesh from the shape defined in this shape builder.
     */
    fun createMesh(isStatic: Boolean = true,
                   normalizeNormals: Boolean = true,
                   clearAfterwards: Boolean = true): Mesh {

        if (normalizeNormals) normalizeNormals()

        val mesh = Mesh(isStatic,
             vertexData.size / vertexEntrySize,
                        indexes.size,
                        VertexAttribute.Position(),
                        VertexAttribute.Normal())
        mesh.setVertices(vertexData.items, 0, vertexData.size)
        mesh.setIndices(indexes.items, 0, indexes.size)

        if (clearAfterwards) clear()

        return mesh
    }

    /**
     * Creates a model from the shape defined in this shape builder.
     * Uses the specified [material] for the model.  Defaults to light gray color.
     */
    fun createModel(material: Material = Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)),
                    isStatic: Boolean = true,
                    normalizeNormals: Boolean = true,
                    clearAfterwards: Boolean = true): Model {

        // Create mesh
        val mesh = createMesh(isStatic, normalizeNormals, clearAfterwards)

        // Create model
        modelBuilder.begin()
        modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)
        return modelBuilder.end()
    }

    /**
     * Update the specified mesh with the vertices specified in this builder.
     */
    fun updateMeshVertices(mesh: Mesh) {
        mesh.updateVertices(0, vertexData.items, 0, vertexData.size)
    }

    /**
     * Normalize all vertex normals
     */
    fun normalizeNormals() {
        for (i in 0 until nextVertexId) {
            val index = i.toShort()
            getNormal(index, n)
            n.nor()
            setNormal(index, n)
        }
    }


    fun clear() {
        vertexData.clear()
        indexes.clear()
        nextVertexId = 0
        triangleCount_ = 0
    }

}