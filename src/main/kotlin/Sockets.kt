package com.example

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import com.example.WebSocketFrameHandlers
import com.example.DefaultWebSocketFrameHandler
fun Application.configureSockets() {
    install(WebSockets)

    val telegramService = TelegramService(Database.instance)
    val connections = ConcurrentHashMap.newKeySet<DefaultWebSocketSession>()
    val logger = LoggerFactory.getLogger("WebSocketServer")
    val frameHandler: WebSocketFrameHandlers=DefaultWebSocketFrameHandler(telegramService,connections,logger)

    suspend fun DefaultWebSocketSession.handleWebSocket(username: String) {
        // Handle WebSocket communication here
        connections.add(this)
        try {
            send("Connected to chat!")
            logger.info("User $username connected")
            val allMessage = telegramService.getAllMessages()
            allMessage.forEach { message -> send("${message.sender} : ${message.message}") }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> frameHandler.handleText(this ,frame,username)
                    is Frame.Close -> frameHandler.handleClose(this,frame,username)
                    else -> frameHandler.handleOther(this,frame,username)

                }
            }
            logger.warn("User $username crashed or lost connection")
            close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server detected client crash"))
            connections.forEach { it.send("$username has disconnected unexpectedly") }
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
