package io.creavity.exposedx.dao.tables

import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.transactions.TransactionManager

val transactionExist get() = TransactionManager.isInitialized() && TransactionManager.currentOrNull() != null

interface CopiableObject<M: CopiableObject<M>> {
    fun copy(): M = this.javaClass.constructors.first().let {
        it.isAccessible = true
        it.newInstance(null) as M
    }
}

typealias JoinFunction = (ColumnSet.() -> ColumnSet)
