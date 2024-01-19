package com.projectronin.interop.soda

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import oracle.soda.OracleCollection
import oracle.soda.OracleDatabase
import oracle.soda.OracleDocument
import oracle.soda.rdbms.OracleRDBMSClient
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Repository
import javax.sql.DataSource

fun main() {
    runApplication<SpringBootSodaDemo>()
}

@SpringBootApplication
@Suppress("ktlint:standard:max-line-length")
class SpringBootSodaDemo {
    private val urlWithWallet = "jdbc:oracle:thin:@my_atp_low?TNS_ADMIN=/tmp/tls_wallet/"
    private val urlWithoutWallet =
        "jdbc:oracle:thin:@(description=(retry_count=0)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=localhost))(connect_data=(service_name=my_atp_low.adb.oraclecloud.com))(security=(ssl_server_dn_match=no)))"

    private val url = urlWithWallet

    @Bean
    fun dataSource(): DataSource =
        DataSourceBuilder.create()
            .driverClassName("oracle.jdbc.OracleDriver")
            .url(url)
            .username("admin")
            .password("Longpassword1")
            .build()

    @Bean
    fun run(repository: SodaRepository) =
        CommandLineRunner {
            val collection = repository.getCollection("patient")
            println("Opened patient collection")

            println("Total patients: ${collection.find().count()}")

            val newPatient =
                rcdmPatient("sodatest") {
                    identifier.generates(6)
                }
            println("Inserting new patient: $newPatient")
            repository.insertData(newPatient, collection)

            println("Total patients: ${collection.find().count()}")

            val randomIdentifier = newPatient.identifier.random()
            val query =
                """{"identifier[*]":{ "system":"${randomIdentifier.system!!.value}", "value":"${randomIdentifier.value!!.value}"} }"""
            val found = repository.queryForOne(query, collection)
            println("Query for one found (key = ${found.key}) - ${found.contentAsString}")

            val allFound = repository.query(query, collection)
            println("Query found ${allFound.size}")
            allFound.forEach {
                println("${it.key} - ${it.contentAsString}")
            }

            val sqlResult = repository.sqlFindById(newPatient.id!!.value!!, "patient")
            println("SQL Lookup found $sqlResult")
        }
}

@Repository
class SodaRepository(private val dataSource: DataSource) {
    private fun database(): OracleDatabase {
        val client = OracleRDBMSClient()
        return client.getDatabase(dataSource.connection)
    }

    fun getCollection(name: String): OracleCollection = database().admin().createCollection(name)

    fun insertData(
        data: Any,
        collection: OracleCollection,
    ) {
        val document = database().createDocumentFrom(JacksonManager.objectMapper.writeValueAsString(data))
        collection.insert(document)
    }

    fun queryForOne(
        query: String,
        collection: OracleCollection,
    ): OracleDocument {
        println(query)
        return collection.find().filter(query).one
    }

    fun query(
        query: String,
        collection: OracleCollection,
    ): List<OracleDocument> {
        return collection.find().filter(query).all()
    }

    fun sqlFindById(
        id: String,
        collection: String,
    ): String? {
        return dataSource.connection.prepareStatement(
            "SELECT json_serialize(c.json_document) FROM $collection c WHERE c.json_document.id = :1",
        )
            .use { preparedStatement ->
                preparedStatement.setString(1, id)
                preparedStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getString(1)
                    } else {
                        null
                    }
                }
            }
    }
}
