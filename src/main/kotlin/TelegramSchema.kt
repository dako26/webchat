package com.example

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.bson.Document


@Serializable
data class Telegram(
    val sender: String,
    val message: String

) {
    fun toDocument(): Document {
        return Document("sender", sender)
            .append("message", message)
    }

    companion object {
        fun fromDocument(doc: Document): Telegram {
            val sender = doc.getString("sender")
            val message = doc.getString("message")
            return Telegram(sender, message)
        }
    }
}


class TelegramService(database: MongoDatabase) {
    private val collection: MongoCollection<Document> = database.getCollection("telegram")

    // Save message
    fun saveMessage(telegram: Telegram) {
        val doc = telegram.toDocument().append("timestamp", System.currentTimeMillis())
        collection.insertOne(doc)
    }

    // Get all messages
    suspend fun getAllMessages(): List<Telegram> = withContext(Dispatchers.IO) {
        collection.find().map { Telegram.fromDocument(it) }.toList()
    }
}