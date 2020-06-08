package io.creavity.exposedx.managers

import io.creavity.exposedx.dao.entities.*
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.LongEntity
import io.creavity.exposedx.dao.entities.generics.UUIDEntity
import io.creavity.exposedx.dao.tables.EntityTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime
import java.util.*


abstract class AuditManagerAbstract<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>,
        UID : Comparable<UID>, UE : Entity<UID>, UT : EntityTable<UID, UE, UT>>(
        override val entityTable: EntityTable<ID, E, T>,
        open val userTable: () -> UT): SoftDeleteManager<ID, E, T>(entityTable) {

    abstract val createdAt: Column<DateTime>
    abstract val createdBy: EntityTable<UID, UE, UT>
    abstract val modifiedAt: Column<DateTime>
    abstract val modifiedBy: EntityTable<UID, UE, UT>

}

class AuditManager<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>, UID : Comparable<UID>, UE : Entity<UID>, UT : EntityTable<UID, UE, UT>>(
        override val entityTable: EntityTable<ID, E, T>,
        override val userTable: () -> UT)
    : AuditManagerAbstract<ID, E, T, UID, UE, UT>(entityTable, userTable) {

    private lateinit var columnCreatedAt: Column<DateTime>
    private lateinit var columnCreatedBy: ManyToOneRelationRef<UID, UE, UT>
    private lateinit var columnModifiedAt: Column<DateTime>
    private lateinit var columnModifiedBy: ManyToOneRelationRef<UID, UE, UT>

    override fun createColumns() {
        super.createColumns()
        val userTable = userTable()
        columnCreatedAt = with(entityTable) { datetime("created_at").clientDefault { DateTime.now() } }
        columnCreatedBy  = entityTable.manyToOne("created_by", userTable)
        columnModifiedAt = with(entityTable) { datetime("modified_at").clientDefault { DateTime.now() } }
        columnModifiedBy  = entityTable.manyToOne("modified_by", userTable)
    }

    // El .columns es para ejecutar el create columns, no me siento orgulloso de este código pero por ahora no hay muchas soluciones
    // porque no hay forma de inicializar las columnas de forma perezosa
    override val createdAt get() = entityTable.columns.let { columnCreatedAt.getValue(entityTable, this::columnCreatedAt) }
    override val createdBy get() = entityTable.columns.let { columnCreatedBy.getValue(entityTable, this::columnCreatedBy) }
    override val modifiedAt get() = entityTable.columns.let { columnModifiedAt.getValue(entityTable, this::columnModifiedAt) }
    override val modifiedBy get() = entityTable.columns.let { columnModifiedBy.getValue(entityTable, this::columnModifiedBy) }

}

class AuditManagerOptional<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>, UID : Comparable<UID>, UE : Entity<UID>, UT : EntityTable<UID, UE, UT>>(
        override val entityTable: EntityTable<ID, E, T>,
        override val userTable: () -> UT)
    : AuditManagerAbstract<ID, E, T, UID, UE, UT>(entityTable, userTable) {

    private lateinit var columnCreatedAt: Column<DateTime>
    private lateinit var columnCreatedBy: ManyToOptionalRelationRef<UID, UE, UT>
    private lateinit var columnModifiedAt: Column<DateTime>
    private lateinit var columnModifiedBy: ManyToOptionalRelationRef<UID, UE, UT>

    override fun createColumns() {
        super.createColumns()
        val userTable = userTable()
        columnCreatedAt = with(entityTable) { datetime("created_at").clientDefault { DateTime.now() } }
        columnCreatedBy  = entityTable.manyToOptional("created_by", userTable)
        columnModifiedAt = with(entityTable) { datetime("modified_at").clientDefault { DateTime.now() } }
        columnModifiedBy  = entityTable.manyToOptional("modified_by", userTable)
    }

    override val createdAt get() =  entityTable.columns.let { columnCreatedAt.getValue(entityTable, this::columnCreatedAt) }
    override val createdBy get() =  entityTable.columns.let { columnCreatedBy.getValue(entityTable, this::columnCreatedBy) }
    override val modifiedAt get() =  entityTable.columns.let { columnModifiedAt.getValue(entityTable, this::columnModifiedAt) }
    override val modifiedBy get() =  entityTable.columns.let { columnModifiedBy.getValue(entityTable, this::columnModifiedBy) }

}

