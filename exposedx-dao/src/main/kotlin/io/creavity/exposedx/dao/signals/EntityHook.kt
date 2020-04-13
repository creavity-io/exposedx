package io.creavity.exposedx.dao.signals

import io.creavity.exposedx.dao.entities.Entity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transactionScope
import java.util.concurrent.CopyOnWriteArrayList

enum class EntityChangeType {
    Created,
    Updated,
    Removed;
}


data class EntityChange<ID : Comparable<ID>, E : Entity<ID>>(val table: Table, val entityId: EntityID<ID>, val changeType: EntityChangeType, val transactionId: String)

private val Transaction.entityEvents: MutableList<EntityChange<*, *>> by transactionScope { CopyOnWriteArrayList<EntityChange<*, *>>() }
private val entitySubscribers = CopyOnWriteArrayList<(EntityChange<*, *>) -> Unit>()

object EntityHook {
    fun subscribe(action: (EntityChange<*, *>) -> Unit): (EntityChange<*, *>) -> Unit {
        entitySubscribers.add(action)
        return action
    }

    fun unsubscribe(action: (EntityChange<*, *>) -> Unit) {
        entitySubscribers.remove(action)
    }
}

fun <ID : Comparable<ID>, E : Entity<ID>> Transaction.registerChange(table: IdTable<ID>, entityId: EntityID<ID>, changeType: EntityChangeType) {
    EntityChange(table, entityId, changeType, id).let {
        if (entityEvents.lastOrNull() != it) {
            entityEvents.add(it)
        }
    }
}

fun Transaction.alertSubscribers() {
    entityEvents.forEach { e ->
        entitySubscribers.forEach {
            it(e)
        }
    }
    entityEvents.clear()
}

fun Transaction.registeredChanges() = entityEvents.toList()

fun <T> withHook(action: (EntityChange<*, *>) -> Unit, body: () -> T): T {
    EntityHook.subscribe(action)
    try {
        return body().apply {
            TransactionManager.current().commit()
        }
    } finally {
        EntityHook.unsubscribe(action)
    }
}