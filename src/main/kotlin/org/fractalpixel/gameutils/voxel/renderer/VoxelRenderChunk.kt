package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.BaseLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import org.entityflakes.Entity
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.libgdxutils.buildWireframeBoxPart
import org.fractalpixel.gameutils.lights.InfiniteLight
import org.fractalpixel.gameutils.lights.LightProvider
import org.fractalpixel.gameutils.lights.SphericalLight
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.space.Location
import org.fractalpixel.gameutils.utils.MeshPool
import org.fractalpixel.gameutils.utils.Recyclable
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.ConstantFun
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.volume.Volume

/**
 * Holds rendering data for a voxel chunk.
 */
// TODO: Decide on way to handle lighting:
//       A) Up to N lights per chunk, use the strongest.
//          PRO: Easy to implement (libGDX default)
//          CON: Causes nasty seams if there are many lights
//          Conclusion: Unacceptable
//       B) Deferred lighting / shading.
//          PRO: Fast
//          PRO: Uniform approach for all renderable geometry, moving entities cast shadows
//          CON: Very complex to implement -- high risk of blocking project
//          CON: Volumetric shading not included, requries separate solution
//          CON: Still requires shadow map for shadows, further increasing complexity
//          CON: Radiosity requires separate approach
//       C) Volumetric lighting
//            * store depth info for chunks
//            - keep grid of sample points for a detail level (could maybe use blue-noise scattering if more efficient, but that could
//              complicate match so not initially).  Perhaps about 1 sample point for each 2 or 4 voxels, if terrain is smooth (for
//              buildings and other cubic, artificial voxels have 1 sample point per voxel?).
//            * store light from x directions (e.g. every 45 or 22.5 degrees) for each sample point (or ideally more for better light direction resolution) - basically spherical cubemap
//            * store fog color, opacity, scattering, reflectivity(?), flow direction, speed, current flow pos/phase, and texture type(s)/parameters for each sample point
//            - raymarch in stored chunk depth info from each sample point towards each light that the sample point is in range of,
//              calculate atmospheric effect from sample points on the way, and calculate umbra from distance function,
//              assign attenuated light from the direction to the weighted directional components of the sample point (basically light texture)
//            - when level updated, upload the sample point data to graphics card by updating a level specific texture? (or data object) with the data
//              (a multi-channel, or several 3D textures could be most optimal, as they'd allow hardwware supported interpolation).
//            - when rendering, step from the point in the fragment shader towards the camera along (interpolated) sample points,
//              apply attenuation, scattered incoming light (use light map at the sample point), and the fog texture at the correct pos.
//              (fog texture could perhaps be replaced by particle systems that drift along the flow lines for greater continuity of particles?  Or why not both.....  Or actually skip it until needed..)
//            - Adaptive sampling point placement would be ideal (more closer to ground intersection, lesser further out - maybe use max of block size and distance at the location? And one sample for air chunks
//            - As an additional feature, some kind of radiosity simulation could be run with this also..
//            - Could it be possible to propagate the light with multiple iterations instead of raymarching each point to every light?  Expand from lights instead?  Maybe.  Maybe easier to do radiosity that way.  Would it be slower?  Maybe.
//            - When new area scrolls in, use interpolated values of lower level of detail samples as initial sample values, while calculating better values.
//            - Propagating radiosity information between samples should have a comparable (or slightly better, if there
//              are many lights) performance to path tracing from each sample to each visible light,
//              and would enable bouncing and scattering light.  It can also be progressively refined,
//              especially if interpolated more rough samples can be used as a base.  A few chunks should be calculable in a frame.  And progressive refinement is nice to amortize.
//            - ALGORITHM: For each sample, Read inputs from neighbours in our direction, attenuate, diffract,
//              and if we are next to a surface, bounce them, calculate output for ourselves, put it in interleaved
//              output buffer, switch buffers next iteration.  If a light overlaps us or is neighbouring us, interpolate
//              it's intensity with neighbours and our pos, then add it's spherical radiance color function to our output.
//              If we are at level of detail outer boundary, get light information from interpolated samples of lower level of detail.
//              If we are at lowest level of detail outer boundary, get incoming surrounding spherical light radiance.
//              ALGORITHM PRO: Simple, local, scalable, can use lower LOD estimates as input, reacts 'automatically' to
//              moving light sources and adding / removing light sources (number of light sources does not affect performance,
//              and area lights are easy), iteratively improves illumination, while still doing major illumination fast enough to appear realtime (according to my napkin calculations..)
//              ALGORITHM CON: Light direction not terribly accurate (hack: orient sample light sphere in direction of strongest light? -> correct and sharpish shadows for most dominant light source in each location)
//              ALGORITHM CON: About 100 MB additional storage space with a quite sparse grid (evey fourth block) (assuming 10 levels of detail) and very coarse light information
//          PRO: Level of detail based volumetric shading with about same effort
//          PRO: Soft shadows
//          PRO: Lighting info for entities
//          PRO: Global illumination info (at low resolution)
//          CON: No very sharp shadows possible (not a great loss, a bit of haze is nice and usual, only a drawback in vacuum, and on the other hand soft shadows almost free).
//          CON: Need to store depth info for chunks instead of throwing it away - except if there are no intersections I guess -
//               maybe just store values for centers and interpolate in that case, to get some kind of semi-sensible soft shadows.
//          CON: Need to upload volumetric data to gfx card, but that would be required anyway for volumetric info
//          CON: Need to raymarch chunks for each sample point, and recalulate all samples within radius when a light moves or terrain changes
//          PRO: Radiosity seems possible with additional thought and work
//          PRO: In fragment shader, you get lights pretty much by just interpolating the adjacent samples -- but the fog between the fragment and the camera still need to be marched and voxel light data applied.
//          CON: Light direction is not very exact, so highly reflective materials like water not that good - but on the other hand the hemisphere can be reflected.
//          CON: Entities do not cast shadows, unless they define some distance function and the lights are updated when they move.  They might fake some shadow though?
//          CON: Buildings / blocky structures / recursive structures require integration with the distance function sampling (and atmosphere if they handle it? - maybe best if not?)
//
class VoxelRenderChunk(val configuration: VoxelConfiguration): Recyclable {

