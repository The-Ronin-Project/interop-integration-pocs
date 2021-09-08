package com.projectronin.integration.demo

import com.github.database.rider.core.api.configuration.DBUnit
import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.DBUnitExtension
import com.github.database.rider.junit5.api.DBRider
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.Before
import org.junit.BeforeClass
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

@Testcontainers
@DBRider
@DataSet("dbunit/example.yaml")
class DBTest {
    companion object {
        @Container
        private val postgreSQL = PostgreSQLContainer<Nothing>("postgres:latest")

        lateinit private var connection : Connection

        @JvmStatic
        @BeforeAll
        fun liquibaseSetup() {
            println("Setting up liquibase")
            val connectionProps = Properties()
            connectionProps["user"] = postgreSQL.username
            connectionProps["password"] = postgreSQL.password

            connection = DriverManager.getConnection(postgreSQL.jdbcUrl, connectionProps)
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database)
            liquibase.update(Contexts())
            println("Liquibase setup complete")
        }
    }

    val connectionHolder = ConnectionHolder { connection }

    @Test
    fun example() {
        println(DBTest::class.java.getResource("/dbunit.yml")?.path)

        val ktormDatabase =
            Database.connect(postgreSQL.jdbcUrl, user = postgreSQL.username, password = postgreSQL.password)

        println("Inserting names")
        ktormDatabase.insert(Example) {
            set(it.name, "Name 1")
        }
        ktormDatabase.insert(Example) {
            set(it.name, "Name 2")
        }

        println("Retrieving and printing names")
        for (row in ktormDatabase.from(Example).select()) {
            println(row[Example.name])
        }
    }

    @Test
    fun example2() {
        val ktormDatabase =
            Database.connect(postgreSQL.jdbcUrl, user = postgreSQL.username, password = postgreSQL.password)

        println("Retrieving and printing names")
        for (row in ktormDatabase.from(Example).select()) {
            println(row[Example.name])
        }
    }
}

object Example : Table<Nothing>("example") {
    val id = int("id").primaryKey()
    val name = varchar("name")
}