package io.creavity.exposedx.dao.queryset

import io.creavity.exposedx.dao.manager.EntityManager
import org.jetbrains.exposed.sql.*


fun EntityManager<*, *, *>.addRelatedJoin(table: Table, newJoin: (ColumnSet.() -> ColumnSet)) {
    if(_parent != null) {
        _parent!!.addRelatedJoin(table, newJoin)
    } else {
        this.relatedJoin[table] = newJoin
    }
}

fun joinWith(leftTable: Table, rightTable: Table, column: Column<*>): (ColumnSet.() -> ColumnSet) {
    val originalLeftTable = if(leftTable is Alias<*>) leftTable.delegate else leftTable

    val rightOn: Column<*>
    val leftOn: Column<*>

    if(column.table == originalLeftTable) {
        leftOn = if(leftTable is Alias<*>) leftTable[column] else column
        rightOn = if(rightTable is Alias<*>) rightTable[column.referee!!] else column.referee!!
    } else {
        leftOn = if(leftTable is Alias<*>) leftTable[column.referee!!] else column.referee!!
        rightOn = if(rightTable is Alias<*>) rightTable[column] else column
    }
    return {
        leftJoin(rightTable, { leftOn }, { rightOn })
    }
}




fun EntityManager<*, *, *>.joinWithParent() {
    _parent?.let { leftTable ->
        leftTable.joinWithParent() // join parent with his parent
        val parentAlias = leftTable.aliasRelated

        addRelatedJoin(this.aliasRelated!!,
                joinWith(parentAlias?:leftTable, this.aliasRelated!!, this.relatedColumnId!!)
        )
    }
}


