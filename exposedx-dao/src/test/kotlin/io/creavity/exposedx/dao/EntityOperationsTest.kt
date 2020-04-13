package io.creavity.exposedx.dao

import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.manager.flushCache
import io.creavity.exposedx.dao.manager.new
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
            entity3.save()
        }

        transaction {
            val count = Country.select { Country.name eq "Ecuador"}.count()
            assertThat(count).isEqualTo(1)

            val count2 = Country.select { Country.name eq "Brazil"}.count()
            assertThat(count2).isEqualTo(0)
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
    fun `Reset object`() {
        val entity1 = Country.new { name = "Peru" }
        entity1.name = "Ecuador"
        entity1.reload()
        assertThat(entity1.name).isEqualTo("Peru")
    }

    @Test
    fun `Reset with transaction`()  {
        transaction {
            val entity1 = Country.new { name = "Peru" }
            entity1.name = "Ecuador"
            entity1.reload()
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