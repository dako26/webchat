package com.example

import io.ktor.server.application.*
import javax.xml.crypto.Data

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)

}


fun Application.module() {
    Database.initialize(environment)
    val database = Database.instance
    val userService = UserService(database)

    configureSecurity(userService)
    configureSockets()
    configureDatabases()
    configureRouting(userService)
}
