package io.creavity.exposedx.dao.entities.generics

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.tables.EntityTable

abstract class LongEntity : Entity<Long>() {
    override val idClass get() = Long::class.java
}

open class LongEntityTable<T : Entity<Long>, M: EntityTable<Long, T, M>>(name: String = "", columnName: String = "id") : EntityTable<Long, T, M>(name) {
    override val id by long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

@Deprecated("Use LongEntityTable instead")
open class LongEntityManager<T : Entity<Long>, M: EntityTable<Long, T, M>>(name: String = "", columnName: String = "id") : LongEntityTable<T, M>(name, columnName)