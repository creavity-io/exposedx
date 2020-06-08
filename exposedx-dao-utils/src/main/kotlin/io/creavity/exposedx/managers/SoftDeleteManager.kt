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
        override val entityTable: EntityTable<ID, E, T>) :
        DefaultEntityManager<ID, E, T>(entityTable) {

    private lateinit var  columnIsDeleted: Column<Boolean>

    override fun createColumns() {
        super.createColumns()
        columnIsDeleted = with(entityTable) { bool("is_deleted").default(false) }
    }

    override fun delete(id: EntityID<ID>) {
        // tal vez convenga hacer un save nada más de esa forma hace un bulk update cuando hace varios delete individuales.. maybe ^
        this.entityTable.objects.filter { this.id eq id }.delete()
    }

    val isDeleted get() = columnIsDeleted.getValue(entityTable, this::columnIsDeleted)

    override fun buildEntityQuery(query: Query) = SoftDeleteEntityQuery(entityTable as T, query).filter { isDeleted eq false }

    fun buildEntityQueryAllObjects(): EntityQuery<ID, E, T> = SoftDeleteEntityQuery(entityTable as T, entityTable.buildQuery())
}


@Suppress("UNCHECKED_CAST")
fun <ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>>
        EntityTable<ID, E, T>.softDeleteManager(): SoftDeleteManager<ID, E, T> = SoftDeleteManager(this as T)


interface SoftDeleteTable<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>> {

    val manager: SoftDeleteManager<ID, E, T>

    val isDeleted get() = manager.isDeleted

    val allObjects get() = manager.buildEntityQueryAllObjects()

}


interface SoftDeleteEntity {
    private val entity get() = this as Entity<Comparable<Any>>
    private val softDeleteManager get() = this.entity.table.manager as SoftDeleteManager

    var isDeleted: Boolean
        get() = with(entity) { softDeleteManager.isDeleted.lookup() as Boolean }
        set(value) = with(entity) { softDeleteManager.isDeleted.putValue(value) }
}


abstract class IntSoftDeleteTable<E : Entity<Int>, M: EntityTable<Int, E, M>>(name: String = "", columnName: String = "id") : IntEntityTable<E, M>(name, columnName), SoftDeleteTable<Int, E, M> {
    override val manager = SoftDeleteManager(this)
}
abstract class UUIDSoftDeleteTable<E : Entity<UUID>, M: EntityTable<UUID, E, M>>(name: String = "", columnName: String = "id") : UUIDEntityTable<E, M>(name, columnName), SoftDeleteTable<UUID, E, M> {
    override val manager = SoftDeleteManager(this)
}
abstract class LongSoftDeleteTable<E : Entity<Long>, M: EntityTable<Long, E, M>>(name: String = "", columnName: String = "id") : LongEntityTable<E, M>(name, columnName), SoftDeleteTable<Long, E, M> {
    override val manager = SoftDeleteManager(this)
}

open class IntSoftDeleteEntity(): IntEntity(), SoftDeleteEntity
open class LongSoftDeleteEntity(): LongEntity(), SoftDeleteEntity
open class UUIDSoftDeleteEntity(): LongEntity(), SoftDeleteEntity