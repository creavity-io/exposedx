package io.creavity.exposedx.adapters

import com.google.gson.*
import io.creavity.exposedx.dao.entities.Entity
import org.jetbrains.exposed.dao.id.EntityID
import java.lang.reflect.Type
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

class EntityGsonAdapter: JsonDeserializer<Entity<*>>, JsonSerializer<Entity<*>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Entity<*> {
        val klass = (typeOfT as Class<*>).kotlin
        val instance = typeOfT.constructors.first().newInstance() as Entity<*>
        val idClass = klass.memberProperties.first { it.name == "idClass" }.also { it.isAccessible = true }.getter.call(instance) as Class<*>
        val jsonObject = json.asJsonObject
        jsonObject.entrySet().forEach { (field, value) ->
            val fieldProp = klass.memberProperties.firstOrNull { it.name == field } ?: error("${klass.simpleName} not contains ${field} property.")
            when {
                fieldProp is KMutableProperty<*> -> fieldProp.setter.call(instance, context.deserialize(value, fieldProp.returnType.javaType))
                fieldProp.returnType.classifier == EntityID::class -> (fieldProp.getter.call(instance) as EntityID<*>)._value = context.deserialize(value, idClass)
            }
        }
        return instance
    }

    override fun serialize(src: Entity<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val klass = (typeOfSrc as Class<*>).kotlin
        val jsonObject = JsonObject();

        val fields = klass.memberProperties.filter { !arrayOf( "updateOnFlush", "table", "flushAction", "db", "isLoaded", "readValues", "updateOnFlush", "idClass").contains(it.name) }
        fields.forEach {
            jsonObject.add(it.name, context.serialize(it.getter.call(src)))
        }

        return jsonObject
    }
}