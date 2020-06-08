package io.creavity.exposedx.dao.entities.generics

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.tables.EntityTable
import java.util.*

abstract class UUIDEntity : Entity<UUID>() {
    override val idClass get() = UUID::class.java
}

open class UUIDEntityTable<T : Entity<UUID>, M: EntityTable<UUID, T, M>>(name: String = "", columnName: String = "id") : EntityTable<UUID, T, M>(name) {
    override val id by uuid(columnName).autoGenerate().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(originalId) }
}


@Deprecated("Use UUIDEntityTable instead")
open class UUIDEntityManager<T : Entity<UUID>, M: EntityTable<UUID, T, M>>(name: String = "", columnName: String = "id") : UUIDEntityTable<T, M>(name, columnName)