package com.example


import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap


interface WebSocketFrameHandlers {
    suspend fun handleText(session: DefaultWebSocketSession, frame: Frame.Text, username: String)
    suspend fun handleClose(session: DefaultWebSocketSession, frame: Frame.Close, username: String)
    suspend fun handleOther(session: DefaultWebSocketSession, frame: Frame, username: String)
}

class DefaultWebSocketFrameHandler(
    private val telegramService: TelegramService,
    private val connections: ConcurrentHashMap.KeySetView<DefaultWebSocketSession, Boolean>,
    private val logger: Logger
) : WebSocketFrameHandlers {
    override suspend fun handleText(session: DefaultWebSocketSession, frame: Frame.Text, username: String) {
        val receivedText = frame.readText()
        val json = Json.decodeFromString<Map<String, String>>(receivedText)
        val messageText = json["message"] ?: run {
            logger.warn("Received message without 'message' field from $username:$receivedText")
            return
        }
        val telegram = Telegram(username, messageText)
        withContext(Dispatchers.IO) {
            telegramService.saveMessage(telegram)
        }
        connections.forEach { it.send("${telegram.sender}: ${telegram.message}") }
    }

    override suspend fun handleClose(session: DefaultWebSocketSession, frame: Frame.Close, username: String) {
        val closeReason = frame.readReason() ?: CloseReason(CloseReason.Codes.NORMAL, "Client close")
        logger.info("User $username closed Connection: ${closeReason.code} - ${closeReason.message} ")
        session.close(closeReason)
        connections.forEach {
            it.send("$username has left")
        }
    }

    override suspend fun handleOther(session: DefaultWebSocketSession, frame: Frame, username: String) {
        logger.warn("Received unknown frame type from $username")
    }
}
