package com.projectronin.interop.soda

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import oracle.soda.OracleCollection
import oracle.soda.OracleDatabase
import oracle.soda.OracleDocument
import oracle.soda.OracleOperationBuilder
import oracle.soda.rdbms.OracleRDBMSClient
import java.sql.DriverManager
import java.util.Properties

fun main() {
    val demo = SodaDemo()
    val collection = demo.getCollection("patient")
    println("Opened patient collection")

    println("Total patients: ${collection.find().count()}")

    val newPatient = rcdmPatient("sodatest") {
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
}

class SodaDemo {
    private val database: OracleDatabase

    init {
        val url =
            "jdbc:oracle:thin:@my_atp_low_tls?TNS_ADMIN=/tmp/scratch/tls_wallet/"

        val props = Properties()
        props.setProperty("user", "admin")
        props.setProperty("password", "Longpassword1")

        database = run {
            val connection = DriverManager.getConnection(url, props)
            val client = OracleRDBMSClient()
            client.getDatabase(connection)
        }
    }

    fun getCollection(name: String): OracleCollection = database.admin().createCollection(name)

    fun insertData(data: Any, collection: OracleCollection) {
        val document = database.createDocumentFrom(JacksonManager.objectMapper.writeValueAsString(data))
        collection.insert(document)
    }

    fun queryForOne(query: String, collection: OracleCollection): OracleDocument {
        println(query)
        return collection.find().filter(query).one
    }

    fun query(query: String, collection: OracleCollection): List<OracleDocument> {
        return collection.find().filter(query).all()
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
