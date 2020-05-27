package io.creavity.exposedx.dao

import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityManager
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.entities.manyToOne
import io.creavity.exposedx.dao.manager.new
import io.creavity.exposedx.dao.queryset.first
import io.creavity.exposedx.dao.queryset.last
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityQueryTest {
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
    fun `Test count in entity querys `() {
        Country.new {
            name = "Argentina"
        }
        Country.new { name = "Australia"; }
        Country.new { name = "Peru" }

        // val count = Country.objects.filter { population greater 500 }.count()

        // assertThat(count).isEqualTo(2L)

    }

    @Test
    fun `Test delete in entity querys `() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        Country.new { name = "Peru" }

        val itemsDeleted = Country.objects.filter { name like "A%"}.delete()

        assertThat(itemsDeleted).isEqualTo(2L)

        val count = Country.objects.filter { name like "A%"}.count()

        assertThat(count).isEqualTo(0L)
    }

    @Test
    fun `Test filter in entity querys`() {
        Country.new { name = "Peru" }
        Country.new { name = "Chile" }
        Country.new { name = "Brazil" }

        val results = Country.objects.filter { name eq "Peru" }
        assertThat(results.count()).isEqualTo(1L)
        assertThat(results.first().name).isEqualTo("Peru")
    }

    @Test
    fun `Test exclude in entity querys `() {
        Country.new { name = "Peru" }
        Country.new { name = "Chile" }
        Country.new { name = "Brazil" }

        val results = Country.objects.exclude { name eq "Peru"}
        assertThat(results.count()).isEqualTo(2L)
        assertThat(results.first().name).isEqualTo("Chile")
        assertThat(results.last().name).isEqualTo("Brazil")
    }

    @Test
    fun `Test update statement in entity querys `() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        Country.new { name = "Peru" }

        Country.objects.filter { name like "A%" }.update {
            it[name] = concat(name, stringParam("1"))
        }

        val results = Country.objects.all()
        assertThat(results.count()).isEqualTo(3L)
        assertThat(results.toList()[0].name).isEqualTo("Argentina1")
        assertThat(results.toList()[1].name).isEqualTo("Australia1")
        assertThat(results.toList()[2].name).isEqualTo("Peru")
    }

    @Test
    fun `Test update with updateEach in entity querys `() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        Country.new { name = "Peru" }

        Country.objects.filter { name like "A%"}.forUpdate {
            it.name = it.name + "1"
        }

        val results = Country.objects.all()
        assertThat(results.count()).isEqualTo(3L)
        assertThat(results.toList()[0].name).isEqualTo("Argentina1")
        assertThat(results.toList()[1].name).isEqualTo("Australia1")
        assertThat(results.toList()[2].name).isEqualTo("Peru")

    }


}
