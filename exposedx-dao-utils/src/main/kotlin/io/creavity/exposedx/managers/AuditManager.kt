package io.creavity.exposedx.managers

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.ManyToOptionalRelationRef
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.entities.manyToOne
import io.creavity.exposedx.dao.tables.EntityTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime
import java.util.*


interface IAuditableTable<UID : Comparable<UID>, UE : Entity<UID>, USER_TABLE : EntityTable<UID, UE, USER_TABLE>> {
    val createdAt: Column<DateTime>
    val createdBy: EntityTable<UID, UE, USER_TABLE>
    val modifiedAt: Column<DateTime>
    val modifiedBy: EntityTable<UID, UE, USER_TABLE>
}

abstract class AuditableTable<ID : Comparable<ID>, E : Entity<ID>, T : EntityTable<ID, E, T>,
        UID : Comparable<UID>, UE : Entity<UID>, USER_TABLE : EntityTable<UID, UE, USER_TABLE>>(name: String="", userTable: USER_TABLE): SoftDeleteTable<ID, E, T>(name),
        IAuditableTable<UID, UE, USER_TABLE> {

    override val createdAt by datetime("created_at").clientDefault { DateTime.now() }
    override val createdBy by manyToOne("created_by", userTable)
    override val modifiedAt by datetime("modified_at").clientDefault { DateTime.now() }
    override val modifiedBy by manyToOne("modified_by", userTable)
}



abstract class IntAuditableTable<E : Entity<Int>,
        T: EntityTable<Int, E, T>,
        UID : Comparable<UID>,
        UE : Entity<UID>,
        USER_TABLE : EntityTable<UID, UE, USER_TABLE>>(name: String = "", columnName: String = "id", userTable: USER_TABLE) :
        AuditableTable<Int, E, T, UID, UE, USER_TABLE>(name, userTable) {
    override val id by integer(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

abstract class LongAuditableTable<E : Entity<Long>,
        T: EntityTable<Long, E, T>,
        UID : Comparable<UID>,
        UE : Entity<UID>,
        USER_TABLE : EntityTable<UID, UE, USER_TABLE>>
        (name: String = "", columnName: String = "id", userTable: USER_TABLE) :
        AuditableTable<Long, E, T, UID, UE, USER_TABLE>(name, userTable) {
    override val id by long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

abstract class UUIDAuditableTable<E : Entity<UUID>,
        T: EntityTable<UUID, E, T>,
        UID : Comparable<UID>,
        UE : Entity<UID>,
        USER_TABLE : EntityTable<UID, UE, USER_TABLE>>
(name: String = "", columnName: String = "id", userTable: USER_TABLE) :
        AuditableTable<UUID, E, T, UID, UE, USER_TABLE>(name, userTable) {
    override val id by uuid(columnName).autoGenerate().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}




interface IAuditableEntity<ID: Comparable<ID>, E : Entity<ID>?> : SoftDeleteEntity {
    @Suppress("UNCHECK_CAST")
    private val entity get() = this as Entity<Comparable<Any>>
    private val auditTable get() = this.entity.table as IAuditableTable<*, *, *>

    var createdAt: DateTime
        get() = with(entity) { auditTable.createdAt.lookup() as DateTime }
        set(value) = with(entity) { auditTable.createdAt.putValue(value) }

    var createdBy: E
        get(): E = with(entity) { auditTable.createdBy.lookup()as E }
        set(value) = with(entity) { auditTable.createdBy.putValue(value) }

    var modifiedAt: DateTime
        get() = with(entity) { auditTable.modifiedAt.lookup() as DateTime }
        set(value) = with(entity) { auditTable.modifiedAt.putValue(value) }

    var modifiedBy: E
        get(): E = with(entity) { auditTable.modifiedBy.lookup() as E }
        set(value) = with(entity) { auditTable.modifiedBy.putValue(value) }
}
