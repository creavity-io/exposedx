package io.creavity.exposedx.dao

import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.tables.new
import io.creavity.exposedx.dao.queryset.first
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityOneToOptionalTest {

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
            SchemaUtils.create(Manager, Company, ContactInfo)
        }
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(Manager, Company, ContactInfo)
        }
    }

    @Test
    fun `Test access related one to optional after save`() {

        val manager = Manager.new { name = "Juan"; country = Country.new  { name="Peru" }  }
        val contactInfo = ContactInfo.new { phone = "+1 123 123 123"; address = "street"; country = Country.new  { name="Peru" }  }
        val company = Company.new { name = "Acme"; this.manager = manager; this.contactInfo = contactInfo }

        assertThat(company.contactInfo!!.address).isEqualTo("street")
    }

    @Test
    fun `Test query all objects that has optionals`() {

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

        assertThat(Company.objects.all().toList().count()).isEqualTo(3)
    }

    @Test
    fun `Test access related one to optional with new query`() {

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


        val newCompany = Company.objects.get(company.id)!!

        assertThat(newCompany.contactInfo!!.address).isEqualTo("street")
    }


    @Test
    fun `Test filter by one to optional related`() {

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


        transaction {
            val result = Company.objects.filter { this.contactInfo.address like "str%" }.all().toList()
            assertThat(result.count()).isEqualTo(1)
            val resultCompany = result.first()
            //val result = Region.selectAll().first()
            assertThat(resultCompany.name).isEqualTo(company.name)
        }
    }

    @Test
    fun `Test filter by one to optional with id`() {

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


        transaction {
            val result = Company.objects.filter { this.contactInfo.id eq manager.id }.all().toList()
            assertThat(result.count()).isEqualTo(1)
            val resultCompany = result.first()
            //val result = Region.selectAll().first()
            assertThat(resultCompany.name).isEqualTo(company.name)
        }
    }
    @Test
    fun `Test filter by one to one with is null`() {

        val peru = Country.new { name="Peru" }
        val argentina = Country.new { name="Argentina" }

        val manager = Manager.new { name = "Juan"; country = peru  }
        val contactInfo = ContactInfo.new { phone = "+1 9999 9999 999"; address = "street"; country = peru  }
        val company = Company.new { name = "Acme"; this.manager = manager; this.contactInfo = contactInfo }

        val manager3 = Manager.new { name = "Lucas"; country = argentina }
        val company3 = Company.new { name = "Earth"; this.manager = manager3; this.contactInfo = null }

        val manager4 = Manager.new { name = "Lucas"; country = argentina }
        val company4 = Company.new { name = "Earth"; this.manager = manager4; this.contactInfo = null }

        val manager5 = Manager.new { name = "Lucas"; country = argentina }
        val company5 = Company.new { name = "Earth"; this.manager = manager5; this.contactInfo = null }


        val orphanContactInfo = ContactInfo.new { phone = "+0 000 000 000"; address = "neverland"; country = peru }


        transaction {
            val result = Company.objects.filter { this.contactInfo.id.isNull() }.all().toList()
            assertThat(result.count()).isEqualTo(3)
        }
    }

    @Test
    fun `Test filter by deep one to optional`() {

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


        transaction {
            val result = Company.objects.filter { this.contactInfo.country.name like "Per%" }.all().toList()
            assertThat(result.count()).isEqualTo(1)
            val resultCompany = result.first()
            //val result = Region.selectAll().first()
            assertThat(resultCompany.name).isEqualTo(company.name)
        }
    }

    @Test
    fun `Test inverse relation in one to optional`() {
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

        val newContactInfo = ContactInfo.objects.get(contactInfo.id)!!

        transaction {
            val resultCompany = newContactInfo.company
            assertThat(resultCompany!!.name).isEqualTo(company.name)
        }
    }

    @Test
    fun `Test inverse relation null`() {
        val peru = Country.new { name="Peru" }
        val orphanContactInfo = ContactInfo.new { phone = "+0 000 000 000"; address = "neverland"; country = peru }

        transaction {
            val resultCompany = orphanContactInfo.company
            assertThat(resultCompany).isEqualTo(null)
        }
    }

    @Test
    fun `Test filter by inverse relation in one to optional`() {
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

        transaction {
            val result = ContactInfo.objects.filter { this.company.name like "Ac%" }.all().toList()
            assertThat(result.count()).isEqualTo(1)
            val resultContactInfo = result.first()
            assertThat(resultContactInfo.address).isEqualTo("street")
        }
    }

}