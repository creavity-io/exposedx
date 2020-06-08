package io.creavity.exposedx.dao.entities.identifiers

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import io.creavity.exposedx.dao.tables.transactionCache
import org.jetbrains.exposed.sql.transactions.TransactionManager

class DaoEntityID<T:Comparable<T>>(id: T?, table: IdTable<T>) : EntityID<T>(table, id) {

    override fun invokeOnNoValue() {
        TransactionManager.current().transactionCache[table].flushInserts()
    }

}
