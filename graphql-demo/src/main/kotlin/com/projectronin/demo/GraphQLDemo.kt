package com.projectronin.demo

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@SpringBootApplication
class GraphQLDemo

fun main(args: Array<String>) {
    runApplication<GraphQLDemo>(*args)
}

@Component
class MessageQuery(val service: MessageService) : Query {
    fun messages(id: String? = null): List<Message> {
        id?.let {
            val message = service.findById(id)
            return message?.let { listOf(message) } ?: listOf()
        }

        return service.findMessages().toList()
    }
}

@Component
class MessageMutation(val service: MessageService) : Mutation {
    fun addMessage(message: Message): Message = service.post(message)
}

@Table("MESSAGES")
data class Message(@Id val id: String?, val text: String)

interface MessageRepository : CrudRepository<Message, String>

@Service
class MessageService(val db: MessageRepository) {
    fun findById(id: String): Message? = db.findById(id).orElse(null)

    fun findMessages(): Iterable<Message> = db.findAll()

    fun post(message: Message) : Message = db.save(message)
}