package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import org.kwrench.math.*


/**
 * Builds a surface by adding vertexes to a ShapeBuilder and connecting the added vertexes to
 * ones previously added by this LayeredShapeBuilder to create a surface.
 */
class LayeredShapeBuilder(var shapeBuilder: ShapeBuilder? = null) {

    private val tempTransformation = Matrix4()
    private val tempCenter = Vector3()

    private val tempTranslation = Vector3()
    private val tempRotation = Quaternion()
    private val tempScale = Vector3()

    // Indexes of vertexes in the previous layer
    private val previousIndexes = com.badlogic.gdx.utils.ShortArray()
    // Indexes of vertexes in the current layer
    private val currentIndexes = com.badlogic.gdx.utils.ShortArray()


    /**
     * The points that define the current layer.
     */
    val buildPoints = java.util.ArrayList<Vector3>()

    /**
     * Sets the build points positions to zero, and ensures there is the specified number of build points.
     */
    fun resetBuildPoints(number: Int = 16) {
        buildPoints.ensureCapacity(number)
        while (buildPoints.size < number) buildPoints.add(Vector3())
        while (buildPoints.size > number) buildPoints.removeAt(buildPoints.size - 1)
        for (i in 0 .. buildPoints.size - 1) {
            buildPoints[i].setZero()
        }
    }


    /**
     * Replace all build points with new ones.
     * Checks if the newBuildPoints list is the same as the current buildPoints list, in that case does nothing.
     */
    fun setBuildPoints(newBuildPoints: List<Vector3>) {
        if (newBuildPoints != buildPoints) {
            buildPoints.clear()
            buildPoints.addAll(newBuildPoints)
        }
    }

    /**
     * Calculates the center of all build points, that is, the average of their locations.
     * If there are no build points, the center is considered to be at 0,0,0.
     */
    fun calculateCenter(centerOut: Vector3): Vector3 {
        val numPoints = buildPoints.size
        centerOut.setZero()
        for (i in 0 .. numPoints -1) {
            centerOut.add(buildPoints[i])
        }

        if (numPoints > 0) centerOut.scl(1f / numPoints)
        return centerOut
    }

    /**
     * Clears any existing build points.
     * Any new layer will not connect to any previous points.
     * @param shapeBuilder can be used to set a new shapeBuilder to use at the same time.
     * If null or not provided, the current shapeBuilder will be used.
     */
    fun reset(shapeBuilder: ShapeBuilder? = null): LayeredShapeBuilder {
        buildPoints.clear()
        previousIndexes.clear()
        currentIndexes.clear()

        if (shapeBuilder != null) this.shapeBuilder = shapeBuilder

        return this
    }



    /**
     * Arranges the build points in a circle on the x-z plane, facing up towards positive y.
     * @param radius the distance of each point from origo
     * @param numberOfPoints number of build points to generate.
     */
    fun circle(radius: Float,
               numberOfPoints: Int = 16): LayeredShapeBuilder {

        resetBuildPoints(numberOfPoints)

        for (i in 0 .. numberOfPoints-1) {
            val t = (1.0 * i) / numberOfPoints
            buildPoints[i].set(
                    (radius * Math.cos(t * Tau)).toFloat(),
                    0f,
                    (radius * Math.sin(t * Tau)).toFloat()
            )
        }

        return this
    }

    /**
     * Arranges the build points in a circular shape on the x-z plane, facing up towards positive y,
     * using the specified function to calculate the distance of each point from the center.
     * @param radialScale a function that gets a direction vector (with coordinates in -1..1 range) and returns the distance of the specified point from origo.
     * @param numberOfPoints number of build points to generate.
     */
    fun circle(normalScale: (Double, Double) -> Double,
               numberOfPoints: Int = 16): LayeredShapeBuilder {
        return circle({t ->
            val x = Math.cos(t * Tau)
            val y = Math.sin(t * Tau)
            normalScale(x, y)
        }, numberOfPoints)
    }

