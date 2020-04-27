package io.creavity.exposedx.dao.queryset

import io.creavity.exposedx.dao.entities.Entity
import org.jetbrains.exposed.dao.id.EntityID
import io.creavity.exposedx.dao.entities.identifiers.DaoEntityID
import io.creavity.exposedx.dao.manager.EntityManager
import io.creavity.exposedx.dao.manager.CopiableObject
import io.creavity.exposedx.dao.manager.wrapRow
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Exception
import kotlin.sequences.Sequence

internal fun <T> localTransaction(statement: Transaction.() -> T): T {
    if (TransactionManager.isInitialized()) {
        val current = TransactionManager.currentOrNull()
        if (current != null) {
            return current.statement()
        }
    }
    return transaction(null) {
        this.statement()
    }
}

interface EntityQuery<ID : Comparable<ID>, E : Entity<ID>, T : EntityManager<ID, E, *>> {
    val rawQuery: Query
    operator fun iterator(): Iterator<E>
    fun filter(op: Op<Boolean>): EntityQuery<ID, E, T>
    fun exclude(op: Op<Boolean>): EntityQuery<ID, E, T>
    fun filter(vararg where: T.() -> Op<Boolean>): EntityQuery<ID, E, T>
    fun exclude(vararg where: T.() -> Op<Boolean>): EntityQuery<ID, E, T>
    fun filter(where: T.() -> Op<Boolean>): EntityQuery<ID, E, T>
    fun exclude(where: T.() -> Op<Boolean>): EntityQuery<ID, E, T>
    fun orderBy(column: Expression<*>, order: SortOrder = SortOrder.ASC): EntityQuery<ID, E, T>
    fun orderBy(vararg sort: String): EntityQuery<ID, E, T>
    fun filter(ids: List<ID>): EntityQuery<ID, E, T>
    fun filterByEntityIds(ids: List<EntityID<ID>>): EntityQuery<ID, E, T>
    fun get(id: EntityID<ID>): E?
    fun get(id: ID): E?
    fun groupBy(vararg columns: Expression<*>): Query
    fun having(op: SqlExpressionBuilder.() -> Op<Boolean>): Query
    fun delete(): Int
    fun limit(size: Int, offset: Long): EntityQuery<ID, E, T>
    fun forUpdate(): EntityQuery<ID, E, T> = this
    fun copy(): EntityQuery<ID, E, T>
    fun orderBy(vararg order: Pair<Expression<*>, SortOrder>): EntityQuery<ID, E, T>
    fun all(): EntitySizedIterable<ID, E>
    fun empty(): Boolean
    fun count(): Long
    fun update(body: T.(UpdateStatement) -> Unit): Int
    fun forUpdate(updateFn: (E) -> Unit)
    fun selectRelated(vararg tables: EntityManager<*, *, *>): EntityQuery<ID, E, T>
    fun prefetchRelated(vararg tables: EntityManager<*, *, *>): EntityQuery<ID, E, T>
}

// puede cambiairse a inline class
class EntitySizedIterable<ID : Comparable<ID>, E : Entity<ID>> constructor(val queryBase: EntityQuery<ID, E, *>) : SizedIterable<E> {
    override fun limit(n: Int, offset: Long) = EntitySizedIterable(queryBase.limit(n, offset))

    override fun count() = queryBase.count()

    override fun empty() = queryBase.empty()

    override fun copy() = EntitySizedIterable(queryBase.copy())

    override fun orderBy(vararg order: Pair<Expression<*>, SortOrder>) = EntitySizedIterable(queryBase.orderBy(*order))

    override fun iterator() = queryBase.iterator()

    override fun forUpdate(): SizedIterable<E> {
        return queryBase.forUpdate().all()
    }
}

