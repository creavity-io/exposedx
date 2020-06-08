package io.creavity.exposedx.dao.entities

import io.creavity.exposedx.dao.tables.EntityTable
import io.creavity.exposedx.dao.queryset.joinWithParent
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import kotlin.reflect.KProperty

/**
 * Intercept column requests for add joins to the query.
 */
@Suppress("UNCHECKED_CAST")
operator fun <T: Column<*>> T.getValue(rightTable: EntityTable<*, *, *>, property: KProperty<*>): T  {
    return rightTable.relatedColumnId?.let { relatedColumn ->
        if(relatedColumn.referee.toString() == this.toString()) { return relatedColumn as T } // si es el id.
        rightTable.joinWithParent()
        return rightTable.aliasRelated!!.get(this) as T
    } ?: this
}


class ManyToOneRelationRef<ID : Comparable<ID>, E : Entity<ID>, M : EntityTable<ID, E, M>>(from: EntityTable<*,*,*>,
                                                                                           name: String,
                                                                                           val foreign: M,
                                                                                           onDelete: ReferenceOption? = null,
                                                                                           onUpdate: ReferenceOption? = null,
                                                                                           fkName: String? = null) {
    val column: Column<Any> = from.reference(name, foreign, onDelete, onUpdate, fkName) as Column<Any>

    private val foreignTable by lazy {
        foreign.copy().also {
            it.asRelatedTable(column)
        }
    }

    operator fun <ID : Comparable<ID>, E : Entity<ID>,
            M2 : EntityTable<ID, E, M2>>
            getValue(table: EntityTable<ID, E, M2>, property: KProperty<*>): M {
        return foreignTable
    }
}

class ManyToOptionalRelationRef<ID : Comparable<ID>, E : Entity<ID>, M : EntityTable<ID, E, M>>(from: EntityTable<*,*,*>,
                                                                                                name: String,
                                                                                                val foreign: M,
                                                                                                onDelete: ReferenceOption? = null,
                                                                                                onUpdate: ReferenceOption? = null,
                                                                                                fkName: String? = null) {

    val column: Column<Any> = from.optReference(name, foreign, onDelete, onUpdate, fkName) as Column<Any>

    private val foreignTable by lazy {
        foreign.copy().also {
            it.asRelatedTable(column)
        }
    }

    operator fun <ID : Comparable<ID>, E : Entity<ID>,
            M2 : EntityTable<ID, E, M2>>
            getValue(table: EntityTable<ID, E, M2>, property: KProperty<*>): M {
        return foreignTable
    }
}



@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityTable<ID, E, RelatedTable>> EntityTable<*, *, *>.manyToOne(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOneRelationRef<ID, E, RelatedTable> {
    return ManyToOneRelationRef(this, name, foreign, onDelete, onUpdate, fkName)
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityTable<ID, E, RelatedTable>> EntityTable<*, *, *>.oneToOne(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOneRelationRef<ID, E, RelatedTable> {
    return ManyToOneRelationRef(this, name, foreign, onDelete, onUpdate, fkName)
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityTable<ID, E, RelatedTable>> EntityTable<*, *, *>.manyToOptional(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOptionalRelationRef<ID, E, RelatedTable> {
    return ManyToOptionalRelationRef(this, name, foreign, onDelete, onUpdate, fkName)
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityTable<ID, E, RelatedTable>> EntityTable<*, *, *>.oneToOptional(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOptionalRelationRef<ID, E, RelatedTable> {
    return ManyToOptionalRelationRef(this, name, foreign, onDelete, onUpdate, fkName)
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityTable<ID, E, RelatedTable>> RelatedTable.manyToOptional(
        name: String,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOptionalRelationRef<ID, E, RelatedTable> {
    return ManyToOptionalRelationRef(this, name, this, onDelete, onUpdate, fkName)
}

@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityTable<ID, E, RelatedTable>> RelatedTable.oneToOptional(
        name: String,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOptionalRelationRef<ID, E, RelatedTable> {
    return ManyToOptionalRelationRef(this, name, this, onDelete, onUpdate, fkName)
}


class NullableRelation<ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>>(val table: EntityTable<ID, E, M>)
fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>> M.nullable() = NullableRelation(this)


operator fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>> M.getValue(rightTable: EntityTable<*, *, *>, property: KProperty<*>): M {
    return this
}