    /**
     * Arranges the build points in a circular shape on the x-z plane, facing up towards positive y,
     * using the specified function to calculate the distance of each point from the center.
     * @param radialScale a function that gets a value from 0 to 1 and returns the distance of the specified point from origo.
     * @param numberOfPoints number of build points to generate.
     */
    fun circle(radialScale: (Double) -> Double,
               numberOfPoints: Int = 16): LayeredShapeBuilder {

        resetBuildPoints(numberOfPoints)

        for (i in 0 .. numberOfPoints-1) {
            val t = (1.0 * i) / numberOfPoints
            val radius = radialScale(t).toFloat()
            buildPoints[i].set(
                    radius * Math.cos(t * Tau).toFloat(),
                    0f,
                    radius * Math.sin(t * Tau).toFloat()
            )
        }

        return this
    }

    /**
     * Arranges the build points in a box shape on the x-z plane, facing up towards positive y and centered on origo.
     * Uses one build points for each corner.
     * @param sizeX size of the box along the x axis.
     * @param sizeZ size of the box along the z axis.
     */
    fun simpleBox(sizeX: Float,
                  sizeZ: Float): LayeredShapeBuilder {

        resetBuildPoints(4)

        val x = sizeX * 0.5f
        val z = sizeZ * 0.5f
        var index = 0
        buildPoints[index++].set(-x, 0f, -z)
        buildPoints[index++].set(x, 0f, -z)
        buildPoints[index++].set(x, 0f, z)
        buildPoints[index++].set(-x, 0f, z)

        return this
    }

    /**
     * Arranges the build points in a box shape on the x-z plane, facing up towards positive y and centered on origo.
     * Uses two build points for each corner, to force sharp normals.  Use cornerBevelFraction to round the corners if desired.
     * @param sizeX size of the box along the x axis.
     * @param sizeZ size of the box along the z axis.
     * @param cornerBevelFraction fraction of the shorter edge size that will be used to bevel or round the corner.
     */
    fun box(sizeX: Float,
            sizeZ: Float,
            cornerBevelFraction: Float = 0f): LayeredShapeBuilder {

        resetBuildPoints(4 * 2)

        val xs = sizeX * 0.5f
        val zs = sizeZ * 0.5f

        val bevelSize = (xs min zs) * cornerBevelFraction.clampTo(0f, 0.5f)
        var index = 0
        buildPoints[index++].set(-xs + bevelSize, 0f, -zs)
        buildPoints[index++].set(xs - bevelSize, 0f, -zs)
        buildPoints[index++].set(xs, 0f, -zs + bevelSize)
        buildPoints[index++].set(xs, 0f, zs - bevelSize)
        buildPoints[index++].set(xs - bevelSize, 0f, zs)
        buildPoints[index++].set(-xs + bevelSize, 0f, zs)
        buildPoints[index++].set(-xs, 0f, zs - bevelSize)
        buildPoints[index++].set(-xs, 0f, -zs + bevelSize)

        return this
    }


    /**
     * Arranges the build points in a box shape on the x-z plane, facing up towards positive y and centered on origo.
     * Uses two build points for each corner, to force sharp normals.  Use cornerBevelFraction to round the corners if desired.
     * @param sizeX size of the box along the x axis.
     * @param sizeZ size of the box along the z axis.
     * @param cornerBevelFraction fraction of the shorter edge size that will be used to bevel or round the corner.
     */
    fun brokeBox(sizeX: Float,
            sizeZ: Float,
            cornerBevelFraction: Float = 0f): LayeredShapeBuilder {

        val tempPos = Vector3()
        val tempBevel = Vector3()
        val tempSide = Vector3()

        resetBuildPoints(4 * 2)

        fun initSide(sideCenter: Vector3, sideLength: Float, bevelSize: Float, index: Int) {
            tempSide.set(sideCenter)
            tempSide.crs(Vector3.Zero)
            tempSide.nor().scl(sideLength * 0.5f)

            tempBevel.set(tempSide).nor().scl(bevelSize)

            tempPos.set(sideCenter).sub(tempSide).sub(tempBevel)
            buildPoints[index].set(tempPos)

            tempPos.set(sideCenter).add(tempSide).add(tempBevel)
            buildPoints[index+1].set(tempPos)
        }

        val bevelSize = (sizeX min sizeZ) * cornerBevelFraction
        val sideCenter = Vector3()
        initSide(sideCenter.set(sizeX*0.5f, 0f, 0f), sizeZ, bevelSize, 0)
        initSide(sideCenter.set(0f, 0f, sizeZ*0.5f), sizeX, bevelSize, 2)
        initSide(sideCenter.set(-sizeX*0.5f, 0f, 0f), sizeZ, bevelSize, 4)
        initSide(sideCenter.set(0f, 0f, -sizeZ*0.5f), sizeX, bevelSize, 6)

        return this
    }

