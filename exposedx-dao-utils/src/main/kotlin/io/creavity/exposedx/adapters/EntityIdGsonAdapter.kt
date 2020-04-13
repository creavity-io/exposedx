package io.creavity.exposedx.adapters
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.jetbrains.exposed.dao.id.EntityID
import java.lang.reflect.Type


class EntityIdGsonAdapter: JsonSerializer<EntityID<*>> {
    override fun serialize(src: EntityID<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src._value)
    }
}