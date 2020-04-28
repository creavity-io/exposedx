package io.creavity.exposedx.dao.manager

import io.creavity.exposedx.dao.entities.Entity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import io.creavity.exposedx.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.sql.*


fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrapRows(rows: SizedIterable<ResultRow>, isForUpdate: Boolean = false): SizedIterable<E> = rows mapLazy {
    wrapRow(it, isForUpdate)
}

fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrapRows(rows: SizedIterable<ResultRow>, alias: Alias<IdTable<*>>, isForUpdate: Boolean = false) = rows mapLazy {
    wrapRow(it, alias, isForUpdate)
}

fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrapRows(rows: SizedIterable<ResultRow>, alias: QueryAlias, isForUpdate: Boolean = false) = rows mapLazy {
    wrapRow(it, alias, isForUpdate)
}

fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrapRow(row: ResultRow, alias: Alias<IdTable<*>>, isForUpdate: Boolean = false): E {
    require(alias.delegate == this) { "Alias for a wrong table ${alias.delegate.tableName} while ${this.tableName} expected" }
    val newFieldsMapping = row.fieldIndex.keys.mapNotNull { exp ->
        val column = exp as? Column<*>
        val value = row[exp]
        val originalColumn = column?.let { alias.originalColumn(it) }
        when {
            originalColumn != null -> originalColumn to value
            column?.table == alias.delegate -> null
            else -> exp to value
        }
    }.toMap()
    return wrapRow(ResultRow.createAndFillValues(newFieldsMapping), isForUpdate)
}

fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrapRow(row: ResultRow, alias: QueryAlias, isForUpdate: Boolean = false): E {
    require(alias.columns.any { (it.table as Alias<*>).delegate == this }) { "QueryAlias doesn't have any column from ${this.tableName} table" }
    val originalColumns = alias.query.set.source.columns
    val newFieldsMapping = row.fieldIndex.keys.mapNotNull { exp ->
        val value = row[exp]
        when {
            exp is Column && exp.table is Alias<*> -> {
                val delegate = (exp.table as Alias<*>).delegate
                val column = originalColumns.single {
                    delegate == it.table && exp.name == it.name
                }
                column to value
            }
            exp is Column && exp.table == this -> null
            else -> exp to value
        }
    }.toMap()
    return wrapRow(ResultRow.createAndFillValues(newFieldsMapping), isForUpdate)
}

@Suppress("MemberVisibilityCanBePrivate")
fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrapRow(row: ResultRow, isForUpdate: Boolean = false): E = wrap(row, isForUpdate)

@Suppress("UNCHECKED_CAST")
internal fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.wrap(row: ResultRow, isForUpdate: Boolean = false): E {
    val entityId = row[this.originalId]
    val found = _cache[this].find(entityId)?.apply { readValues = row }
    if (found != null) return found as E

    return createInstance().also {
        it.init(_cache[this].transaction.db, entityId, row, isForUpdate)
        _cache[this].store(it)
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.lazyWrap(id: EntityID<ID>, db: Database): E {
    if(transactionExist) {
        val found = _cache[this].find(id)
        if (found != null) return found as E
    }

    return createInstance().also {
        it.init(db, id) {
            this@lazyWrap.findResultRowById(id) ?: throw EntityNotFoundException(id, this@lazyWrap)
        }
        if(transactionExist) _cache[this].store(it)
    }
}