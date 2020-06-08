package io.creavity.exposedx.columns

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import kotlin.reflect.KClass

interface StringColumnSerializer<T> {
    fun serialize(o: T): String
    fun deserialize(s: String): T
}

class SerializedColumn<T: Any>(private val clazz: KClass<T>, private val serializer: StringColumnSerializer<T>) : TextColumnType() {
    override fun valueFromDB(value: Any): T {
        if(clazz.isInstance(value)) return value as T
        return serializer.deserialize(super.valueFromDB(value) as String)
    }

    override fun nonNullValueToString(value: Any): String {
        return super.nonNullValueToString(serializer.serialize(value as T))
    }

    override fun notNullValueToDB(value: Any): Any {
        return super.notNullValueToDB(serializer.serialize(value as T))
    }
}

fun <T: Any> Table.serialized(name: String, clazz: KClass<T>, serializer: StringColumnSerializer<T>): Column<T> = registerColumn(name, SerializedColumn(clazz, serializer))
