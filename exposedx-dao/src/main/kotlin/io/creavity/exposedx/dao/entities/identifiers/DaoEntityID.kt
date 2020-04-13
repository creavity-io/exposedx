package io.creavity.exposedx.dao.entities.identifiers

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import io.creavity.exposedx.dao.manager.transactionCache
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.TransactionManager
import kotlin.reflect.KFunction0

class DaoEntityID<T:Comparable<T>>(id: T?, table: IdTable<T>) : EntityID<T>(table, id) {

    override fun invokeOnNoValue() {
        TransactionManager.current().transactionCache[table].flushInserts()
    }

}
