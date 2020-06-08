package io.creavity.exposedx.dao.entities

import io.creavity.exposedx.dao.tables.EntityTable
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.KProperty

interface EntityDelegator<ID: Comparable<ID>> {
    fun <T> Column<*>.lookup(): T
    fun <T> Column<*>.putValue(value: T)

    fun <T> EntityTable<*, *, *>.lookup(): T = this.relatedColumnId!!.lookup()
    fun <T> EntityTable<*, *, *>.putValue(value: T) = this.relatedColumnId!!.putValue(value)

    operator fun <T> Column<T>.getValue(o: Entity<ID>, desc: KProperty<*>): T = lookup()
    operator fun <T> Column<T>.setValue(o: Entity<ID>, desc: KProperty<*>, value: T) = putValue(value)

    operator fun <ID_R: Comparable<ID_R>, E : Entity<ID_R>, M : EntityTable<ID_R, E, M>> EntityTable<ID_R, E, M>.getValue(o: Entity<ID>, desc: KProperty<*>): E = this.lookup()
    operator fun <ID_R: Comparable<ID_R>, E: Entity<ID_R>, M: EntityTable<ID_R, E, M>> EntityTable<ID_R, E, M>.setValue(o: Entity<ID>, desc: KProperty<*>, value: E) = this.putValue(value)

    operator fun <ID_R: Comparable<ID_R>, E: Entity<ID_R>, M: EntityTable<ID_R, E, M>> NullableRelation<ID_R, E, M>.getValue(o: Entity<ID>, desc: KProperty<*>): E? = this.table.lookup()
    operator fun <ID_R: Comparable<ID_R>, E: Entity<ID_R>, M: EntityTable<ID_R, E, M>> NullableRelation<ID_R, E, M>.setValue(o: Entity<ID>, desc: KProperty<*>, value: E?) = this.table.putValue(value)

}