    private val pos = MutableInt3()
    val position: Int3 get() = pos // Do not allow editing

    var terrain: VoxelTerrain = emptyTerrain
        private set

    var level: Int = 0
        private set


    private var initialShape: ShapeBuilder? = null
    private var mesh: Mesh? = null
    var modelInstance: ModelInstance? = null

    /**
     * True if the model has been created and is rendering.
     */
    var initialized: Boolean = false
        private set

    // Used for frustum culling
    private val bounds = BoundingBox()
    private var boundingSphereRadius = 0f
    private val boundingSphereCenter = Vector3()


    /**
     * Initialize the position and detail level of this chunk and tell it the terrain it is located in.
     */
    fun init(
        terrain: VoxelTerrain,
        level: Int,
        pos: Int3,
        shape: ShapeBuilder
    ) {
        this.terrain = terrain
        this.level = level
        this.pos.set(pos)
        this.initialShape = shape

        bounds.clr()
        boundingSphereRadius = configuration.calculateBoundingSphere(pos, level, boundingSphereCenter)

    }

    /**
     * Update mesh based on latest terrain distance function.
     */
    fun update() {
        // TODO: Update mesh if world edited, run in background co-routine...
    }

    /**
     * Called to render this chunk.
     * Also creates the 3D model from the shape whenever the calculation is ready.
     */
    fun render(context: RenderingContext3D) {
        initializeModelIfCalculated()

        // Create relevant lights
        // TODO: Caache lights between invocations?
        val directionalLights = ArrayList<DirectionalLight>()
        val pointLights = ArrayList<PointLight>()

        // Directional
        terrain.lightProvider.forEachInfiniteLight { entity, light ->
            directionalLights.add(light.createGdxLight())
        }

        // Point
        val volume = configuration.getChunkVolume(pos, level)
        val minimumSize = configuration.blockWorldSize(level)
        terrain.lightProvider.forEachPointLight(volume, minimumSize) { entity, location, light ->
            pointLights.add(light.createGdxLight())
        }

        // Add lights to environment
        // TODO: Use own structures?
        val environment = context.environment!!
        directionalLights.forEach { environment.add(it) }
        pointLights.forEach { environment.add(it) }

        // Render model instance if available
        val currentModelInstance = modelInstance
        if (currentModelInstance != null /* && isVisible(context.camera) */ ) {
            context.modelBatch.render(currentModelInstance, environment)
        }

        // Remove added lights
        directionalLights.forEach { environment.remove(it) }
        pointLights.forEach { environment.remove(it) }
    }

    /* BUG: Frustrum culling doesn't seem to work correctly, keeps flickering between frames.
             Might have to implement it ourselves?  Or is the problem misaligned chunks?  But bounding box is based on actual geometry..

     */
    /**
     * Returns true if this chunk would be visible to the camera.
     */
    private fun isVisible(camera: Camera): Boolean {
        //camera.update()
        return camera.frustum.sphereInFrustumWithoutNearFar(boundingSphereCenter, boundingSphereRadius)
        //return camera.frustum.boundsInFrustum(bounds)
    }




