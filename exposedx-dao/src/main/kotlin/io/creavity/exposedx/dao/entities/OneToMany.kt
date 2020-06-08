package io.creavity.exposedx.dao.entities

import org.jetbrains.exposed.dao.id.EntityID
import io.creavity.exposedx.dao.queryset.EntityQueryBase
import io.creavity.exposedx.dao.queryset.joinWithParent
import io.creavity.exposedx.dao.tables.EntityTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import kotlin.reflect.KProperty

class OneToManyQuery<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>>(entityTable: T, rawQuery: Query)
    : EntityQueryBase<ID, E, T>(entityTable, rawQuery), Iterable<E>

class OneToManyRelationRef<ID : Comparable<ID>, E : Entity<ID>, M : EntityTable<ID, E, M>>(val ref: M, val column: Column<Any>) {
    /*
    * Copy a table for use in querys.
    * */
    operator fun <ID : Comparable<ID>, E : Entity<ID>,
            M2 : EntityTable<ID, E, M2>>
            getValue(table: EntityTable<ID, E, M2>, property: KProperty<*>): M {
        return ref.copy().also {
            it.asRelatedTable(column, table)
            it.joinWithParent()
        }
    }
}


class OneToManyRelation<ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>>(val column: Column<EntityID<ID>>) {
    operator fun getValue(entity: Entity<ID>, property: KProperty<*>): OneToManyQuery<ID, E, M> {
        return entity.getOneToMany(column)
    }
}
class OneToOneRelation<ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>>(val column: Column<EntityID<ID>>) {
    operator fun getValue(entity: Entity<ID>, property: KProperty<*>): E {
        return (entity.getOneToMany<E, M>(column)).first()
    }
}

@Deprecated("Replace with oneToManyRef", ReplaceWith("oneToManyRef(joinableTable, refTable)"))
fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>,
        ID2: Comparable<ID2>, E2: Entity<ID2>, M2: EntityTable<ID2, E2, M2>> oneToMany(joinableTable: M, refTable: M2): OneToManyRelationRef<ID2, E2, M2> {
    return oneToManyRef(joinableTable, refTable)
}

fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>,
        ID2: Comparable<ID2>, E2: Entity<ID2>, M2: EntityTable<ID2, E2, M2>> oneToManyRef(joinableTable: M, refTable: M2): OneToManyRelationRef<ID2, E2, M2> {
    return OneToManyRelationRef(refTable, joinableTable.relatedColumnId!!)
}

fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>,
        ID2: Comparable<ID2>, E2: Entity<ID2>, M2: EntityTable<ID2, E2, M2>> oneToOneRef(joinableTable: M, refTable: M2): OneToManyRelationRef<ID2, E2, M2> {
    return OneToManyRelationRef(refTable, joinableTable.relatedColumnId!!)
}


fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>> M.asList(): OneToManyRelation<ID, E, M> {
    return OneToManyRelation(relatedColumnId!! as Column<EntityID<ID>>)
}
fun <ID: Comparable<ID>, E: Entity<ID>, M: EntityTable<ID, E, M>> M.asReverseOne(): OneToOneRelation<ID, E, M> {
    return OneToOneRelation(relatedColumnId!! as Column<EntityID<ID>>)
}
