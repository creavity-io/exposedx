package io.creavity.exposedx.dao

import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.manager.new
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityManyToOneTest {

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
    fun `Test that we can use id column of manager in querys without inner join`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        Region.new { name = "Lima"; country = peru }

        Region.objects.filter { country.id eq 1 }

        transaction {
            val result = Region.selectAll().first()
            assertThat(result[Region.country.id]).isEqualTo(peru.id)
        }
    }

    @Test
    fun `Test simple inner join`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        Region.new { name = "Lima"; country = peru }

        val result = Region.objects.filter { country.name eq "Peru" }.count()
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `Test double inner join`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val trujillo = Region.new { name = "Trujillo"; country = peru }

        School.new { name = "School 1"; region = lima }
        School.new { name = "School 2"; region = lima }

        val result = School.objects.filter { region.country.name like "Peru" }.count()
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `Test doble query sql`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val trujillo = Region.new { name = "Trujillo"; country = peru }

        School.new { name = "School 1"; region = lima }
        School.new { name = "School 2"; region = lima }

        transaction {
            School.objects.filter { region.country.name eq "Peru" }.all()
            val sql = "SELECT SCHOOL.ID, SCHOOL.\"NAME\", SCHOOL.REGION_ID, SCHOOL.SECONDARY_REGION_ID" +
                    " FROM SCHOOL INNER JOIN REGION region_id_Region" +
                    " ON SCHOOL.REGION_ID = region_id_Region.ID" +
                    " INNER JOIN COUNTRY country_Country" +
                    " ON region_id_Region.COUNTRY = country_Country.ID" +
                    " WHERE country_Country.\"NAME\" = ?"
            verify(exactly = 1) { this@EntityManyToOneTest.connection.prepareStatement(sql, any<Int>()) }

        }
    }

    @Test
    fun `Test with nullable relations`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val trujillo = Region.new { name = "Trujillo"; country = peru }

        School.new { name = "School 1"; region = lima }
        School.new { name = "School 2"; region = lima; secondaryRegion = null }
        School.new { name = "School 2"; region = lima; secondaryRegion = trujillo }

        School.objects.filter { secondaryRegion.id.isNull() }.count().also {
            assertThat(it).isEqualTo(2)
        }

        School.objects.filter { region.id eq lima.id and secondaryRegion.id.isNull() }.count().also {
            assertThat(it).isEqualTo(2)
        }
    }

}