package com.example

import com.mongodb.client.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.bson.Document
import org.mindrot.jbcrypt.BCrypt
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.sessions.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


fun Application.configureSecurity(userService: UserService) {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true })  // Install serialization
    }
    authentication {
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
