package org.fractalpixel.gameutils.scheduler

import org.entityflakes.Entity
import org.entityflakes.entitymanager.ComponentRef
import org.entityflakes.processor.EntityProcessorBase
import org.mistutils.time.Time
import org.mistutils.updating.strategies.VariableTimestepStrategy


/**
 * Processes entities with Scheduled components, running scheduled actions.
 */
class ScheduleProcessor: EntityProcessorBase(VariableTimestepStrategy(), Scheduled::class) {

    val scheduledRef = ComponentRef(Scheduled::class)

    override fun updateEntity(entity: Entity, time: Time) {
        scheduledRef[entity].update(time)
    }
}