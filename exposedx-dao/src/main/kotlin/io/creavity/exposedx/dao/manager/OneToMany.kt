package io.creavity.exposedx.dao.manager

import org.jetbrains.exposed.dao.id.EntityID
import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.queryset.EntityQueryBase
import io.creavity.exposedx.dao.queryset.joinWithParent
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import kotlin.reflect.KProperty

class OneToManyQuery<ID : Comparable<ID>, E : Entity<ID>, T : EntityManager<ID, E, T>>(entityManager: T, rawQuery: Query)
    : EntityQueryBase<ID, E, T>(entityManager, rawQuery), Iterable<E>

class OneToManyRelationRef<ID : Comparable<ID>, E : Entity<ID>, M : EntityManager<ID, E, M>>(val ref: M, val column: Column<Any>) {
    /*
    * Copy a table for use in querys.
    * */
    operator fun <ID : Comparable<ID>, E : Entity<ID>,
            M2 : EntityManager<ID, E, M2>>
            getValue(table: EntityManager<ID, E, M2>, property: KProperty<*>): M {
        return ref.copy().also {
            it.asRelatedTable(column, table)
            it.joinWithParent()
        }
    }
}


class OneToManyRelation<ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>>(val column: Column<EntityID<ID>>) {
    operator fun getValue(entity: Entity<ID>, property: KProperty<*>): OneToManyQuery<ID, E, M> {
        return entity.getOneToMany(column)
    }
}

fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>,
        ID2: Comparable<ID2>, E2: Entity<ID2>, M2: EntityManager<ID2, E2, M2>> oneToManyRef(joinableTable: M, refTable: M2): OneToManyRelationRef<ID2, E2, M2> {
    return OneToManyRelationRef(refTable, joinableTable.relatedColumnId!!)
}

fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityManager<ID, E, M>> M.oneToMany(): OneToManyRelation<ID, E, M> {
    return OneToManyRelation(relatedColumnId!! as Column<EntityID<ID>>)
}
