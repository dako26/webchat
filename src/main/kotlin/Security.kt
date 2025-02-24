package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json


fun Application.configureSecurity(userService: UserService) {
    install(Sessions) {
        cookie<UserSession>("user_session") {
        }
    }
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true })
    }
    install(Authentication) {
        session<UserSession> {
            validate { session ->
                userService.findByUsername(session.username)?.let { UserIdPrincipal(it.username) }
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, "Session expired. Please log in again.")
            }
        }
    }
}
