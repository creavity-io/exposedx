package io.creavity.exposedx.dao.exceptions

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

class EntityNotFoundException(val id: EntityID<*>, val entity: IdTable<*>)
    : Exception("Entity ${entity::class.simpleName}, id=$id not found in the database")

