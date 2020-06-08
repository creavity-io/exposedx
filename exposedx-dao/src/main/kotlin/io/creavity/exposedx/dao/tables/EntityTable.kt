package io.creavity.exposedx.dao.tables

import org.jetbrains.exposed.dao.id.EntityID
import io.creavity.exposedx.dao.entities.Entity
import org.jetbrains.exposed.dao.id.IdTable
import io.creavity.exposedx.dao.queryset.EntityQuery
import io.creavity.exposedx.dao.queryset.EntityQueryBase
import io.creavity.exposedx.dao.queryset.localTransaction
import io.creavity.exposedx.dao.signals.EntityChangeType
import io.creavity.exposedx.dao.signals.registerChange
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager


open class DefaultEntityManager<ID : Comparable<ID>, E : Entity<ID>, M : EntityTable<ID, E, M>> (protected open val entityTable: EntityTable<ID, E, M>) {

    open fun save(prototype: E, columns: Array<out Column<*>>): E = entityTable.dbSave(prototype, columns)

    open fun delete(id: EntityID<ID>) = entityTable.dbDelete(id)

    open fun buildQuery(query: Query) = query

    open fun buildEntityQuery(query: Query): EntityQuery<ID, E, M>  = EntityQueryBase(entityTable as M, query)

    open fun createColumns() {}
}


@Suppress("UNCHECKED_CAST")
abstract class EntityTable<ID : Comparable<ID>, E : Entity<ID>, M : EntityTable<ID, E, M>>(name: String = "") : IdTable<ID>(), ISqlExpressionBuilder, CopiableObject<M> {
    internal val originalId: Column<EntityID<ID>> get() = this.id.referee() ?: this.id

    private val klass = this.javaClass.enclosingClass

    override var tableName = name.ifBlank { klass.simpleName }

    private val ctor = klass.constructors.first()

    open val manager = DefaultEntityManager(this)

    override val columns: List<Column<*>> by lazy {
        manager.createColumns()
        super.columns
    }

    val objects: EntityQuery<ID, E, M> by lazy { this.buildEntityQuery() }

    internal var relatedColumnId: Column<Any>? = null

    internal var relatedJoin: MutableMap<Table, JoinFunction> = mutableMapOf() // se utiliza para hacer los joins

    internal val aliasRelated by lazy {
        // tener en cuenta que si se llama al alias related antes de setear el related column id, este se quedara como null
        relatedColumnId?.let {
            Alias(this, "${it.name}_${this.tableName}")
        }
    }

    internal var _parent: EntityTable<*, *, *>? = null


    internal val _cache get() = TransactionManager.current().transactionCache

    private val defaultQuery get() = this.selectAll()

    internal open fun createInstance() = ctor.newInstance() as E

    internal fun asRelatedTable(column: Column<Any>, parent: EntityTable<*,*,*> = column.table as EntityTable<*,*,*>) {
        this.relatedColumnId = column
        this._parent = parent
    }

    fun buildQuery(rawQuery: Query? = null) = manager.buildQuery(rawQuery ?: this.defaultQuery).copy()

    fun buildEntityQuery(rawQuery: Query? = null): EntityQuery<ID, E, M> = manager.buildEntityQuery(buildQuery())

    fun findResultRowById(id: EntityID<ID>): ResultRow? = localTransaction { buildQuery().adjustWhere { this@EntityTable.originalId eq id }.firstOrNull() }

    fun save(prototype: E, columns: Array<out Column<*>>): E = manager.save(prototype, columns)

    fun delete(id: EntityID<ID>) = manager.delete(id)

    open fun dbSave(prototype: E, columns: Array<out Column<*>>): E = prototype.also {
        localTransaction {
            _cache.scheduleSave(this@EntityTable, prototype, columns)
        }
    }

    open fun dbDelete(id: EntityID<ID>) = localTransaction {
        this@EntityTable.deleteWhere { this@EntityTable.id eq id }
        _cache[this@EntityTable].remove(id)
        TransactionManager.current().registerChange(this@EntityTable, id, EntityChangeType.Removed)
    }

    infix fun  Expression<String?>.contains(obj: String) = this.upperCase() like obj.toUpperCase()


    infix fun eq(application: E) = this.id eq application.id
    fun isNull() = this.id.isNull()
    fun isNotNull() = this.id.isNotNull()
    infix fun inList(list: List<E>) = this.id.inList(list.map { it.id })

    fun flush() {
        if (transactionExist) _cache.flush(listOf(this))
    }
    
}


// TODO deprecate entity manager por entity table
@Deprecated("Use EntityTable instead")
abstract class EntityManager<ID : Comparable<ID>, E : Entity<ID>, M : EntityManager<ID, E, M>>(name: String = "") : EntityTable<ID, E, M>()

