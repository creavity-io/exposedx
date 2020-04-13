package io.creavity.exposedx.dao

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import org.jetbrains.exposed.sql.statements.DeleteStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

// https://github.com/JetBrains/Exposed/pull/830
operator fun ExpressionWithColumnType<String>.plus(t: String) = SqlExpressionBuilder.concat(this, wrap(t))