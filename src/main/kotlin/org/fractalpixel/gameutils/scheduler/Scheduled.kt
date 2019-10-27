package org.fractalpixel.gameutils.scheduler

import org.entityflakes.ComponentBase
import org.entityflakes.Entity
import org.kwrench.time.Time
import java.util.*

/**
 * Component for entities with scheduled events.
 * Optimized for few events for each entity, and not too many overall.
 */
class Scheduled(private val events: ArrayList<ScheduledEvent> = ArrayList()): ComponentBase() {

    constructor(vararg events: ScheduledEvent): this(ArrayList(events.asList()))

    /**
     * Add the specified event to this Scheduled component.
     * @param secondsUntilEvent number of game time seconds until the event
     * @param action action to execute while the event is active
     * @param durationGameSeconds game time seconds that the event is active.
     * @param positionInterpolator custom interpolator for he progress parameter to the action, or null to use the default value (0..1)
     */
    fun addEvent(secondsUntilEvent: Double,
                 action: (entity: Entity, progress: Double, gameTime: Time) -> Unit,
                 durationGameSeconds: Double? = null,
                 positionInterpolator: ((progress: Double) -> Double)? = null) {
        val startGameTime = (entity?.world?.time?.secondsSinceStart ?: 0.0) + secondsUntilEvent
        addEvent(ScheduledEvent(startGameTime, action, durationGameSeconds, positionInterpolator))
    }

    /**
     * Add the specified event to this Scheduled component.
     * The event is executed at the specified game time.
     */
    fun addEvent(event: ScheduledEvent) {
        var insertIndex = 0
        while (insertIndex < events.size &&
               event.startGameTime >= events[insertIndex].startGameTime) {
            insertIndex++
        }
        events.add(insertIndex, event)
    }

    /**
     * Add an event to delete the entity after the specified number of seconds of gametime
     */
    fun deleteAt(secondsUntilDelete: Double) {
        addEvent(secondsUntilDelete, { entity, progress, gameTime -> entity.delete() })
    }

    fun removeAllEvents() {
        events.clear()
    }

    /**
     * The last time of any scheduled event, or null if no events are scheduled.
     */
    val endTime: Double? get() = events.map{it.startGameTime + (it.durationGameSeconds ?: 0.0)}.max()

    fun update(time: Time) {
        val ownEntity = entity
        if (ownEntity != null) {
            val currentTime = time.secondsSinceStart
            // Update events starting from first that is active
            var event = events.firstOrNull()
            var index = 0
            while (event != null && event.startGameTime <= currentTime) {
                // Update event if we passed its start time
                val keep = event.updateEvent(ownEntity, time)
                if (!keep) {
                    events.removeAt(index)
                }
                else {
                    // Move to next, keep prev
                    index++
                }

                event = events.getOrNull(index)
            }
        }
    }

    override fun doInit(entity: Entity) {
        // Nothing to init
    }

    override fun doDispose() {
        removeAllEvents()
    }
}