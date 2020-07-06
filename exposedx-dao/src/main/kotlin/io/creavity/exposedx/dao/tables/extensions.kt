package io.creavity.exposedx.dao.tables

import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.exceptions.EntityNotFoundException
import io.creavity.exposedx.dao.queryset.localTransaction


/**
 * Create a new entity with the fields that are set in the [init] block. The id will be automatically set.
 *
 * @param init The block where the entities' fields can be set.
 *
 * @return The entity that has been created.
 */
fun <ID : Comparable<ID>, E : Entity<ID>> EntityTable<ID, E, *>.new(init: E.() -> Unit) = localTransaction {
    createInstance().apply {
        this.init()
        this.save()
    }
}

/**
 * Reloads entity fields from database as new object.
 * @param flush whether pending entity changes should be flushed previously
 */
fun <ID : Comparable<ID>, E : Entity<ID>> EntityTable<ID, E, *>.reload(entity: E, reset: Boolean=false, flush: Boolean = false): E = entity.also {
    localTransaction {
        if (flush) {
            _cache.flush()
        }
        if(reset) entity.reset()
        _cache[this@reload].store(entity.id, entity)
        entity.init(this.db, entity.id, findResultRowById(entity.id) ?: throw EntityNotFoundException(entity.id, this@reload))
    }
}

