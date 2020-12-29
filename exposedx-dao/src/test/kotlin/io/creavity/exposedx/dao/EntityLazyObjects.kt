package io.creavity.exposedx.dao

import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.tables.new
import io.creavity.exposedx.dao.queryset.first
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityLazyObjects {
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
    fun `Should can access id of a related object`() {

        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        val school = School.objects.first()
        assertThat(school.region.id.value).isEqualTo(1)

    }

    @Test
    fun `Should not make other query when access to id of related object`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        transaction {
            val school = School.objects.first()
            assertThat(school.region.id.value).isEqualTo(1)
            verify(exactly = 1) { this@EntityLazyObjects.connection.prepareStatement(any(), any<Int>()) }
        }

    }

    @Test
    fun `Should can access related properties with lazy way`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        val school = School.objects.first()
        assertThat(school.region.name).isEqualTo("Region")
        assertThat(school.region.country.name).isEqualTo("Country")

    }

    @Test
    fun `Should not query when object already cached`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        val school = School.objects.first()
        assertThat(school.region.name).isEqualTo("Region") // query region

        clearMocks(connection)
        transaction {
            school.region.name // should use cached object.
            verify(exactly = 0) { this@EntityLazyObjects.connection.prepareStatement(any(), any<Int>()) } // dont query anything
        }
    }

    @Test
    fun `Should make a one query by each related object`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

         transaction {
             val school = School.objects.first() // query school
             assertThat(school.region.name).isEqualTo("Region") // query region
             assertThat(school.region.country.name).isEqualTo("Country") // use cached region and query country
             verify(exactly = 3) { this@EntityLazyObjects.connection.prepareStatement(any(), any<Int>()) }

         }

    }

    @Test
    fun `Test lazy two objects`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }
        val school = School.objects.first()
        assertThat(school.region.country.name).isEqualTo("Country")
    }


    @Test
    fun `Test lazy related querys`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        transaction {
            val school = School.objects.first() //one
            Region.objects.all().iterator() // two
            School.objects.all().iterator() // three
            Country.objects.all().iterator() // four
            assertThat(school.region.country.name).isEqualTo("Country") // use cached objects.
            verify(exactly = 4) { this@EntityLazyObjects.connection.prepareStatement(any(), any<Int>()) }
        }
    }

}