    private fun initializeModelIfCalculated() {
        // Create OpenGL mesh when a calculation is ready
        val shape = initialShape
        if (shape != null) {
            // Initialize OpenGL constructs in OpenGL thread
            createModelInstance( createMesh(shape) )

            // Release the shape as it was already used to build the mesh and is no longer needed
            releaseShape()

            initialized = true
        }
    }

    private fun createMesh(shape: ShapeBuilder): Mesh {
        // Get or create mesh instance
        val createdMesh = meshPool.obtain(shape.vertexCount, shape.indexCount)

        // Build shape from the surface points we found
        shape.updateMesh(createdMesh, false)

        mesh = createdMesh
        return createdMesh
    }

    private fun createModelInstance(createdMesh: Mesh) {
        // Build the model and create an instance of it
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        // Add some wireframes showing where the chunks are if requested
        var debugColor = Color.WHITE

        // TODO: Avoid calculating this if no debug visualization is on..
        val mayContainSurface = terrain.distanceFun.mayContainSurface(configuration.getChunkVolume(pos, level), configuration.blockWorldSize(level))
        val wireframeColor = configuration.calculateBlockLevelDebugColor(level, mayContainSurface, createdMesh.numIndices > 0)

        // IDEA: Add different debug visualization modes later if needed?  Maybe more generic system

        if (configuration.debugLines) {
            val corner = configuration.chunkWorldCornerPos(pos, level)
            var sideLen = configuration.chunkWorldSize(level).toFloat()

            if (configuration.debugOutlines) {
                modelBuilder.buildWireframeBoxPart(corner, sideLen, color = configuration.blockEdgeDebugLineColor, id = wireframeId)
            }

            if (configuration.debugLinesForEmptyBlocks || mayContainSurface) {
                corner.add(configuration.blockTypeDebugLineSpacing * sideLen)
                sideLen *= (1f - 2f * configuration.blockTypeDebugLineSpacing)
                modelBuilder.buildWireframeBoxPart(
                    corner, sideLen, color = wireframeColor, id = wireframeId
                )
            }
        }

        // Debug visualize this:
        if (configuration.colorizeTerrainByLevel) debugColor = wireframeColor

        // Add terrain shape to model
        if (createdMesh.numVertices > 0 && createdMesh.numIndices > 0) {
            val material = Material()
            material.set(ColorAttribute.createDiffuse(0.8f * debugColor.r, 0.8f * debugColor.g, 0.8f * debugColor.b, 1f))
            modelBuilder.part(voxelTerrainChunkId, createdMesh, GL20.GL_TRIANGLES, material)
        }

        // Create model from chunk shape and wireframe
        val model = modelBuilder.end()

        // Create instance of model
        val instance = ModelInstance(model)
        modelInstance = instance

        /*
        // Initialize bounds
        model.calculateBoundingBox(bounds)
         */
    }

    override fun dispose() {
        // Same procedure as for resetting, as all meshes are pooled so they are not disposed here.
        reset()
    }

    override fun reset() {
        releaseShape()
        releaseMesh()

        initialized = false
        terrain = emptyTerrain
        level = 0
        pos.zero()
        modelInstance = null
        bounds.clr()
    }

    private fun releaseShape() {
        // Release the shape (if it is not null)
        initialShape?.let { ShapeCalculator.shapeBuilderPool.release(it) }
        initialShape = null
    }

    private fun releaseMesh() {
        // Free mesh for later re-use
        mesh?.let{ meshPool.release(it) }

        // Set it to null so that it doesn't get released again
        mesh = null
    }


    companion object {
        val emptyLightProvider = object : LightProvider {
            override fun forEachPointLight(volume: Volume, minimumSize: Double, visitor: (entity: Entity, location: Location, light: SphericalLight) -> Unit) {
            }

            override fun forEachInfiniteLight(visitor: (entity: Entity, light: InfiniteLight) -> Unit) {
            }
        }

        private val emptyTerrain = VoxelTerrain(ConstantFun(1.0), emptyLightProvider)

        private val meshPool = MeshPool() // This needs to be accessed from the OpenGL thread anyway, so keep it here.

        /**
         * Used as id for meshparts that contain voxel terrain.
         * Used by the shader provider to decide on the shader to use.
         */
        val voxelTerrainChunkId = "voxelTerrainChunk"

        /**
         * Used as id for meshparts that contain voxel terrain.
         * Used by the shader provider to decide on the shader to use.
         */
        val wireframeId = "wireframe"

    }

}