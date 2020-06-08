package io.creavity.exposedx.dao.entities.generics

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.tables.EntityTable


abstract class IntEntity : Entity<Int>() {
    override val idClass get() = Int::class.java
}

open class IntEntityTable<E : Entity<Int>, M: EntityTable<Int, E, M>>(name: String = "", columnName: String = "id") : EntityTable<Int, E, M>(name) {
    override val id by integer(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}


@Deprecated("Use IntEntityTable instead")
open class IntEntityManager<T : Entity<Int>, M: EntityTable<Int, T, M>>(name: String = "", columnName: String = "id") : IntEntityTable<T, M>(name, columnName)