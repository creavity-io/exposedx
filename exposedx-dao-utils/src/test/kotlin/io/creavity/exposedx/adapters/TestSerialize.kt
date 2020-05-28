package io.creavity.exposedx.adapters

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import io.creavity.exposedx.dao.entities.*
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityManager
import io.creavity.exposedx.dao.manager.new
import io.creavity.exposedx.dao.queryset.first
import io.mockk.clearMocks
import io.mockk.spyk

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.lang.reflect.ParameterizedType
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

val gson = GsonBuilder().apply {
    ServiceLoader.load(JsonDeserializer::class.java, JsonDeserializer::class.java.classLoader).iterator().forEach { clazz ->
        val toType = clazz.javaClass.genericInterfaces
                .filterIsInstance(ParameterizedType::class.java)
                .first { it.rawType == JsonDeserializer::class.java}
                .actualTypeArguments[0] // obtiene el T del JsonDeserializer<T>

        val toClass = if(toType is Class<*>) toType else (toType as ParameterizedType).rawType as Class<*>

        this.registerTypeHierarchyAdapter(toClass, clazz)
    }
    ServiceLoader.load(JsonSerializer::class.java, JsonSerializer::class.java.classLoader).iterator().forEach { clazz ->
        val toType = clazz.javaClass.genericInterfaces
                .filterIsInstance(
                        ParameterizedType::class.java)
                .first { it.rawType == JsonSerializer::class.java}
                .actualTypeArguments[0] // obtiene el T del JsonDeserializer<T>

        val toClass = if(toType is Class<*>) toType else (toType as ParameterizedType).rawType as Class<*>

        this.registerTypeHierarchyAdapter(toClass, clazz)
    }
}.create()

class TestSerialize {
    @Test
    fun `Test serialize a simple bean`() {
        val t = Country()
        t.name = "Peru"

        assertThat(gson.toJson(t)).isEqualTo("{\"name\":\"Peru\"}")
    }


    @Test
    fun `Test deserialize objects with ids`() {
        val json = "{\"name\":\"Peru\",\"id\":1}"

        val obj = gson.fromJson(json, Country::class.java)
        assertThat(obj.name).isEqualTo("Peru")
        assertThat(obj.id.value).isEqualTo(1)
    }

    @Test
    fun `Test serialize a many to one bean`() {
        val peru = Country()
        peru.name = "Peru"

        val t = Region()
        t.name = "La Libertad"
        t.country = peru

        assertThat(gson.toJson(t)).isEqualTo("""{"country":{"name":"Peru"},"name":"La Libertad"}""")
    }

    @Test
    fun `Test Many to Optional to JSON`() {
        val peru = Country()
        peru.name = "Peru"

        val t = Region()
        t.name = "La Libertad"
        t.country = peru

        val school = School()
        school.region = t
        school.name = "San Juan"

        assertThat(gson.toJson(school)).isEqualTo("""{"name":"San Juan","region":{"country":{"name":"Peru"},"name":"La Libertad"}}""")
    }



    @Test
    fun `Test deserialize with nested objects`() {
        val json = "{\"country\":{\"name\":\"Peru\",\"id\":1},\"name\":\"La Libertad\",\"id\":1}"

        val obj = gson.fromJson(json, Region::class.java)
        assertThat(obj.name).isEqualTo("La Libertad")
        assertThat(obj.country.name).isEqualTo("Peru")
        assertThat(gson.toJson(obj)).isEqualTo(json)
    }

    @Test
    fun `Test serialize nested incomplete objects`() {
        val json = "{\"country\":{\"id\":1},\"name\":\"La Libertad\",\"id\":1}"

        val peru = Country()
        peru.id._value = 1

        val t = Region()
        t.id._value = 1
        t.name = "La Libertad"
        t.country = peru

        val obj = gson.toJson(t, Region::class.java)
        assertThat(obj).isEqualTo(json)
    }

