package io.creavity.exposedx.dao.tables

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.BatchUpdateStatement
import java.util.*

class EntityBatchUpdate<ID: Comparable<ID>>(val table: IdTable<ID>) {

    private val data = ArrayList<Pair<EntityID<ID>, SortedMap<Column<*>, Any?>>>()

    fun addBatch(id: EntityID<ID>) {
        if (id.table != table) error("Table from Entity ID ${id.table.tableName} differs from entity class ${table.tableName}")
        data.add(id to TreeMap())
    }

    operator fun set(column: Column<*>, value: Any?) {
        val values = data.last().second

        if (values.containsKey(column)) {
            error("$column is already initialized")
        }

        values[column] = value
    }

    fun execute(transaction: Transaction): Int {
        val updateSets = data.filterNot {it.second.isEmpty()}.groupBy { it.second.keys }
        return updateSets.values.fold(0) { acc, set ->
            acc + BatchUpdateStatement(table).let {
                it.data.addAll(set)
                it.execute(transaction)!!
            }
        }
    }
}
