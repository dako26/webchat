package com.example

import com.mongodb.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureDatabases() {
    val mongoDatabase = connectToMongoDB()
    val users = UserService(mongoDatabase)
    val telegrams = TelegramService(mongoDatabase)

    routing {
        post("/telegram") {
            val telegram = call.receive<Telegram>()
            telegrams.saveMessage(telegram)
            call.respond(HttpStatusCode.Created, mapOf("sender" to telegram.sender, "message" to telegram.message))
        }
        get("/telegram") {
            val allTelegrams = telegrams.getAllMessages()
            call.respond(HttpStatusCode.OK, allTelegrams)
        }
        // Create user
        post("/users") {
            val user = call.receive<Users>()
            val id = users.create(user)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }


        // Read user
        get("/users/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
            users.read(id)?.let { user ->
                call.respond(user)
            } ?: call.respond(HttpStatusCode.NotFound, "User not found")
        }

        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing ID")
            val user = call.receive<Users>()
            val updatedUser = users.update(id, user)
            if (updatedUser != null) {
                call.respond(HttpStatusCode.OK, "User updated successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }

        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
            val deletedUser = users.delete(id)
            if (deletedUser != null) {
                call.respond(HttpStatusCode.OK, "User deleted successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }
    }
}

fun Application.connectToMongoDB(): MongoDatabase {
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "myDatabase"

    val credentials = user?.let { u -> password?.let { p -> "$u:$p@" } }.orEmpty()
    val connectionString = "mongodb://$credentials$host:$port/$databaseName"

    val settings = com.mongodb.MongoClientSettings.builder()
        .applyConnectionString(com.mongodb.ConnectionString(connectionString))
        .build()

    val mongoClient = MongoClients.create(settings)
    val database = mongoClient.getDatabase(databaseName)

    monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return database
}