open class EntityQueryBase<ID : Comparable<ID>, E : Entity<ID>, T : EntityManager<ID, E, *>>(val entityManager: T,
                                                                                             override val rawQuery: Query) : EntityQuery<ID, E, T> {

    private val selectRelatedTables = mutableSetOf<EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>>()
    private val prefetchRelatedTables = mutableSetOf<EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>>()

    override fun limit(size: Int, offset: Long): EntityQuery<ID, E, T> = entityQuery.apply { rawQuery.limit(size, offset) }

    override fun count() = localTransaction { rawQuery.notForUpdate().count() }

    override fun empty() = localTransaction { rawQuery.empty() }

    override fun orderBy(column: Expression<*>, order: SortOrder): EntityQuery<ID, E, T> = orderBy(column to order)

    override fun orderBy(vararg sort: String): EntityQuery<ID, E, T> {
        return orderBy(*sort.map {
            var column = it
            var order = SortOrder.ASC
            if (it.startsWith("-")) {
                column = it.substringAfter("-")
                order = SortOrder.DESC
            }
            val columnExpression = entityManager.columns.filter { it.name.equals(column) }.first()
            columnExpression to order
        }.toTypedArray())
    }

    override fun orderBy(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") vararg columns: Pair<Expression<*>, SortOrder>) = entityQuery.apply { rawQuery.orderBy(*columns) }

    private var elements: List<E>? = null

    override fun iterator() = elements?.iterator() ?: fetchElements()

    /*
    * Get every direct column that we have to prefetch.
    * */
    private fun getColumnsToPrefetch(): LinkedHashMap<Column<*>, MutableList<EntityID<Comparable<Any>>>> {
        val relatedcolumnsToFetch = linkedMapOf<Column<*>, MutableList<EntityID<Comparable<Any>>>>()
        prefetchRelatedTables.forEach {
            // todo: refactor, move this to other place.
            var table = it
            while (table.relatedColumnId != null) {
                if (table.relatedColumnId!!.table == this@EntityQueryBase.entityManager) { // only if this table can handle it
                    relatedcolumnsToFetch.getOrPut(table.relatedColumnId!!) { mutableListOf() }
                    break
                } else {
                    table = table.relatedColumnId!!.table as EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>
                }
            }
        }
        return relatedcolumnsToFetch
    }


    private fun joinWithSelectRelated() {
        selectRelatedTables.forEach {
            // todo: refactor, move this to other place.
            var table = it

            while (table.relatedColumnId != null) {
                val leftTable: Table = (table.relatedColumnId?.table as EntityManager<*, *, *>?)?.aliasRelated
                        ?: this.entityManager
                val newJoin = joinWith(leftTable, table.aliasRelated!!, table.relatedColumnId!!)
                rawQuery.adjustColumnSet { newJoin() }
                rawQuery.adjustSlice { this.slice(table.aliasRelated!!.columns + rawQuery.set.fields) }
                table = table.relatedColumnId!!.table as EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>
            }
        }
    }

    private fun fetchElements(): Iterator<E> = localTransaction {
        joinWithSelectRelated()

        val relatedcolumnsToFetch = getColumnsToPrefetch()

        val results = execQuery().map { row ->
            relatedcolumnsToFetch.forEach { column, list -> list.add(row[column] as EntityID<Comparable<Any>>) } // prefetch related.
            selectRelatedTables.forEach {
                // todo: refactor, move this to other place.
                var table = it
                while (table.relatedColumnId != null) {
                    table.wrapRow(row, table.aliasRelated!!, rawQuery.isForUpdate())
                    table = table.relatedColumnId!!.table as EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>
                }
            }
            entityManager.wrapRow(row, rawQuery.isForUpdate())
        }.toList()// toList() execute

        relatedcolumnsToFetch.forEach { column, list ->
            val table = column.referee!!.table as EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>

            // pass prefetch related to the child, if it can handle it will take and prefetch it.
            table.objects.filterByEntityIds(list).prefetchRelated(*prefetchRelatedTables.toTypedArray()).all()
        }

        elements = results.iterator().asSequence().toList()
        elements!!.iterator()
    }

    private fun execQuery(): Sequence<ResultRow> = rawQuery.asSequence()

    override fun all(): EntitySizedIterable<ID, E> = fetchElements().let { EntitySizedIterable(this) }

    override fun forUpdate(): EntityQuery<ID, E, T> = entityQuery.apply { rawQuery.forUpdate() }

    override fun update(body: T.(UpdateStatement) -> Unit) = localTransaction {
        rawQuery.where?.let {
            entityManager.update({ it }, body = { entityManager.body(it) })
        } ?: entityManager.update(body = { entityManager.body(it) })
    }

    override fun forUpdate(updateFn: (E) -> Unit) = localTransaction {
        forUpdate().all().forEach(updateFn)
    }

    override fun filter(op: Op<Boolean>): EntityQuery<ID, E, T> = entityQuery.apply { rawQuery.adjustWhere { op } }

    override fun exclude(op: Op<Boolean>): EntityQuery<ID, E, T> = entityQuery.apply { rawQuery.adjustWhere { not(op) } }

    override fun filter(where: T.() -> Op<Boolean>): EntityQuery<ID, E, T> = entityQuery.apply {
        // T es de tipo Table
        val manager = (entityManager as CopiableObject<*>).copy() as T
        val exp = manager.where()
        manager.relatedJoin.forEach { _, joinFn ->
            rawQuery.adjustColumnSet { joinFn(this) }
        }
        rawQuery.adjustWhere { exp }
    }

    override fun exclude(where: T.() -> Op<Boolean>): EntityQuery<ID, E, T> = entityQuery.apply {
        val manager = (entityManager as CopiableObject<*>).copy() as T
        val exp = manager.where()
        manager.relatedJoin.forEach { _, joinFn ->
            rawQuery.adjustColumnSet { joinFn(this) }
        }
        rawQuery.adjustWhere { not(exp) }
    }

    override fun filter(vararg where: T.() -> Op<Boolean>): EntityQuery<ID, E, T> = entityQuery.apply {
        where.forEach { filter(it) }
    }

    override fun exclude(vararg where: T.() -> Op<Boolean>): EntityQuery<ID, E, T> = entityQuery.apply {
        where.forEach { exclude(it) }
    }

    override fun selectRelated(vararg tables: EntityManager<*, *, *>): EntityQuery<ID, E, T> = entityQuery.apply {
        this.selectRelatedTables += tables.asList() as Collection<EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>>
    }

    override fun prefetchRelated(vararg tables: EntityManager<*, *, *>): EntityQuery<ID, E, T> = entityQuery.apply {
        this.prefetchRelatedTables += tables.asList() as Collection<EntityManager<Comparable<Any>, Entity<Comparable<Any>>, *>>
    }

    override fun filterByEntityIds(ids: List<EntityID<ID>>): EntityQuery<ID, E, T> = filter { entityManager.originalId inList ids }
    override fun filter(ids: List<ID>): EntityQuery<ID, E, T> = filterByEntityIds(ids.map { DaoEntityID(it, entityManager) })
    override fun get(id: EntityID<ID>): E? = localTransaction { filterByEntityIds(listOf(id)).all().firstOrNull() }
    override fun get(id: ID): E? = localTransaction { filter(listOf(id)).all().firstOrNull() }

    override fun delete(): Int = localTransaction {
        if (!rawQuery.groupedByColumns.isEmpty() || rawQuery.having != null) throw Exception("Cant delete with group by or having.")
        // todo: remove from cache
        if (rawQuery.where == null) {
            this@EntityQueryBase.entityManager.deleteAll()
        } else {
            this@EntityQueryBase.entityManager.deleteWhere(rawQuery.limit, rawQuery.offset) { rawQuery.where!! }
        }
    }

    override fun having(op: SqlExpressionBuilder.() -> Op<Boolean>) = rawQuery.having(op)

    override fun groupBy(vararg columns: Expression<*>) = rawQuery.groupBy(*columns)

    override fun copy(): EntityQueryBase<ID, E, T> =
            selfConstructor.newInstance(entityManager, rawQuery.copy()).also {
                it.selectRelatedTables.addAll(this.selectRelatedTables)
                it.prefetchRelatedTables.addAll(this.prefetchRelatedTables)
            }


    private val selfConstructor by lazy {
        try {
            this.javaClass.getDeclaredConstructor(EntityManager::class.java, Query::class.java).also { constructor ->
                constructor.isAccessible = true
            }
        } catch (ex: NoSuchMethodException) {
            error("EntityQuery need a constructor with entitymanager and query parameters.")
        }
    }

    /**
     * Cada vez que se ejecuta un filtro se copia y devuelve un nuevo objeto, si no copiara el objeto, estas querys se unirían.
     *
     * query = users.objects.filter { isDeleted eq True }
     * users.filter { name like "A%" }
     * users.filter { name like "B%" } // al modificar la misma query devolverían name = "A" and name = "B"
     */
    private val entityQuery: EntityQueryBase<ID, E, T> get() = copy()
}


fun <ID : Comparable<ID>, E : Entity<ID>, T : EntityManager<ID, E, T>> EntityQuery<ID, E, T>.first() = this.all().first()
fun <ID : Comparable<ID>, E : Entity<ID>, T : EntityManager<ID, E, T>> EntityQuery<ID, E, T>.firstOrNull() = this.all().firstOrNull()
fun <ID : Comparable<ID>, E : Entity<ID>, T : EntityManager<ID, E, T>> EntityQuery<ID, E, T>.last() = this.all().last()