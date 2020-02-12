package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.voxel.VoxelTerrain

/**
 * Renders a voxel terrain.
 */
class VoxelRendererLayer(val terrain: VoxelTerrain): Layer3D() {


    val shapeBuilder = ShapeBuilder()
    var mesh: Mesh? = null
    var modelInstance: ModelInstance? = null

    fun build() {
        // Create mesh
        val a = shapeBuilder.addVertex(Vector3(0f, 0f, -10f))
        val b = shapeBuilder.addVertex(Vector3(11f, 0f, -100f))
        val c = shapeBuilder.addVertex(Vector3(11f, 11f, 10f))
        val d = shapeBuilder.addVertex(Vector3(0f, 11f, 110f))
        shapeBuilder.addQuad(a, b, c, d, true)
        mesh = shapeBuilder.createMesh()

        // Create modelInstance
        val modelBuilder = ModelBuilder()
        val material = Material()
        material.set(ColorAttribute.createDiffuse(0.5f, 0.6f, 0.55f, 1f))
        modelBuilder.begin()
        modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)
        val model = modelBuilder.end()
        modelInstance = ModelInstance(model)
    }



    override fun render(context: RenderingContext3D) {
        if (modelInstance == null) build()

        val currentModelInstance = modelInstance
        if (currentModelInstance != null) {
            context.modelBatch.render(currentModelInstance, context.environment)
        }
    }
}