package com.example

import io.ktor.server.application.*
import org.litote.kmongo.KMongo

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val database = KMongo.createClient().getDatabase("myDatabase")
    val userService = UserService(database)

    configureSockets()
    configureSerialization()
    configureDatabases()
    configureSecurity(userService)
    configureRouting(userService)
}
