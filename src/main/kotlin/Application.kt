package com.example

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)

}


fun Application.module() {
    val database = connectToMongoDB()
    val userService = UserService(database)

    configureSecurity(userService)
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureRouting(userService)
}
