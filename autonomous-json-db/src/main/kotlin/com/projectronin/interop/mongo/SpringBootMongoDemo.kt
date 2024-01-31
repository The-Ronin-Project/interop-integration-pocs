package com.projectronin.interop.mongo

import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.MongoRepository
import javax.sql.DataSource

@Suppress("ktlint:standard:max-line-length")
fun main() {
    val mongoUrl =
        "mongodb://admin:Longpassword1@localhost:27017/admin?authMechanism=PLAIN&authSource=\$external&ssl=true&retryWrites=false&loadBalanced=true"
    println("MONGO URL $mongoUrl")

    val application = SpringApplication(SpringBootMongoDemo::class.java)
    application.setDefaultProperties(mapOf("spring.data.mongodb.uri" to mongoUrl))
    application.run()
}

@SpringBootApplication
@Suppress("ktlint:standard:max-line-length")
class SpringBootMongoDemo {
    // We have to define a DataSource because we brought in JDBC
    @Bean
    fun dataSource(): DataSource =
        DataSourceBuilder.create()
            .driverClassName("oracle.jdbc.OracleDriver")
            .url("jdbc:oracle:thin:@my_atp_low?TNS_ADMIN=/tmp/tls_wallet/")
            .username("admin")
            .password("Longpassword1")
            .build()

    @Bean
    fun run(repository: MongoRepo) =
        CommandLineRunner {
            println("Total patients: ${repository.count()}")

            val newPatient =
                rcdmPatient("sodatest") {
                    identifier.generates(6)
                }
            println("Inserting new patient: $newPatient")
            repository.insert(newPatient)

            println("Total patients: ${repository.count()}")

//            val randomIdentifier = newPatient.identifier.random()
//            val query =
//                """{"identifier[*]":{ "system":"${randomIdentifier.system!!.value}", "value":"${randomIdentifier.value!!.value}"} }"""
//            val found = repository.queryForOne(query, collection)
//            println("Query for one found (key = ${found.key}) - ${found.contentAsString}")
//
//            val allFound = repository.query(query, collection)
//            println("Query found ${allFound.size}")
//            allFound.forEach {
//                println("${it.key} - ${it.contentAsString}")
//            }
//
//            val sqlResult = repository.sqlFindById(newPatient.id!!.value!!, "patient")
//            println("SQL Lookup found $sqlResult")
        }
}

interface MongoRepo : MongoRepository<Patient, String>
