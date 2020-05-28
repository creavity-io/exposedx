package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.manager.new
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityOperationsTest {

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
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(Country, Region, School)
        }
    }


    @Test
    fun `Create entity object using new method`() {
        Country.new {
            name = "Country Name"
        }
        transaction {
            val count = Country.select { Country.name eq "Country Name"}.count()
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun `Create entitys with inner news`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new { name = "Country" } } }

        transaction {
            School.select { School.name eq "School"}.count().also {
                assertThat(it).isEqualTo(1)
            }
            Region.select { Region.name eq "Region"}.count().also {
                assertThat(it).isEqualTo(1)
            }
            Country.select { Country.name eq "Country"}.count().also {
                assertThat(it).isEqualTo(1)
            }
        }
    }


    @Test
    fun `Create entity object using save method`() {
        val c = Country()
        c.name = "Country Name"
        c.save()

        transaction {
            val count = Country.select { Country.name eq "Country Name"}.count()
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun `Throw not null exception inmediatly when save object`() {
        transaction {
            var exception: Throwable? = null
            try {
                val c = Country()
                c.save()
            } catch (ex: IllegalArgumentException) {
                   exception = ex
            }
            assertThat(exception).isNotNull().withFailMessage("Should raise not null exception")
            assertThat(exception!!.localizedMessage).contains("Country.name")
        }
    }

    @Test
    fun `Batch insert in one transaction`() {
        transaction {
            val entity1 = Country.new { name = "Peru" }
            val entity2 = Country.new { name = "Chile" }
            val entity3 = Country.new { name = "Brazil" }

            assertThat(entity1.id.value).isEqualTo(1)
            assertThat(entity2.id.value).isEqualTo(2)
            assertThat(entity3.id.value).isEqualTo(3)
        }
    }

    @Test
    fun `Test save entities relationed`() {

        transaction {
            val peru = Country.new { name = "Peru" }
            val trujillo = Region.new { name = "Trujillo"; country=peru }
            val school = School.new { name = "Colegio A"; region=trujillo }
        }
    }


    @Test
    fun `Check Id after save`() {
        val entity1 = Country.new { name = "Peru" }
        val entity2 = Country.new { name = "Chile" }
        val entity3 = Country.new { name = "Brazil" }

        assertThat(entity1.id.value).isEqualTo(1)
        assertThat(entity2.id.value).isEqualTo(2)
        assertThat(entity3.id.value).isEqualTo(3)
    }

    @Test
    fun `Update with save method`() {
        Country.new { name = "Peru" }
        Country.new { name = "Chile" }
        Country.new { name = "Brazil" }

        transaction {
            val entity3 = Country.objects.get(3)!!
            entity3.name = "Ecuador"
            entity3.isActive = true
            entity3.save()
        }

        transaction {
            val count = Country.select { Country.name eq "Ecuador"}.count()
            assertThat(count).isEqualTo(1)

            val count2 = Country.select { Country.name eq "Brazil"}.count()
            assertThat(count2).isEqualTo(0)

            val count3 = Country.select { Country.isActive eq true}.count()
            assertThat(count3).isEqualTo(1)

        }
    }

    @Test
    fun `Update only some columns`() {
        val peru = Country.new { name = "Peru" }

        Region.new { name = "Lima"; country=peru; isActive=false}
        Region.new { name = "Arequipa"; country=peru; isActive=false }
        Region.new { name = "Trujillo"; country=peru; isActive=false }

        transaction {
            val entity3 = Region.objects.get(3)!!
            entity3.name = "Chiclayo"
            entity3.isActive = true
            entity3.save(Region.isActive)
            assertThat(entity3.name).isEqualTo("Chiclayo").withFailMessage("Name should not change after save")
        }



        transaction {
            val count = Region.select { (Region.id eq 3) and (Region.isActive eq true)}.count()
            assertThat(count).isEqualTo(1)

            val count2 = Region.select { Region.name eq "Chiclayo"}.count()
            assertThat(count2).isEqualTo(0)
        }
    }

    @Test
    fun `Update without search`() {
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name="Lima"; country=peru; isActive = true}

        transaction {
            val region = Region()
            region.id._value = peru.id._value
            region.name = "Trujillo"
            region.save()
        }

        transaction {
            val region = Region.objects.get(1)!!
            assertThat(region.name).isEqualTo("Trujillo")
            assertThat(region.country.name).isEqualTo("Peru")
            assertThat(region.isActive).isEqualTo(true)
        }
    }


    @Test
    fun `Delete entity`() {
        val entity1 = Country.new { name = "Peru" }
        Country.new { name = "Chile" }
        Country.new { name = "Brazil" }

        entity1.delete()

        transaction {
            val count = Country.selectAll().count()
            assertThat(count).isEqualTo(2)
        }
    }


    @Test
    fun `Use detached object`() {
        val entity1 = Country.new { name = "Peru" }
        assertThat(entity1.name).isEqualTo("Peru")
    }



    @Test
    fun `Reload without reset from object without db`() {
        val peru = Country.new { name = "Peru" }
        Region.new { country=peru; isActive = true; name = "Lima" }

        val region = Region()
        region.id._value = 1
        region.isActive = false
        region.reload()
        assertThat(region.name).isEqualTo("Lima")
        assertThat(region.isActive).isEqualTo(false)
    }

    @Test
    fun `Reload without reset`() {
        val peru = Country.new { name = "Peru" }
        val region = Region.new { country=peru; isActive = true; name = "Lima" }

        region.isActive = false
        region.reload()
        assertThat(region.name).isEqualTo("Lima")
        assertThat(region.isActive).isEqualTo(false)
    }

    @Test
    fun `Reload without reset in transaction`() {
        transaction {
            val peru = Country.new { name = "Peru" }
            val region = Region.new { country=peru; isActive = true; name = "Lima" }
            region.isActive = false
            region.reload()
            region.isActive

            assertThat(region.name).isEqualTo("Lima")
            assertThat(region.isActive).isEqualTo(false)
        }
    }

    @Test
    fun `Reload with reset`() {
        val entity1 = Country.new { name = "Peru" }
        entity1.name = "Ecuador"
        entity1.reload(true)
        assertThat(entity1.name).isEqualTo("Peru")
    }

    @Test
    fun `Reload with reset in transaction`()  {
        transaction {
            val entity1 = Country.new { name = "Peru" }
            entity1.name = "Ecuador"
            entity1.reload(true)
            assertThat(entity1.name).isEqualTo("Peru")
        }
    }

    @Test
    fun `Update detached object`() {
        val entity1 = Country.new { name = "Brazil" }
        entity1.name = "Ecuador"
        assertThat(entity1.name).isEqualTo("Ecuador")
        entity1.save()

        transaction {
            val count = Country.select { Country.name eq "Ecuador"}.count()
            assertThat(count).isEqualTo(1)

            val count2 = Country.select { Country.name eq "Brazil"}.count()
            assertThat(count2).isEqualTo(0)
        }
    }

    @Test
    fun `Update on flush`() {
        Country.new { name = "Brazil" }

        transaction {
           Country.objects.filter { name eq "Brazil" }.forUpdate {
                it.name = "Ecuador"
            }
        }

        transaction {
            val count = Country.select { Country.name eq "Ecuador"}.count()
            assertThat(count).isEqualTo(1)

            val count2 = Country.select { Country.name eq "Brazil"}.count()
            assertThat(count2).isEqualTo(0)
        }
    }
}