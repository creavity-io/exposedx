package io.creavity.exposedx.dao.entities.generics

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.manager.EntityManager


abstract class IntEntity : Entity<Int>() {
    override val idClass get() = Int::class.java
}

open class IntEntityManager<E : Entity<Int>, M: EntityManager<Int, E, M>>(name: String = "", columnName: String = "id") : EntityManager<Int, E, M>(name) {
    override val id by integer(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}


