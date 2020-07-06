package io.creavity.exposedx.managers

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.generics.*
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.tables.DefaultEntityManager
import io.creavity.exposedx.dao.tables.EntityTable
import io.creavity.exposedx.dao.queryset.EntityQuery
import io.creavity.exposedx.dao.queryset.EntityQueryBase
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import java.util.*

open class SoftDeleteEntityQuery<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, *>>(entityTable: T, rawQuery: Query) :
        EntityQueryBase<ID, E, T>(entityTable, rawQuery) {
    override fun delete(): Int {
        return this.update {
            it[(this as SoftDeleteTable<ID, E, *>).isDeleted] = true
        }
    }
}


open class SoftDeleteManager<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>>(
        override val entityTable: SoftDeleteTable<ID, E, T>) :
        DefaultEntityManager<ID, E, T>(entityTable) {


    override fun delete(id: EntityID<ID>) {
        // tal vez convenga hacer un save nada más de esa forma hace un bulk update cuando hace varios delete individuales.. maybe ^
        this.entityTable.objects.filter { this.id eq id }.delete()
    }

    override fun buildEntityQuery(query: Query) = SoftDeleteEntityQuery(entityTable as T, query).filter { entityTable.isDeleted eq false }

    fun buildEntityQueryAllObjects(): EntityQuery<ID, E, T> = SoftDeleteEntityQuery(entityTable as T, entityTable.buildQuery())
}


abstract class SoftDeleteTable<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>>(name: String="") : EntityTable<ID, E, T>(name) {

    override val manager = SoftDeleteManager(this)

    val isDeleted = bool("is_deleted").default(false)

    val allObjects get() = manager.buildEntityQueryAllObjects()

}


interface SoftDeleteEntity {
    private val entity get() = this as Entity<Comparable<Any>>
    private val softDeleteTable get() = this.entity.table as SoftDeleteTable

    var isDeleted: Boolean
        get() = with(entity) { softDeleteTable.isDeleted.lookup() as Boolean }
        set(value) = with(entity) { softDeleteTable.isDeleted.putValue(value) }
}


abstract class IntSoftDeleteTable<E : Entity<Int>, M: EntityTable<Int, E, M>>(name: String = "", columnName: String = "id") : SoftDeleteTable<Int, E, M>(name) {
    override val id by integer(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}
abstract class UUIDSoftDeleteTable<E : Entity<UUID>, M: EntityTable<UUID, E, M>>(name: String = "", columnName: String = "id") : SoftDeleteTable<UUID, E, M>(name) {
    override val id by uuid(columnName).autoGenerate().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}
abstract class LongSoftDeleteTable<E : Entity<Long>, M: EntityTable<Long, E, M>>(name: String = "", columnName: String = "id") : SoftDeleteTable<Long, E, M>(name) {
    override val id by long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

open class IntSoftDeleteEntity(): IntEntity(), SoftDeleteEntity
open class LongSoftDeleteEntity(): LongEntity(), SoftDeleteEntity
open class UUIDSoftDeleteEntity(): UUIDEntity(), SoftDeleteEntity