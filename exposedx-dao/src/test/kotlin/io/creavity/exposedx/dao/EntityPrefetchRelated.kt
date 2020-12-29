package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.tables.new
import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityPrefetchRelated {
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
            SchemaUtils.create(Country, Region, School, Manager, Company, ContactInfo)
        }
        clearMocks(connection)
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(Country, Region, School, Manager, Company, ContactInfo)
        }
    }

    fun assertNQuerys(n: Int, transaction: () -> Unit) {
        clearMocks(connection)
        transaction {
            transaction()
            verify(exactly = n) { this@EntityPrefetchRelated.connection.prepareStatement(any(), any<Int>()) }
        }
    }

    @Test
    fun `Test count of query of prefetch related `() {
        val c1 = Country.new  { name="Country" }
        val r = Region.new { name = "Region"; country = c1}
        val r2 = Region.new { name = "Region 2"; country = c1}
        School.new { name = "School"; region = r }
        School.new { name = "School"; region = r }
        School.new { name = "School"; region = r2 }
        School.new { name = "School"; region = r2 }


        var schools: Iterable<School> = listOf()

        assertNQuerys(3) {
            schools = School.objects.prefetchRelated(School.region, School.region.country).all()
        }

        assertNQuerys(0) {
            Assertions.assertThat(schools.first().region.country.name).isEqualTo("Country") // use cached objects
        }
    }


    @Test
    fun `Test count of query of prefetch related with one to one `() {
        val peru = Country.new { name="Peru" }
        val argentina = Country.new { name="Argentina" }

        val manager = Manager.new { name = "Juan"; country = peru  }
        val contactInfo = ContactInfo.new { phone = "+1 9999 9999 999"; address = "street"; country = peru  }
        val company = Company.new { name = "Acme"; this.manager = manager; this.contactInfo = contactInfo }

        val manager2 = Manager.new { name = "Pablo"; country = argentina }
        val contactInfo2 = ContactInfo.new { phone = "+1 123 123 123"; address = "house"; country = argentina  }
        val company2 = Company.new { name = "Moon"; this.manager = manager2; this.contactInfo = contactInfo2 }

        val manager3 = Manager.new { name = "Lucas"; country = argentina }
        val company3 = Company.new { name = "Earth"; this.manager = manager3; this.contactInfo = null }

        val orphanContactInfo = ContactInfo.new { phone = "+0 000 000 000"; address = "neverland"; country = peru }

        var companies = listOf<Company>()

        assertNQuerys(3) {
            companies = Company.objects.prefetchRelated(Company.manager.country, School.region.country).all().toList()
        }

        assertNQuerys(0) {
            Assertions.assertThat(companies.first().manager.country.name).isEqualTo("Peru") // use cached objects
        }
    }

    @Test
    fun `Test count of query of prefetch related with one to optional `() {
        val peru = Country.new { name="Peru" }
        val argentina = Country.new { name="Argentina" }

        val manager = Manager.new { name = "Juan"; country = peru  }
        val contactInfo = ContactInfo.new { phone = "+1 9999 9999 999"; address = "street"; country = peru  }
        val company = Company.new { name = "Acme"; this.manager = manager; this.contactInfo = contactInfo }

        val manager2 = Manager.new { name = "Pablo"; country = argentina }
        val contactInfo2 = ContactInfo.new { phone = "+1 123 123 123"; address = "house"; country = argentina  }
        val company2 = Company.new { name = "Moon"; this.manager = manager2; this.contactInfo = contactInfo2 }

        val manager3 = Manager.new { name = "Lucas"; country = argentina }
        val company3 = Company.new { name = "Earth"; this.manager = manager3; this.contactInfo = null }

        val orphanContactInfo = ContactInfo.new { phone = "+0 000 000 000"; address = "neverland"; country = peru }

        var companies = listOf<Company>()

        assertNQuerys(3) {
            companies = Company.objects.prefetchRelated(Company.contactInfo.country, School.region.country).all().toList()
        }

        assertNQuerys(0) {
            Assertions.assertThat(companies[0].contactInfo?.country?.name).isEqualTo("Peru") // use cached objects
            Assertions.assertThat(companies[1].contactInfo?.country?.name).isEqualTo("Argentina") // use cached objects
            Assertions.assertThat(companies[2].contactInfo).isEqualTo(null) // use cached objects
        }
    }


    @Test
    fun `Test count of query of prefetch related inverse `() {
        val c1 = Country.new  { name="Country" }
        val r = Region.new { name = "Region"; country = c1}
        val r2 = Region.new { name = "Region 2"; country = c1}
        School.new { name = "School"; region = r }
        School.new { name = "School"; region = r }
        School.new { name = "School"; region = r2 }
        School.new { name = "School"; region = r2 }


        var countries = listOf<Country>()

        assertNQuerys(3) {
            countries = Country.objects.prefetchRelated(Country.regions.schools).all().toList()
        }

        assertNQuerys(0) {
            Assertions.assertThat(countries[0].regions.all().count()).isEqualTo(2) // use cached objects
        }
    }


    @Test
    fun `Test count of query of prefetch related inverse with one to one`() {
        val peru = Country.new { name="Peru" }
        val argentina = Country.new { name="Argentina" }

        val manager = Manager.new { name = "Juan"; country = peru  }
        val contactInfo = ContactInfo.new { phone = "+1 9999 9999 999"; address = "street"; country = peru  }
        val company = Company.new { name = "Acme"; this.manager = manager; this.contactInfo = contactInfo }

        val manager2 = Manager.new { name = "Pablo"; country = argentina }
        val contactInfo2 = ContactInfo.new { phone = "+1 123 123 123"; address = "house"; country = argentina  }
        val company2 = Company.new { name = "Moon"; this.manager = manager2; this.contactInfo = contactInfo2 }

        val manager3 = Manager.new { name = "Lucas"; country = argentina }
        val company3 = Company.new { name = "Earth"; this.manager = manager3; this.contactInfo = null }

        val orphanContactInfo = ContactInfo.new { phone = "+0 000 000 000"; address = "neverland"; country = peru }

        var managers = listOf<Manager>()

        assertNQuerys(2) {
            managers = Manager.objects.prefetchRelated(Manager.company).all().toList()
        }

        assertNQuerys(0) {
            Assertions.assertThat(managers.first().company.name).isEqualTo("Peru") // use cached objects
        }
    }

}