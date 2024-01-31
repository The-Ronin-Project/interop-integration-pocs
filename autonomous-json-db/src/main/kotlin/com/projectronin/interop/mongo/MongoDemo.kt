package com.projectronin.interop.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import org.bson.Document

fun main() {
    val demo = MongoDemo()
    val collection = demo.getCollection("new-patient")
    println("Opened patient collection")

    println("Total patients: ${collection.find().count()}")

    val newPatient =
        rcdmPatient("sodatest") {
            identifier.generates(6)
        }
    println("Inserting new patient: $newPatient")
    demo.insertData(newPatient, collection)

    println("Total patients: ${collection.find().count()}")
}

@Suppress("ktlint:standard:max-line-length")
class MongoDemo {
    private val mongoDatabase: MongoDatabase

    init {
        val settings =
            MongoClientSettings.builder()
//                .credential(MongoCredential.createCredential("admin", "admin", "Longpassword1".toCharArray()))
//                .applyToClusterSettings { it.hosts(listOf(ServerAddress("localhost"))) }
//                .readPreference(ReadPreference.primaryPreferred())
//                .retryWrites(true)
//                .applyToSslSettings { it.enabled(true).invalidHostNameAllowed(true) }
                .applyConnectionString(
                    ConnectionString(
                        "mongodb://admin:Longpassword1@localhost:27017/admin?authMechanism=PLAIN&authSource=\$external&ssl=true&retryWrites=false&loadBalanced=true",
                    ),
                )
                .build()
        val mongoClient = MongoClients.create(settings)
        mongoDatabase = mongoClient.getDatabase("admin")
    }

    fun getCollection(name: String): MongoCollection<Document> = mongoDatabase.getCollection(name)

    fun insertData(
        data: Any,
        collection: MongoCollection<Document>,
    ) {
        val document = Document.parse(JacksonManager.objectMapper.writeValueAsString(data))
        collection.insertOne(document)
    }
}
