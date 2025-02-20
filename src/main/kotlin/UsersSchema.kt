package com.example

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt

@Serializable
data class UserSession(val username: String, val login: Boolean)
@Serializable
data class Users(
    val username: String,
    val hashedPassword: String? =null,
    val salt: String? = null
) {
    fun toDocument(): Document {
        if (salt == null) {
            throw IllegalArgumentException("Salt cannot be null")
        }

        // Hash the password with the stored salt
        val hashedPassword = BCrypt.hashpw(hashedPassword ?: "", salt)

        return Document("username", username)
            .append("password", hashedPassword)
            .append("salt", salt)
    }

    companion object {
        fun fromDocument(doc: Document): Users {
            val username = doc.getString("username")
            val password = doc.getString("password")
            val salt = doc.getString("salt")
            return Users(username, password, salt)
        }
    }
}


class UserService(private val database: MongoDatabase) {
    private val collection: MongoCollection<Document> = database.getCollection("users")

    suspend fun findByUsername(username: String): Users? = withContext(Dispatchers.IO){
        collection.find(Filters.eq("username",username)).first()?.let{Users.fromDocument(it)}

    }
    // Create new user
    suspend fun create(user: Users): String = withContext(Dispatchers.IO) {
        val salt = BCrypt.gensalt() // Generate salt only once
        val userWithSalt = Users(user.username, user.hashedPassword, salt)
        val doc = userWithSalt.toDocument()  // Converts Users object to Document
        collection.insertOne(doc)
        doc["_id"].toString()
    }
    // Read a user
    suspend fun read(id: String): Users? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("_id", ObjectId(id)))
            .first()
            ?.let { Users.fromDocument(it) }
    }

    // Update a user
    suspend fun update(id: String, user: Users): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndReplace(Filters.eq("_id", ObjectId(id)), user.toDocument())
    }

    // Delete a user
    suspend fun delete(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))
    }
}