    /**
     * Arranges the build points using the specified function.
     * @param positionFunction a function that gets a value from 0 to 1 and a vector to set the build point position to.
     * @param numberOfPoints number of build points to generate.
     */
    fun positions(positionFunction: (Double, Vector3) -> Unit,
                  numberOfPoints: Int = 16): LayeredShapeBuilder {

        resetBuildPoints(numberOfPoints)

        for (i in 0 .. numberOfPoints-1) {
            val t = (1.0 * i) / numberOfPoints
            positionFunction(t, buildPoints[i])
        }

        return this
    }

    /**
     * Transform all build points using the specified transformation
     */
    fun move(transformation: Matrix4): LayeredShapeBuilder {
        for (i in 0 until buildPoints.size) {
            buildPoints[i].mul(transformation)
        }

        return this
    }


    /**
     * Moves the build points by applying the specified transformations to all of them.
     * @param dX position movement.
     * @param dY position movement.
     * @param dZ position movement.
     * @param scaleX scale along x axis.  Scaling is applied before rotation.  The scaling center is the center of all build points.
     * @param scaleY scale along y axis.  Scaling is applied before rotation.  The scaling center is the center of all build points.
     * @param scaleZ scale along z axis.  Scaling is applied before rotation.  The scaling center is the center of all build points.
     * @param rotateX rotation around the x axis, in full turns (1 = one rotation).  The rotation center is the center of all build points.
     * @param rotateY rotation around the y axis, in full turns (1 = one rotation).  The rotation center is the center of all build points.
     * @param rotateZ rotation around the z axis, in full turns (1 = one rotation).  The rotation center is the center of all build points.
     */
    fun move(dX: Float,
             dY: Float,
             dZ: Float,
             scaleX: Float = 1f,
             scaleY: Float = 1f,
             scaleZ: Float = 1f,
             rotateX: Float = 0f,
             rotateY: Float = 0f,
             rotateZ: Float = 0f): LayeredShapeBuilder {
        move(tempTranslation.set(dX, dY, dZ),
             tempScale.set(scaleX, scaleY, scaleZ),
             tempRotation.setEulerAnglesRad(rotateX * TauFloat, rotateY * TauFloat, rotateZ * TauFloat))
        return this
    }

    /**
     * Moves the build points by applying the specified transformations to all of them.
     * @param translation translation to apply to the build points.
     * @param scale scale to apply to the build points.  Uses the average point location as the scale origin.  Scaling is applied before rotation.
     * @param rotation rotation to apply to the build points.  Uses the average point location as the rotation origin.
     */
    fun move(translation: Vector3 = NO_TRANSLATION,
             scale: Vector3 = NO_SCALING,
             rotation: Quaternion = NO_ROTATION): LayeredShapeBuilder {

        tempTransformation.idt()

        // Get center position
        calculateCenter(tempCenter)

        // Translate so that center is at origo
        tempCenter.scl(-1f)
        tempTransformation.translate(tempCenter)

        // Apply scale
        tempTransformation.scale(scale.x, scale.y, scale.z)

        // Apply rotation
        tempTransformation.rotate(rotation)

        // Translate back to original position
        tempCenter.scl(-1f)
        tempTransformation.translate(tempCenter)

        // Translate to new position
        tempTransformation.translate(translation)

        // Apply transformation to build points
        move(tempTransformation)

        return this
    }


