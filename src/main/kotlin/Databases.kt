package com.example

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureDatabases() {
    val mongoDatabase = Database.instance
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
    }
}
object Database{
    private lateinit var  mongoClient: com.mongodb.client.MongoClient
    lateinit var instance :MongoDatabase
        private set

    fun initialize(environment: ApplicationEnvironment){
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

        mongoClient = MongoClients.create(settings)
        instance = mongoClient.getDatabase(databaseName)

        environment.monitor.subscribe(ApplicationStopped) {
            mongoClient.close()
        }
    }
}