    @Test
    fun `Test deserialize with optional objects`() {
        val json = """{"name":"San Juan","region": {"country":{"name":"Peru"},"name":"La Libertad"}, "secondaryRegion": null}"""

        val obj = gson.fromJson(json, School::class.java)
        assertThat(obj.region.name).isEqualTo("La Libertad")
        assertThat(obj.secondaryRegion).isEqualTo(null)
    }

    @Test
    fun `Test serialize no set values as null`() {
        val peru = Country()
        peru.name = "Peru"

        val region = Region()
        region.country = peru
        assertThat(gson.toJson(region)).isEqualTo("{\"country\":{\"name\":\"Peru\"}}")
    }

    @Test
    fun `Test deserialize with 0 id for UUIDManager`() {

        val json = """{"id": "", "name":"San Juan"}"""

        val obj = gson.fromJson(json, CountryUUID::class.java)
        assertThat(obj.id._value).isNull()
        assertThat(obj.isNew()).isTrue()
    }

    @Test
    fun `Test deserialize with 0 id`() {
        val json = """{"id": 0, "name":"San Juan","region": {"id": 0, "country":{"id": 0, "name":"Peru"},"name":"La Libertad"}, "secondaryRegion": null}"""

        val obj = gson.fromJson(json, School::class.java)
        assertThat(obj.id._value).isNull()
        assertThat(obj.isNew()).isTrue()
        assertThat(obj.region.isNew()).isTrue()
        assertThat(obj.region.country.isNew()).isTrue()
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTestSerialize {
    lateinit var connection: Connection

    @BeforeAll
    fun beforeAll() {
        Class.forName("org.h2.Driver").newInstance()

        Database.connect({
            connection = spyk(DriverManager.getConnection("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "", ""))
            connection
        })
    }

    @BeforeEach
    fun before() {
        transaction {
            SchemaUtils.create(Country, Region, School)
        }
        clearMocks(connection)
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(Country, Region, School)
        }
    }

    @Test
    fun `Test serialize a with saved objects`() {
        val peru = Country()
        peru.name = "Peru"
        peru.save()

        val region = Region()
        region.name = "La Libertad"
        region.country = peru
        region.save()

        assertThat(gson.toJson(region)).isEqualTo("{\"country\":{\"name\":\"Peru\",\"id\":1},\"name\":\"La Libertad\",\"id\":1}")
    }


    @Test
    fun `Test dont serialize a lazy object`() {
        val peru = Country()
        peru.name = "Peru"
        peru.save()

        val region = Region()
        region.name = "La Libertad"
        region.country = peru
        region.save()

        val regionToSerialize = Region.objects.first()
        assertThat(gson.toJson(regionToSerialize)).isEqualTo("{\"country\":{\"id\":1},\"name\":\"La Libertad\",\"id\":1}")
    }

    @Test
    fun `Test insert from serialized`() {

        val json = """{"name":"Peru"}"""

        val obj = gson.fromJson(json, Country::class.java)
        obj.save()
        assertThat(obj.id.value).isEqualTo(1)
    }

    @Test
    fun `Test update from serialized`() {
        val peru = Country.new { name="Peru" }
        assertThat(peru.id.value).isEqualTo(1)

        val json = """{"name":"China", "id": 1}"""
        val china = gson.fromJson(json, Country::class.java)
        china.save()
        assertThat(china.id.value).isEqualTo(1)
        assertThat(Country.objects.count()).isEqualTo(1)
        assertThat(Country.objects.filter { name eq "China" }.first().id.value).isEqualTo(1)
    }

    @Test
    fun `Test serialize a with saved objects in transaction`() {
        transaction {
            val peru = Country()
            peru.name = "Peru"
            peru.save()

            val region = Region()
            region.name = "La Libertad"
            region.country = peru
            region.save()

            assertThat(gson.toJson(region)).isEqualTo("{\"country\":{\"name\":\"Peru\",\"id\":1},\"name\":\"La Libertad\",\"id\":1}")
        }
    }

}