    /**
     * Returns a copy of this LayeredShapeBuilder, using the same ShapeBuilder and starting from the current indexes and build points of this LayeredShapeBuilder.
     * Can be used e.g. to create branches that start from the same position and fork off.
     * @param reverse if true, the copy will face in the opposite direction of this LayeredShapeBuilder (that is, the build point array will be reversed)
     */
    fun fork(reverse: Boolean = false): LayeredShapeBuilder {
        val copy = LayeredShapeBuilder(shapeBuilder)
        copy.setBuildPoints(buildPoints)
        copy.currentIndexes.addAll(currentIndexes)
        copy.previousIndexes.addAll(previousIndexes)

        if (reverse) {
            copy.currentIndexes.reverse()
            copy.previousIndexes.reverse()
            copy.buildPoints.reverse()
        }

        return copy
    }


    /**
     * Adds a cap to the current layer, by adding one point at the average location of all build points and connecting all
     * build points to it with triangles.
     * @param sharpEdges if true, will add an additional layer before adding the cap, to allow normals to be sharp at the transition.
     * @param invertFaces if true, the generated faces will be inverted.
     */
    fun addStartCap(sharpEdges: Boolean = false, invertFaces: Boolean = false): LayeredShapeBuilder = addCap(false, sharpEdges, invertFaces)

    /**
     * Adds a cap to the current layer, by adding one point at the average location of all build points and connecting all
     * build points to it with triangles.
     * @param sharpEdges if true, will add an additional layer before adding the cap, to allow normals to be sharp at the transition.
     * @param invertFaces if true, the generated faces will be inverted.
     */
    fun addEndCap(sharpEdges: Boolean = false, invertFaces: Boolean = false): LayeredShapeBuilder = addCap(true, sharpEdges, invertFaces)

    /**
     * Adds a cap to the current layer, by adding one point at the average location of all build points and connecting all
     * build points to it with triangles.
     * @param forward if true, the surface will face in the direction the layers are built.  If false, it will face backward.
     * @param sharpEdges if true, will add an additional layer before adding the cap, to allow normals to be sharp at the transition.
     * @param invertFaces if true, the generated faces will be inverted.
     */
    fun addCap(forward: Boolean, sharpEdges: Boolean = false, invertFaces: Boolean = false): LayeredShapeBuilder {
        val numPoints = currentIndexes.size
        if (numPoints >= 3) {
            val shapeBuilder = shapeBuilderNotNull

            // Add additional ring at end before cap, to make a sharp transition to it
            if (forward && sharpEdges) buildVertexes(previousIndexes, currentIndexes)

            // Add vertex for midpoint
            val midIndex = shapeBuilder.addVertex(calculateCenter(tempCenter))

            // Add triangles from the edges to the midpoint
            for (i in 0 until numPoints) {
                val aIndex = currentIndexes[i]
                val bIndex = currentIndexes[(i + 1) % numPoints]
                shapeBuilder.addTriangle(aIndex, bIndex, midIndex, invertFace = forward != invertFaces)
            }

            // Add additional ring at start after the cap, to make a sharp transition from it
            if (!forward && sharpEdges) buildVertexes(previousIndexes, currentIndexes)
        }

        return this
    }

    /**
     * Sets the build points and adds a layer.
     * @param buildPoints the new build points to use.
     * @param transform transform to apply to the build points.  Defaults to none.
     * @param loop if true, the last build point will connect to the first one, forming a closed surface.
     * @param invertFaces if true, the generated faces will be inverted.
     */
    fun addLayer(buildPoints: List<Vector3>,
                 transform: Matrix4 = NO_TRANSFORM,
                 loop: Boolean = true,
                 invertFaces: Boolean = false): LayeredShapeBuilder {
        setBuildPoints(buildPoints)
        addLayer(transform, loop, invertFaces)

        return this
    }

    /**
     * Adds a new layer by applying a transformation to the current build point locations.
     * @param translation translation to apply to the build points.
     * @param scale scale to apply to the build points.  Uses the average point location as the scale origin. Scaling is applied before rotation.
     * @param rotation rotation to apply to the build points.  Uses the average point location as the rotation origin.
     * @param loop if true, the last build point will connect to the first one, forming a closed surface.
     * @param invertFaces if true, the generated faces will be inverted.
     * @param repeats how many times the translation should be applied and a layer added.
     */
    fun addLayer(translation: Vector3,
                 scale: Vector3 = NO_SCALING,
                 rotation: Quaternion = NO_ROTATION,
                 loop: Boolean = true,
                 invertFaces: Boolean = false,
                 repeats: Int = 1): LayeredShapeBuilder {
        for (i in 1..repeats) {
            move(translation, scale, rotation)
            addLayer(loop, invertFaces)
        }

        return this
    }


