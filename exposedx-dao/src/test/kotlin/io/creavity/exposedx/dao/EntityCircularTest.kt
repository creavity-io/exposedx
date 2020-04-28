package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityManager
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.entities.manyToOptional
import io.creavity.exposedx.dao.entities.nullable
import io.creavity.exposedx.dao.entities.selfRelation
import io.creavity.exposedx.dao.manager.new
import io.mockk.clearMocks
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityCircularTest {


    open class UserTable: IntEntityManager<User, UserTable>() {
        val name by varchar("name", 255)
        val parent by selfRelation("parent")
    }

    class User: IntEntity() {
        companion object Table: UserTable()
        var name by Table.name
        var parent by Table.parent.nullable()
    }


    // val UserTable.childs by oneToMany(User.parent, User)
    // val User.child by User.childs.asList()


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
            SchemaUtils.create(User)
        }
        clearMocks(connection)
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(User)
        }
    }

    @Test
    fun `Create circular referencee`() {
        val parent = User.new  { name="Juan"; this.parent = null }
        val child1 = User.new { name = "Jorgito"; this.parent = parent }
        val child2 = User.new { name = "Anita"; this.parent = parent }

        clearMocks(connection)
        transaction {
            assertThat(User.objects.count()).isEqualTo(3)
        }
    }

    @Test
    fun `Filteer circular referencee`() {
        val parent = User.new  { name="Juan"; this.parent = null }
        val child1 = User.new { name = "Jorgito"; this.parent = parent }
        val child2 = User.new { name = "Anita"; this.parent = parent }

        clearMocks(connection)
        transaction {
            assertThat(User.objects.filter { this.parent.id.isNull() }.count()).isEqualTo(1)
            assertThat(User.objects.filter { not(this.parent.id.isNull()) }.count()).isEqualTo(2)
        }
    }
}