interface IAuditableTable<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>,
        UID : Comparable<UID>, UE : Entity<UID>, USER_TABLE : EntityTable<UID, UE, USER_TABLE>>: SoftDeleteTable<ID, E, T> {

    override val manager: AuditManagerAbstract<ID, E, T, UID, UE, USER_TABLE>

    val createdAt get() = manager.createdAt
    val createdBy get() = manager.createdBy
    val modifiedAt get() = manager.modifiedAt
    val modifiedBy get() = manager.modifiedBy

}


@Suppress("UNCHECKED_CAST")
fun <ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>,
        UID : Comparable<UID>, UE : Entity<UID>, UT : EntityTable<UID, UE, UT>>
        EntityTable<ID, E, T>.auditManager(userTable: () -> UT): AuditManagerAbstract<ID, E, T, UID, UE, UT> = AuditManager(this as T, userTable)

fun <ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>,
        UID : Comparable<UID>, UE : Entity<UID>, UT : EntityTable<UID, UE, UT>>
        EntityTable<ID, E, T>.optionalAuditManager(userTable: () -> UT): AuditManagerAbstract<ID, E, T, UID, UE, UT> =  AuditManagerOptional(this as T, userTable)


interface IAuditableEntity<ID: Comparable<ID>, E : Entity<ID>?> : SoftDeleteEntity {
    private val entity get() = this as Entity<Comparable<Any>>
    private val auditManager get() = this.entity.table as IAuditableTable<*,*,*,*,*,*>

    var createdAt: DateTime
        get() = with(entity) { auditManager.createdAt.lookup() as DateTime }
        set(value) = with(entity) { auditManager.createdAt.putValue(value) }

    var createdBy: E
        get(): E = with(entity) { auditManager.createdBy.lookup()as E }
        set(value) = with(entity) { auditManager.createdBy.putValue(value) }

    var modifiedAt: DateTime
        get() = with(entity) { auditManager.modifiedAt.lookup() as DateTime }
        set(value) = with(entity) { auditManager.modifiedAt.putValue(value) }

    var modifiedBy: E
        get(): E = with(entity) { auditManager.modifiedBy.lookup() as E }
        set(value) = with(entity) { auditManager.modifiedBy.putValue(value) }

}


abstract class IntAuditableTable<E : Entity<Int>, M: EntityTable<Int, E, M>, UID : Comparable<UID>, UE : Entity<UID>, USER_TABLE : EntityTable<UID, UE, USER_TABLE>>(name: String = "", columnName: String = "id") :  IntSoftDeleteTable<E, M>(name, columnName), IAuditableTable<Int, E, M, UID, UE, USER_TABLE> {
    abstract override val manager: AuditManagerAbstract<Int, E, M, UID, UE, USER_TABLE>
}
abstract class UUIDAuditableTable<E : Entity<UUID>, M: EntityTable<UUID, E, M>, UID : Comparable<UID>, UE : Entity<UID>, USER_TABLE : EntityTable<UID, UE, USER_TABLE>>(name: String = "", columnName: String = "id") : UUIDSoftDeleteTable<E, M>(name, columnName), IAuditableTable<UUID, E, M, UID, UE, USER_TABLE> {
    abstract override val manager: AuditManagerAbstract<UUID, E, M, UID, UE, USER_TABLE>
}
abstract class LongAuditableTable<E : Entity<Long>, M: EntityTable<Long, E, M>, UID : Comparable<UID>, UE : Entity<UID>, USER_TABLE : EntityTable<UID, UE, USER_TABLE>>(name: String = "", columnName: String = "id") : LongSoftDeleteTable<E, M>(name, columnName), IAuditableTable<Long, E, M, UID, UE, USER_TABLE> {
    abstract override val manager: AuditManagerAbstract<Long, E, M, UID, UE, USER_TABLE>
}

open class IntAuditableEntity<UID : Comparable<UID>, UE : Entity<UID>?>(): IntEntity(), IAuditableEntity<UID, UE>
open class LongAuditableEntity<UID : Comparable<UID>, UE : Entity<UID>?>(): LongEntity(), IAuditableEntity<UID, UE>
open class UUIDAuditableEntity<UID : Comparable<UID>, UE : Entity<UID>?>(): UUIDEntity(), IAuditableEntity<UID, UE>
