package io.creavity.exposedx.managers

import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityTable
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.tables.new
import io.mockk.clearMocks
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager

open class UserTableAM: IntEntityTable<UserAM, UserTableAM>() {
    val name by varchar("name", 255)
}

class UserAM: IntEntity() {
    companion object Table: UserTableAM()
    var name by Table.name
}

open class CountryTableAM: IntAuditableTable<CountryAM, CountryTableAM, Int, UserAM, UserTableAM>(userTable = UserAM) {
    val name by varchar("name", 255)
}

class CountryAM: IntEntity(), IAuditableEntity<Int, UserAM>  {
    companion object Table: CountryTableAM()
    var name by Table.name
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestAuditManager {
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
            SchemaUtils.create(UserAM, CountryAM)
        }
        clearMocks(connection)
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(CountryAM, UserAM)
        }
    }

    @Test
    fun `Test create object with created by`() {
        val peru = CountryAM()
        peru.createdBy = UserAM.new { name = "Juan Perez" }
        peru.modifiedBy = peru.createdBy
        peru.name = "Peru"
        peru.save()
        assertThat(CountryAM.objects.count()).isEqualTo(1)
    }


    @Test
    fun `Test related filter`() {

        val userA = UserAM.new { name = "Juan Perez" }
        val userB = UserAM.new { name = "David Perez" }
        CountryAM.new { name = "Countr"; createdBy = userA; modifiedBy=userB }
    }

}

