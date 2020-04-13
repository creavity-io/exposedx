package io.creavity.exposedx.dao.manager

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.FlushAction
import io.creavity.exposedx.dao.queryset.EntityQuery
import io.creavity.exposedx.dao.queryset.EntityQueryBase
import io.creavity.exposedx.dao.signals.EntityChangeType
import io.creavity.exposedx.dao.signals.registerChange
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transactionScope

internal val Transaction.transactionCache: TransactionCache by transactionScope { TransactionCache(this) }

fun Transaction.flushCache() = transactionCache.flush()

internal class TransactionTableCache<ID : Comparable<ID>, T: IdTable<ID>>(val table: T, val transaction: Transaction) {
    private val data: MutableMap<ID, MutableResultRow<ID>> = linkedMapOf()

    private val inserts: MutableSet<MutableResultRow<ID>> = mutableSetOf()

    fun find(id: EntityID<ID>): MutableResultRow<ID>? = data[id.value] ?: inserts.firstOrNull { it.id._value == id._value }

    fun all() = data.values + inserts

    fun remove(o: MutableResultRow<ID>) = data.remove(o.id.value)

    fun remove(id: EntityID<ID>) = data.remove(id.value)

    fun store(row: MutableResultRow<ID>) {
        data.compute(row.id.value) { _, current ->
            if (current != null && current != row) error("Collision error: There are another resultrow with the same id in cache.")
            else row
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun scheduleSave(row: MutableResultRow<ID>) {
        row.markForSave(transaction.db)
        when (row.flushAction) {
            FlushAction.UPDATE -> store(row) // if detached, add to store
            FlushAction.INSERT -> inserts.add(row)
            else -> Unit
        }
    }

    private var flushingInserts by transactionScope { false }

    fun flushInserts() {
        if (flushingInserts) return
        flushingInserts = true
        try {
            var toFlush = this.inserts.toList()

            while (toFlush.isNotEmpty()) {
                // if is recursive, insert first tables that doesnt depends of other registries
                val (flushNow, nextFlush) = toFlush.partition { !it.isSelfRerencing() }
                batchFlushInsert(flushNow)
                toFlush = nextFlush
            }
            inserts.clear()
        } finally {
            flushingInserts = false
        }
    }

    private var flushingUpdates by transactionScope { false }

    fun flushUpdates() {
     //   if (flushingUpdates) return
        flushingUpdates = true
        try {
            val batch = EntityBatchUpdate(table)
            val updatedEntities = data.values.filter { batchFlushUpdate(it, batch) }
            if (updatedEntities.isNotEmpty()) {
                batch.execute(transaction)
            }
            updatedEntities.forEach {
                transaction.registerChange(it.table, it.id, EntityChangeType.Updated)
            }
        } finally {
            flushingUpdates = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun batchFlushInsert(toFlush: List<MutableResultRow<ID>>) {
        val ids = table.batchInsert(toFlush) { entry ->
            entry.save { column, value ->
                this[column] = value
            }
        }

        for ((entry, genValues) in toFlush.zip(ids)) {
            if (entry.id._value == null) {
                val id = genValues[table.id]
                entry.id._value = id._value
                entry.readValues[entry.table.id as Column<Any?>] = id
            }

            genValues.fieldIndex.keys.forEach { key ->
                entry.readValues[key as Column<Any?>] = genValues[key]
            }
            data.put(entry.id.value, entry)
            transaction.registerChange(entry.table, entry.id, EntityChangeType.Created)
        }
    }

    private fun batchFlushUpdate(row: MutableResultRow<ID>, batch: EntityBatchUpdate<ID>): Boolean {
        if (row.flushAction != FlushAction.UPDATE) {
            return false
        }
        batch.addBatch(row.id)
        row.save { column, value ->
            batch[column] = value
        }
        return true
    }

}


/**
 * EntityCache is a cache only for transactions, the data is saved here for make batch inserts, batch updates, and querys with reffers
 * For Query cache, another cache have to be implemented, in a superior layer independent of Transaction
 */
@Suppress("UNCHECKED_CAST")
internal class TransactionCache(private val transaction: Transaction) {

    private val tableCaches: MutableMap<IdTable<*>, TransactionTableCache<*, *>> = linkedMapOf()

    internal operator fun <ID: Comparable<ID>, T: IdTable<ID>> get(table: T) = tableCaches.getOrPut(table) {
        TransactionTableCache(table, transaction)
    } as TransactionTableCache<ID, T>

    fun flush() = flush(tableCaches.keys)


    private var flushingInsert by transactionScope { false }

    fun flush(tables: Iterable<IdTable<*>>) {
        if (flushingInsert) return
        flushingInsert = true
        try {
            tables.forEach {
                this[it as IdTable<Comparable<Any>>].apply {
                    flushInserts()
                    flushUpdates()
                }
            }
        }finally {
            flushingInsert = false
        }


        /*
        val allTables = SchemaUtils.sortTablesByReferences(tableCaches.keys).filterIsInstance<IdTable<*>>()

        val idx = allTables.indexOfLast { it in tables } // idx of last table related with all tables.

        // avoiding flush tables that doesnt need yet
        allTables.subList(0, idx + 1).forEach {
            this[it].flush()
        }
         */
    }

    fun <ID: Comparable<ID>> scheduleSave(table: IdTable<ID>, row: MutableResultRow<ID>) = this[table].scheduleSave(row)

    fun clear() = tableCaches.clear()


}