package com.example

import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import java.util.concurrent.ConcurrentHashMap
import javax.naming.AuthenticationException

fun Application.configureSockets() {
    install(WebSockets)

    val telegramService = TelegramService(this.connectToMongoDB())
    val connections = ConcurrentHashMap.newKeySet<DefaultWebSocketSession>()

    suspend fun DefaultWebSocketSession.handleWebSocket(username :String) {
        // Handle WebSocket communication here
        connections.add(this)
        try {
            send("Connected to chat!")
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val receivedText = frame.readText()
                        val json = Json.decodeFromString<Map<String, String>>(receivedText)
                        val telegram = Telegram(json["sender"], json["message"])
                        telegramService.saveMessage(telegram)
                        connections.forEach({ it.send("${telegram.sender}: ${telegram.message}") })
                    }

                    is Frame.Binary -> {
                        val recivedBytes = frame.readBytes()
                        send("Recevied binary data of size: ${recivedBytes.size} bytes")
                    }

                    is Frame.Close -> {
                        close(CloseReason(CloseReason.Codes.NORMAL, "close ws. Goodbye"))
                    }

                    is Frame.Ping -> {
                        send(Frame.Pong(frame.buffer))
                    }

                    is Frame.Pong -> {
                        println("Received Pong frame")
                    }

                    else -> {
                        println("Received an unknown frame type")
                    }
                }
            }
        } finally {
            connections.remove(this)
        }
    }
            routing {
        authenticate {
            webSocket("/ws") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
                    return@webSocket
                }
                val username= session.username
                handleWebSocket(username)
            }

        }

    }
}
