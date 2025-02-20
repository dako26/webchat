package com.example

import com.mongodb.client.*
import io.ktor.http.*
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.server.html.*
import kotlinx.html.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import org.mindrot.jbcrypt.BCrypt
import kotlinx.html.dom.createHTMLDocument
import java.io.File
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds


fun Application.configureRouting(userService: UserService) {
    routing {
        get("/") {
            call.respondHtml {
                head {
                    title { +"welcome" }
                }
                body {
                    h1 { +"welcome" }
                    br()
                    br()
                    br()

                    p {
                        h2 {
                            a(href = "/login") { +"Login" }
                        }
                    }
                    br()
                    p {
                        h2 {
                            a(href = "/register") { +"Register" }
                        }
                    }


                }
            }
        }
        // Get login page
        get("/login") {
            call.respondHtml {
                head {
                    title("Login")
                }
                body {
                    h1 { +"Login" }
                    form(action = "/login", method = FormMethod.post) {
                        p {
                            +"Username: "
                            textInput { name = "username" }
                        }
                        p {
                            +"Password: "
                            passwordInput { name = "password" }
                        }
                        p {
                            submitInput { value = "Login" }
                        }
                    }
                }
            }
        }

        // Handle login POST request
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing username")
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing password")

            // Retrieve the user by username
            val user = userService.findByUsername(username)
            if (user != null && user.hashedPassword != null && BCrypt.checkpw(password, user.hashedPassword)) {
                val hashedInputPassword = BCrypt.hashpw(password, user.salt)
                // Set session and respond with success
                if (hashedInputPassword == user.hashedPassword) {
                    call.sessions.set(UserSession(user.username, true))
                    call.respondRedirect("/ws")
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            } else {
                // Respond with Unauthorized if invalid credentials
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }

        get("/register") {
            call.respondHtml {
                head {
                    title("Register")
                }
                body {
                    h1 { +"Register" }
                    form(action = "/register", method = FormMethod.post) {
                        p {
                            +"Username: "
                            textInput { name = "username" }
                        }
                        p {
                            +"Password: "
                            passwordInput { name = "password" }
                        }
                        p {
                            submitInput { value = "Register" }
                        }
                    }
                }
            }
        }

        // Registration POST handler
        post("/register") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing username")
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing password")

            // Check if the username already exists
            if (userService.findByUsername(username) != null) {
                return@post call.respond(HttpStatusCode.Conflict, "Username already exists")
            }

            // Hash the password before storing
            val user = Users(username, password)

            // Create the user
            userService.create(user)

            // Respond with success
            call.respond(HttpStatusCode.Created, "User registered successfully")

        }
        get("/ws") {
            val userSession = call.sessions.get<UserSession>()
            if (userSession == null || !userSession.login) {
                call.respondRedirect("/login")
                return@get
            }
            val file = File("src/main/resources/ws.html")
            if (file.exists()) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found")
            }
        }

    }
}



