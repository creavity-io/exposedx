package io.creavity.exposedx.dao.manager

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import io.creavity.exposedx.dao.entities.Entity
import io.creavity.exposedx.dao.entities.FlushAction
import io.creavity.exposedx.dao.entities.identifiers.DaoEntityID
import org.jetbrains.exposed.sql.*
import kotlin.collections.LinkedHashMap

abstract class MutableResultRow<ID : Comparable<ID>> {
    private var references = mutableMapOf<Column<*>, Entity<*>>()

    internal abstract val table: IdTable<ID>

    val id: EntityID<ID>

    private var updateOnFlush: Boolean = false

    private var _flushAction: FlushAction = FlushAction.NONE

    private var _readValues: ResultRow? = null

    private val writeValues = LinkedHashMap<Column<Any?>, Any?>()

    private var savedValues: Map<Column<Any?>, Any?>? = null

    private var lazyInit: (() -> ResultRow)? = null

    internal var db: Database? = null; private set

    val isLoaded get() = _readValues != null

    init {
        this.id = DaoEntityID(null, table)
    }

    /*
    * Result row from DB
    * */
    internal fun init(db: Database, id: EntityID<ID>, resultRow: ResultRow, updateOnFlush: Boolean=false) {
        this.db = db
        this.id._value = id._value
        this.updateOnFlush = updateOnFlush
        this._readValues = resultRow
        buildReferences()
    }

    /*
   * Result row from lazyInit
   * */
    internal fun init(db: Database, id: EntityID<ID>, lazyInit: () -> ResultRow) {
        this.id._value = id._value
        this.lazyInit = lazyInit
        this.db = db
        this._readValues = null
    }

    internal var readValues
        get() = _readValues?: run {
            _readValues = lazyInit?.let { it();  } ?: ResultRow.createAndFillDefaults(table.columns)
            buildReferences() // en caso sea lazy init
            _readValues!!
        }
        set(value) {
            this.reset().also {
                this._readValues = value
                this.buildReferences()
            }
        }

    internal val flushAction
        get() = when {
            updateOnFlush -> FlushAction.UPDATE
            savedValues.isNullOrEmpty() -> FlushAction.NONE
            else -> _flushAction
        }


    fun reset() {
        this.writeValues.clear()
        this.savedValues = null
        this._flushAction = FlushAction.NONE
    }

    @Suppress("UNCHECKED_CAST")
    internal fun markForSave(db: Database) {
        if(_flushAction == FlushAction.NONE) {
            _flushAction = if (this.id._value == null) FlushAction.INSERT else FlushAction.UPDATE
        }
        this.savedValues = prepareDataToWrite()
        this.db = db
    }
    /*
    * readValues + writeValues
    * */
    @Suppress("UNCHECKED_CAST")
    private fun prepareDataToWrite(): MutableMap<Column<Any?>, Any?> {
        if (flushAction == FlushAction.UPDATE) return writeValues

        val dataToWrite = mutableMapOf<Column<Any?>, Any?>()
        dataToWrite.putAll(writeValues)
        readValues.fieldIndex.keys.filter { readValues.hasValue(it) && it !in this.writeValues }.forEach {
            dataToWrite[it as Column<Any?>] = readValues[it]
        }
        return dataToWrite
    }

    internal fun save(fn: (Column<Any?>, Any?) -> Unit) {
        if(updateOnFlush) { markForSave(db!!) }

        savedValues?.forEach { (column, value) ->
            fn(column, checkValue(value))
        }

        storeWrittenValues()
        reset()
    }

    private fun checkValue(value: Any?): Any? {
        val newValue = if (value is Entity<*>) value.id else value
        if (newValue is Entity<*> && newValue.db != this.db && newValue.db != this.db) {
            error("Can't link entities from different databases.")
        }
        return newValue
    }

    private fun storeWrittenValues() {
        references.clear()
        savedValues?.forEach { c, v ->
            if (v is Entity<*>) {
                readValues[c] = v.id
                references[c] = v
            } else {
                readValues[c] = v
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any?> getValue(column: Column<*>): T = (writeValues[column]?:
                                                                readValues.let { // ejecuto el read values para obtener el by lazy
                                                                    references[column]
                                                                } ?: readValues[column]) as T

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any?> setValue(column: Column<*>, value: T) {
        val currentValue = readValues.getOrNull(column)
        if (currentValue == value || references[column] == value) {
            writeValues.remove(column)
            return
        }
        writeValues[column as Column<Any?>] = value
    }

    internal fun isSelfRerencing(): Boolean {
        return writeValues.any { (key, value) ->
            key.referee == table.id && value is EntityID<*> && value._value == null
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun buildReferences() {
        this.readValues.fieldIndex.keys.filterIsInstance(Column::class.java).mapNotNull { column ->
            column.referee?.table?.let { reference ->
                if(reference is EntityManager<*,*,*> && readValues.hasValue(column)) {
                    val manager = reference as EntityManager<Comparable<Any>, Entity<Comparable<Any>>,*>
                    if(readValues[column]!=null) {
                        column to (manager.lazyWrap(readValues[column] as EntityID<Comparable<Any>>, db!!))
                    }else {
                        null
                    }
                } else {
                    null
                }
            }
        }.also {
            references.clear();
            references.putAll(it)
        }
    }

    private var oneToManyQueries = mutableMapOf<Column<*>, OneToManyQuery<*,*,*>>()

    @Suppress("UNCHECKED_CAST")
    internal fun <E : Entity<ID>, M : EntityManager<ID, E, M>> getOneToMany(column: Column<EntityID<ID>>): OneToManyQuery<ID, E, M> {
        return oneToManyQueries.getOrPut(column) {
            val table = column.table as M
            OneToManyQuery(table, column.table.select { column eq this@MutableResultRow.id })
        } as OneToManyQuery<ID, E, M>
    }
}
