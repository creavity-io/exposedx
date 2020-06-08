package io.creavity.exposedx.dao.entities

import io.creavity.exposedx.dao.tables.EntityTable
import io.creavity.exposedx.dao.tables.MutableResultRow
import io.creavity.exposedx.dao.tables.reload
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.full.companionObjectInstance

enum class FlushAction {
    NONE,
    INSERT,
    UPDATE,
}

abstract class Entity<ID : Comparable<ID>> : EntityDelegator<ID>, MutableResultRow<ID>() {

    /* Getters */
    @Suppress("UNCHECKED_CAST")
    final override val table: EntityTable<ID, Entity<ID>, *>
        get() = this::class.companionObjectInstance as EntityTable<ID, Entity<ID>, *>


    override fun <T> Column<*>.lookup(): T {
        return this@Entity.getValue(this)
    }

    override fun <T> Column<*>.putValue(value: T) {
        this@Entity.setValue(this, value)
    }

    /**
     * Delete this entity.
     *
     * This will remove the entity from the database as well as the cache.
     */
    fun delete() {
        table.delete(this.id)
    }

    fun save(vararg columns: Column<*>): Entity<ID> {
        return this.table.save(this, columns)
    }

    fun reload(reset: Boolean = false) {
        this.table.reload(this, reset, true)
    }

    fun getOrNull(): Entity<ID>? = kotlin.runCatching{ this.apply { reload() } }.getOrNull()


    /**
    * Used by EntityGsonAdapter, because I cant infer ID class, If you found other way fell free from change it.
    * */
    protected abstract val idClass: Class<ID>


}

fun Entity<*>.isNew() = this.id._value == null
fun Entity<*>.flush() = this.table.flush()
