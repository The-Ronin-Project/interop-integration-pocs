package com.projectronin.interop.soda

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import oracle.soda.OracleCollection
import oracle.soda.OracleDatabase
import oracle.soda.OracleDocument
import oracle.soda.OracleOperationBuilder
import oracle.soda.rdbms.OracleRDBMSClient
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

fun main() {
    val demo = SodaDemo()
    val collection = demo.getCollection("patient")
    println("Opened patient collection")

    println("Total patients: ${collection.find().count()}")

    val newPatient =
        rcdmPatient("sodatest") {
            identifier.generates(6)
        }
    println("Inserting new patient: $newPatient")
    demo.insertData(newPatient, collection)

    println("Total patients: ${collection.find().count()}")

    val randomIdentifier = newPatient.identifier.random()
    val query =
        """{"identifier[*]":{ "system":"${randomIdentifier.system!!.value}", "value":"${randomIdentifier.value!!.value}"} }"""
    val found = demo.queryForOne(query, collection)
    println("Query for one found (key = ${found.key}) - ${found.contentAsString}")

    val allFound = demo.query(query, collection)
    println("Query found ${allFound.size}")
    allFound.forEach {
        println("${it.key} - ${it.contentAsString}")
    }

    val sqlResult = demo.sqlFindById(newPatient.id!!.value!!, "patient")
    println("SQL Lookup found $sqlResult")
}

@Suppress("ktlint:standard:max-line-length")
class SodaDemo {
    private val connection: Connection
    private val database: OracleDatabase

    init {
        val urlWithWallet = "jdbc:oracle:thin:@my_atp_low?TNS_ADMIN=/tmp/tls_wallet/"
        val urlWithoutWallet =
            "jdbc:oracle:thin:@(description=(retry_count=0)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=localhost))(connect_data=(service_name=my_atp_low.adb.oraclecloud.com))(security=(ssl_server_dn_match=no)))"

        val url = urlWithoutWallet

        val props = Properties()
        props.setProperty("user", "admin")
        props.setProperty("password", "Longpassword1")

        connection = DriverManager.getConnection(url, props)
        database =
            run {
                val client = OracleRDBMSClient()
                client.getDatabase(connection)
            }
    }

    fun getCollection(name: String): OracleCollection = database.admin().createCollection(name)

    fun insertData(
        data: Any,
        collection: OracleCollection,
    ) {
        val document = database.createDocumentFrom(JacksonManager.objectMapper.writeValueAsString(data))
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
        return connection.prepareStatement("SELECT json_serialize(c.json_document) FROM $collection c WHERE c.json_document.id = :1")
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

fun OracleOperationBuilder.all(): List<OracleDocument> {
    val cursor = cursor
    val documents = mutableListOf<OracleDocument>()
    while (cursor.hasNext()) {
        documents.add(cursor.next())
    }
    return documents
}
