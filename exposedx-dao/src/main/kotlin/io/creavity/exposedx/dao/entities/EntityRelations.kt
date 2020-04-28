package io.creavity.exposedx.dao.entities

import org.jetbrains.exposed.dao.id.EntityID
import io.creavity.exposedx.dao.manager.EntityManager
import io.creavity.exposedx.dao.manager.OneToManyQuery
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


class ManyToOneRelationRef<ID : Comparable<ID>, E : Entity<ID>, M : EntityManager<ID, E, M>>(val from: EntityManager<*,*,*>,
                                                                                                  val name: String,
                                                                                                  val foreign: M,
                                                                                                  val onDelete: ReferenceOption? = null,
                                                                                                  val onUpdate: ReferenceOption? = null,
                                                                                                  val fkName: String? = null) {
    val column: Column<Any> = from.reference(name, foreign, onDelete, onUpdate, fkName) as Column<Any>


    private val foreignTable by lazy {
        foreign.copy().also {
            it.asRelatedTable(column)
        }
    }
    operator fun <ID : Comparable<ID>, E : Entity<ID>,
            M2 : EntityManager<ID, E, M2>>
            getValue(table: EntityManager<ID, E, M2>, property: KProperty<*>): M {
        return foreignTable
    }
}

class ManyToOptionalRelationRef<ID : Comparable<ID>, E : Entity<ID>, M : EntityManager<ID, E, M>>(val from: EntityManager<*,*,*>,
                                                                                             val name: String,
                                                                                             val foreign: M,
                                                                                             val onDelete: ReferenceOption? = null,
                                                                                             val onUpdate: ReferenceOption? = null,
                                                                                             val fkName: String? = null) {

    val column: Column<Any> = from.optReference(name, foreign, onDelete, onUpdate, fkName) as Column<Any>

    private val foreignTable by lazy {
        foreign.copy().also {
            it.asRelatedTable(column)
        }
    }

    operator fun <ID : Comparable<ID>, E : Entity<ID>,
            M2 : EntityManager<ID, E, M2>>
            getValue(table: EntityManager<ID, E, M2>, property: KProperty<*>): M {
        return foreignTable
    }
}



@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityManager<ID, E, RelatedTable>> EntityManager<*, *, *>.manyToOne(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOneRelationRef<ID, E, RelatedTable> {
    return ManyToOneRelationRef(this, name, foreign, onDelete, onUpdate, fkName)
}


@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityManager<ID, E, RelatedTable>> EntityManager<*, *, *>.manyToOptional(
        name: String,
        foreign: RelatedTable,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOptionalRelationRef<ID, E, RelatedTable> {
    return ManyToOptionalRelationRef(this, name, foreign, onDelete, onUpdate, fkName)
}


@Suppress("UNCHECKED_CAST")
fun <ID: Comparable<ID>, E: Entity<ID>, RelatedTable: EntityManager<ID, E, RelatedTable>> RelatedTable.selfRelation(
        name: String,
        onDelete: ReferenceOption? = null,
        onUpdate: ReferenceOption? = null,
        fkName: String? = null
): ManyToOptionalRelationRef<ID, E, RelatedTable> {
    return ManyToOptionalRelationRef(this, name, this, onDelete, onUpdate, fkName)
}

class NullableRelation<ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>>(val manager: EntityManager<ID, E, M>)

fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>> M.nullable() = NullableRelation(this)


operator fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>> M.getValue(rightTable: EntityManager<*, *, *>, property: KProperty<*>): M {
    return this
}