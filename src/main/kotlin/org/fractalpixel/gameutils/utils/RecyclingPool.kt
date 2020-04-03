package org.fractalpixel.gameutils.utils

import org.kwrench.collections.bag.Bag
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Creates new objects of the given [type] by either re-using previously released objects or
 * creating new ones if there are no released ones.  The stored objects are limited to [maxPoolSize], if more are
 * released, they are simply disposed immediately.
 *
 * When creating objects, by default uses the zero-argument constructor of the object, but a custom [createInstance]
 * function can also be provided.
 *
 * If the objects implement the [Recyclable] interface, the reset and dispose functions of it are called
 * when needed.  You can also specify [resetInstance] and [disposeInstance] functions in the constructor, these are run
 * before the objects own reset and dispose functions, this is useful if pooling some third party object that doesn't
 * implement [Recyclable].  If you do not need to do any special reset or dispose actions, you can also use objects
 * without implementing [Recyclable] or providing reset or dispose functions.
 *
 *
 * Not thread safe.
 */
// TODO: Move to utils library
// TODO: Add thread safety?
open class RecyclingPool<T: Any>(
    val type: KClass<T>,
    val maxPoolSize: Int = 10_000,
    val resetInstance: ((T) -> Unit)? = null,
    val disposeInstance: ((T) -> Unit)? = null,
    val createInstance: () -> T = {type.javaObjectType.newInstance()}
 ) {

    // Stores previously released, unused objects.
    protected val pool = Bag<T>()

    /**
     * True if the type T implements the Recyclable interface.
     */
    val implementsRecyclable = type.isSubclassOf(Recyclable::class)

    /**
     * Obtain a new object from this pool, will use any recycled objects if available, or create a new instance if not.
     */
    fun obtain(): T = pool.removeLast() ?: createInstance()

    /**
     * Release a recyclable object to this pool for possible later use.
     */
    fun release(obj: T) {
        if (pool.size() < maxPoolSize) {
            // Reset object  and store it
            resetObject(obj)
            pool.add(obj)
        } else {
            // Pool full, dispose released object
            disposeObject(obj)
        }
    }

    /**
     * Disposes all stored recyclables.
     * This would typically be called when the pool is no longer in use, e.g. at program termination, to free allocated
     * resources, but it can also be used in other situations to temporary remove stored objects.
     */
    fun dispose() {
        pool.forEach(::disposeObject)
        pool.clear()
    }

    private fun resetObject(obj: T) {
        // Use the reset function if provided
        resetInstance?.invoke(obj)

        // Call the reset of the object if it implements Recyclable
        if (implementsRecyclable) (obj as Recyclable).reset()
    }

    private fun disposeObject(obj: T) {
        // Use the dispose function if provided
        disposeInstance?.invoke(obj)

        // Call the dispose of the object if it implements Recyclable
        if (implementsRecyclable) (obj as Recyclable).dispose()
    }
}