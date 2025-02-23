package com.example

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

fun Application.configureSockets() {
    install(WebSockets)

    val telegramService = TelegramService(this.connectToMongoDB())
    val connections = ConcurrentHashMap.newKeySet<DefaultWebSocketSession>()
    val logger = LoggerFactory.getLogger("WebSocketServer")

    suspend fun DefaultWebSocketSession.handleWebSocket(username : String) {
        // Handle WebSocket communication here
        connections.add(this)
        try {
            send("Connected to chat!")
            logger.info("User $username connected")
            val allMessage = telegramService.getAllMessages()
            allMessage.forEach {message -> send("${message.sender} : ${message.message}")}
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val receivedText = frame.readText()
                        val json = Json.decodeFromString<Map<String, String>>(receivedText)
                        val telegram = Telegram(username, json["message"])
                        telegramService.saveMessage(telegram)
                        connections.forEach({ it.send("${telegram.sender}: ${telegram.message}") })
                    }

                    is Frame.Binary -> {
                        val receivedBytes = frame.readBytes()
                        send("Recevied binary data of size: ${receivedBytes.size} bytes")
                    }

                    is Frame.Close -> {
                        val closeReason = frame.readReason() ?: CloseReason(CloseReason.Codes.NORMAL, "Client closed")
                        logger.info("User $username closed connection: ${closeReason.code} - ${closeReason.message}")
                        close(closeReason)
                        connections.forEach({ it.send("$username has left the chat") })
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
            logger.warn("User $username crashed or lost connection")
            close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server detected client crash"))
            connections.forEach({ it.send("$username has disconnected unexpectedly") })
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
                handleWebSocket(session.username)
            }
        }
    }
}
