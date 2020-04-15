package io.creavity.exposedx.dao.entities

import io.creavity.exposedx.dao.manager.EntityManager
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.KProperty

interface EntityDelegator<ID: Comparable<ID>> {
    fun <T> Column<*>.lookup(): T
    fun <T> Column<*>.putValue(value: T)

    operator fun <T> Column<T>.getValue(o: Entity<ID>, desc: KProperty<*>): T = lookup()
    operator fun <T> Column<T>.setValue(o: Entity<ID>, desc: KProperty<*>, value: T) = putValue(value)

    operator fun <ID_R: Comparable<ID_R>, E : Entity<ID_R>, M : EntityManager<ID_R, E, M>> EntityManager<ID_R, E, M>.getValue(o: Entity<ID>, desc: KProperty<*>): E = this.relatedColumnId!!.lookup()
    operator fun <ID_R: Comparable<ID_R>, E: Entity<ID_R>, M: EntityManager<ID_R, E, M>> EntityManager<ID_R, E, M>.setValue(o: Entity<ID>, desc: KProperty<*>, value: E) = this.relatedColumnId!!.putValue(value)

    operator fun <ID_R: Comparable<ID_R>, E: Entity<ID_R>, M: EntityManager<ID_R, E, M>> NullableRelation<ID_R, E, M>.getValue(o: Entity<ID>, desc: KProperty<*>): E? = this.manager.relatedColumnId!!.lookup()
    operator fun <ID_R: Comparable<ID_R>, E: Entity<ID_R>, M: EntityManager<ID_R, E, M>> NullableRelation<ID_R, E, M>.setValue(o: Entity<ID>, desc: KProperty<*>, value: E?) = this.manager.relatedColumnId!!.putValue(value)

}