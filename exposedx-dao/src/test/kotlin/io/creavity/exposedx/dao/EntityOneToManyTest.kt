package io.creavity.exposedx.dao

import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.manager.new
import io.creavity.exposedx.dao.queryset.first
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityOneToManyTest {

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
    fun `Test access related queryset in one to many`() {

        val argentina = Country.new { name = "Argentina" }
        val australia = Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val tujillo = Region.new { name = "Trujillo"; country = peru }
        val buenosAires = Region.new { name = "Buenos Aires"; country = argentina }

        assertThat(peru.regions.count()).isEqualTo(2)
    }

    @Test
    fun `Test foreach related`() {

        val argentina = Country.new { name = "Argentina" }
        val australia = Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val tujillo = Region.new { name = "Trujillo"; country = peru }
        val buenosAires = Region.new { name = "Buenos Aires"; country = argentina }

        peru.regions.all().forEachIndexed { index, region ->
            when(index) {
                0 -> assertThat(region.name).isEqualTo("Lima")
                1 -> assertThat(region.name).isEqualTo("Trujillo")
            }
        }
    }

    @Test
    fun `Test filter by one to many`() {

        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }

        School.new { name = "Escuela 1"; region = lima }
        School.new { name = "Escuela 2"; region = lima }
        School.new { name = "Colegio"; region = lima }


        transaction {
            val result = Region.objects.filter {
                schools.name like "Escuela%"
            }.first()
            //val result = Region.selectAll().first()
            assertThat(result.id).isEqualTo(lima.id)
        }
    }

    @Test
    fun `Test filter by deep one to many`() {

        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }

        School.new { name = "Escuela 1"; region = lima }
        School.new { name = "Escuela 2"; region = lima }
        School.new { name = "Colegio"; region = lima }


        transaction {
            val result = Country.objects.filter {
                regions.schools.name like "Escuela%"
            }.first()
            //val result = Region.selectAll().first()
            assertThat(result.id).isEqualTo(peru.id)
        }
    }

    @Test
    fun `Test inverse relation in nullable relations`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val trujillo = Region.new { name = "Trujillo"; country = peru }

        School.new { name = "School 1"; region = lima }
        School.new { name = "School 2"; region = lima; secondaryRegion = null }
        School.new { name = "School 3"; region = lima; secondaryRegion = trujillo }

        transaction {
            val result = Country.objects.filter {
                regions.schoolsSecondary.name eq "School 3"
            }.distinct().count()
            assertThat(result).isEqualTo(1)
        }
    }

}