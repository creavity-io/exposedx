package io.creavity.exposedx.dao.entities

import org.jetbrains.exposed.dao.id.EntityID
import io.creavity.exposedx.dao.manager.EntityManager
import io.creavity.exposedx.dao.queryset.joinWithParent
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import kotlin.reflect.KProperty

/**
 * Intercept column requests for add joins to the query.
 */
@Suppress("UNCHECKED_CAST")
operator fun <T: Column<*>> T.getValue(rightTable: EntityManager<*, *, *>, property: KProperty<*>): T  {
    return rightTable.relatedColumnId?.let { relatedColumn ->
        if(relatedColumn.referee.toString() == this.toString()) { return relatedColumn as T } // si es el id.
        rightTable.joinWithParent()
        return rightTable.aliasRelated!!.get(this) as T
    } ?: this
}


@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityManager<ID, E, RelatedTable>> EntityManager<*, *, *>.manyToOne(
        foreign: RelatedTable,
        reference: () -> Column<EntityID<ID>>
): RelatedTable {
    return foreign.copy().also {
        it.asRelatedTable(reference() as Column<Any>)
    }
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityManager<ID, E, RelatedTable>> EntityManager<*, *, *>.manyToOne(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null,
        extraFn: ((Column<EntityID<ID>>).() -> Column<EntityID<ID>>)? = null
): RelatedTable {
    return foreign.copy().also {
        val column = reference(name, it, onDelete, onUpdate, fkName).apply {
            extraFn?.let { this.it() }
        }
        it.asRelatedTable(column as Column<Any>)
    }
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityManager<ID, E, RelatedTable>> EntityManager<*, *, *>.manyToOptional(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null,
        extraFn: ((Column<EntityID<ID>?>).() -> Column<EntityID<ID>?>)? = null
): RelatedTable {
    return foreign.copy().also {
        val column = optReference(name, it, onDelete, onUpdate, fkName).apply {
            extraFn?.let { this.it() }
        }
        it.asRelatedTable(column as Column<Any>)
    }
}



class NullableRelation<ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>>(val manager: EntityManager<ID, E, M>)

fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>> M.nullable() = NullableRelation(this)


operator fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>> M.getValue(rightTable: EntityManager<*, *, *>, property: KProperty<*>): M {
    return this
}