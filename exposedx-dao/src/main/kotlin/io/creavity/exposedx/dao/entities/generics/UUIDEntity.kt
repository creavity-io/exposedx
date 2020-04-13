package io.creavity.exposedx.dao.entities.generics

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.manager.EntityManager
import java.util.*

abstract class UUIDEntity : Entity<UUID>() {
    override val idClass get() = UUID::class.java
}

open class UUIDEntityManager<T : Entity<UUID>, M: EntityManager<UUID, T, M>>(name: String = "", columnName: String = "id") : EntityManager<UUID, T, M>(name) {
    override val id by uuid(columnName).autoGenerate().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(originalId) }
}

