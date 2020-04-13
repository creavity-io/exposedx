package io.creavity.exposedx.dao.entities.generics

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.manager.EntityManager

abstract class LongEntity : Entity<Long>() {
    override val idClass get() = Long::class.java
}

open class LongEntityManager<T : Entity<Long>, M: EntityManager<Long, T, M>>(name: String = "", columnName: String = "id") : EntityManager<Long, T, M>(name) {
    override val id by long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