    /**
     * Adds a new layer by transforming the build points using the specified transform and then using their new positions.
     * @param transform transform to apply to the build points.
     * @param loop if true, the last build point will connect to the first one, forming a closed surface.
     * @param invertFaces if true, the generated faces will be inverted.
     * @param repeats how many times the translation should be applied and a layer added.
     */
    fun addLayer(transform: Matrix4,
                 loop: Boolean = true,
                 invertFaces: Boolean = false,
                 repeats: Int = 1): LayeredShapeBuilder {
        for (i in 1..repeats) {
            move(transform)
            addLayer(loop, invertFaces)
        }

        return this
    }

    /**
     * Adds a new layer using the current build point locations.
     * @param loop if true, the last build point will connect to the first one, forming a closed surface.
     * @param invertFaces if true, the generated faces will be inverted.
     */
    fun addLayer(loop: Boolean = true,
                 invertFaces: Boolean = false): LayeredShapeBuilder {

        // Create new vertexes at the current location of the bulid points
        buildVertexes(previousIndexes, currentIndexes)

        // Build faces between the previous layer and the current layer
        buildFaces(previousIndexes, currentIndexes, loop, invertFaces)

        return this
    }

    private fun buildVertexes(previousIndexes: com.badlogic.gdx.utils.ShortArray,
                              currentIndexes: com.badlogic.gdx.utils.ShortArray) {
        // Update previous indexes
        previousIndexes.clear()
        previousIndexes.addAll(currentIndexes)

        // Add vertex indexes
        currentIndexes.clear()
        val shapeBuilder = shapeBuilderNotNull
        for (i in 0 until buildPoints.size) {
            currentIndexes.add(shapeBuilder.addVertex(buildPoints[i]))
        }
    }

    private fun buildFaces(previousIndexes: com.badlogic.gdx.utils.ShortArray,
                           currentIndexes: com.badlogic.gdx.utils.ShortArray,
                           loop: Boolean,
                           invertFaces: Boolean) {
        if (previousIndexes.size <= 0 || currentIndexes.size <= 0) return

        val shapeBuilder = shapeBuilderNotNull

        // Create as many faces as needed between the previous and current indexes
        val num: Int = previousIndexes.size max currentIndexes.size
        var prevIndex1 = 0
        var currentIndex1 = 0
        for (i in 1 until num) {
            val prevIndex2 = (i * previousIndexes.size) / num
            val currentIndex2 = (i * currentIndexes.size) / num

            buildQuad(shapeBuilder, currentIndex1, currentIndex2, prevIndex1, prevIndex2, invertFaces)

            prevIndex1 = prevIndex2
            currentIndex1 = currentIndex2
        }

        if (loop && previousIndexes.size > 0 && currentIndexes.size > 0) {
            buildQuad(shapeBuilder, currentIndex1, 0, prevIndex1, 0, invertFaces)
        }
    }

    private fun buildQuad(shapeBuilder: ShapeBuilder, currentIndex1: Int, currentIndex2: Int, prevIndex1: Int, prevIndex2: Int, invertFaces: Boolean) {
        shapeBuilder.addQuad(currentIndexes[currentIndex1],
                             currentIndexes[currentIndex2],
                             previousIndexes[prevIndex2],
                             previousIndexes[prevIndex1],
                             invertFace = invertFaces)
    }

    private val shapeBuilderNotNull: ShapeBuilder get() = shapeBuilder ?: throw IllegalStateException("ShapeBuilder has not been set.  Use the constructor or clear method to set it.")


    companion object {
        private val NO_TRANSLATION = Vector3()
        private val NO_TRANSFORM = Matrix4()
        private val NO_SCALING = Vector3(1f, 1f, 1f)
        private val NO_ROTATION = Quaternion()
    }


}