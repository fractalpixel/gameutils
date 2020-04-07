package org.fractalpixel.gameutils.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kwrench.collections.bag.Bag
import java.lang.IllegalArgumentException
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
 */
// TODO: Move to utils library
open class RecyclingPool<T: Any>(
    val type: KClass<T>,
    val maxPoolSize: Int = 10_000,
    val resetInstance: ((T) -> Unit)? = null,
    val disposeInstance: ((T) -> Unit)? = null,
    val createInstance: () -> T = {type.javaObjectType.newInstance()},
    val ensureNotAlreadyContained: Boolean = true
 ) {

    // Used to lock access to internal state from different threads or co-routines
    protected val mutex = Mutex()

    // Stores previously released, unused objects.
    protected val pool = Bag<T>()

    /**
     * True if the type T implements the Recyclable interface.
     */
    val implementsRecyclable = type.isSubclassOf(Recyclable::class)

    /**
     * Obtain a new object from this pool, will use any recycled objects if available, or create a new instance if not.
     */
    fun obtain(): T = runBlocking {
        mutex.withLock {
            obtainUnlocked()
        }
    }

    /**
     * Release a recyclable object to this pool for possible later use.
     */
    fun release(obj: T) = runBlocking {
        mutex.withLock {
            releaseUnlocked(obj)
        }
    }

    /**
     * Disposes all stored recyclables.
     * This would typically be called when the pool is no longer in use, e.g. at program termination, to free allocated
     * resources, but it can also be used in other situations to temporary remove stored objects.
     */
    fun dispose() = runBlocking {
        mutex.withLock {
            disposeUnlocked()
        }
    }

    /**
     * Runs obtain without acquiring lock.  Use in descendants to modify its logic.
     */
    protected open fun obtainUnlocked() = pool.removeLast() ?: createNewInstance()

    /**
     * Creates a new instance.  Called in a locked context.  Uses the createInstance function from the constructor by default.
     */
    protected open fun createNewInstance(): T {
        return createInstance()
    }

    /**
     * Runs release without acquiring lock.  Use in descendants to modify its logic.
     */
    protected open fun releaseUnlocked(obj: T) {
        if (pool.size() < maxPoolSize) {
            // Check that not already contained
            if (ensureNotAlreadyContained && pool.contains(obj)) throw IllegalArgumentException("Can't release $obj twice!")

            // Reset object  and store it
            resetObjectUnlocked(obj)
            pool.add(obj)
        } else {
            // Pool full, dispose released object
            disposeObjectUnlocked(obj)
        }
    }

    /**
     * Disposes all stored recyclables and clears the pool.
     * Does not lock using the mutex, only call if mutex lock has been obtained already!
     */
    protected fun disposeUnlocked() {
        pool.forEach(::disposeObjectUnlocked)
        pool.clear()
    }

    protected open fun disposeObjectUnlocked(obj: T) {
        // Use the dispose function if provided
        disposeInstance?.invoke(obj)

        // Call the dispose of the object if it implements Recyclable
        if (implementsRecyclable) (obj as Recyclable).dispose()
    }

    protected open fun resetObjectUnlocked(obj: T) {
        // Use the reset function if provided
        resetInstance?.invoke(obj)

        // Call the reset of the object if it implements Recyclable
        if (implementsRecyclable) (obj as Recyclable).reset()
    }
}