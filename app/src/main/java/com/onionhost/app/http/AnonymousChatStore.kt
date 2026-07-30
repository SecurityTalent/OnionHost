package com.onionhost.app.http

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Chat state is private to the host app and survives hosting/app restarts. */
object AnonymousChatStore {
    data class Attachment(val dataUrl: String, val name: String, val mimeType: String)
    data class Message(val id: Long, val text: String, val sentAt: Long, val ownerId: String?, val attachment: Attachment? = null, val sender: String = "Anonymous")

    private val rooms = ConcurrentHashMap<String, CopyOnWriteArrayList<Message>>()
    // Clients use the last seen ID as a cursor. IDs must therefore be ordered;
    // random IDs caused valid newer messages to be skipped by polling clients.
    private val nextMessageId = AtomicLong(0)
    private val changes = MutableStateFlow(0L)
    private var storageFile: File? = null

    @Synchronized
    fun initialize(context: Context) {
        if (storageFile != null) return
        storageFile = File(context.filesDir, "anonymous_chat.json")
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            val savedRooms = root.optJSONObject("rooms") ?: return
            var largestId = 0L
            savedRooms.keys().forEach { room ->
                val savedMessages = savedRooms.optJSONArray(room) ?: return@forEach
                val restored = CopyOnWriteArrayList<Message>()
                for (index in 0 until savedMessages.length()) {
                    val item = savedMessages.getJSONObject(index)
                    val attachmentJson = item.optJSONObject("attachment")
                    val attachment = attachmentJson?.let { Attachment(it.optString("data"), it.optString("name"), it.optString("type")) }
                    val message = Message(item.getLong("id"), item.optString("text"), item.getLong("sentAt"), item.optString("ownerId").ifBlank { null }, attachment)
                    restored += message
                    largestId = maxOf(largestId, message.id)
                }
                rooms[room] = restored
            }
            nextMessageId.set(largestId)
        } catch (_: Exception) {
            // Keep hosting usable if a previous interrupted write is unreadable.
        }
    }

    fun add(room: String, text: String, ownerId: String? = null, attachment: Attachment? = null, sender: String = if (ownerId == null) "Host" else anonymousName(ownerId)): Message {
        val message = Message(
            id = nextMessageId.incrementAndGet(),
            text = text.take(MAX_MESSAGE_LENGTH),
            sentAt = System.currentTimeMillis(), ownerId = ownerId, attachment = attachment, sender = sender
        )
        val messages = rooms.getOrPut(room) { CopyOnWriteArrayList() }
        messages += message
        // Keep memory bounded even when an invite link is shared widely.
        while (messages.size > MAX_MESSAGES_PER_ROOM) messages.removeAt(0)
        persist()
        changes.value += 1
        return message
    }

    fun since(room: String, after: Long): List<Message> =
        rooms[room].orEmpty().filter { it.id > after }.sortedBy { it.sentAt }

    fun messages(room: String): List<Message> = rooms[room].orEmpty().sortedBy { it.sentAt }

    fun roomNames(): List<String> = rooms.keys().toList().sorted()

    fun messagesFlow(room: String): Flow<List<Message>> = changes.map { messages(room) }
    fun roomNamesFlow(): Flow<List<String>> = changes.map { roomNames() }

    fun deleteByOwner(room: String, id: Long, ownerId: String): Boolean =
        delete(room) { it.id == id && it.ownerId == ownerId }

    fun deleteByHost(room: String, id: Long): Boolean = delete(room) { it.id == id }

    fun deleteRoom(room: String): Boolean {
        val removed = rooms.remove(room) != null
        if (removed) { persist(); changes.value += 1 }
        return removed
    }

    private fun delete(room: String, predicate: (Message) -> Boolean): Boolean {
        val result = rooms[room]?.removeIf(predicate) == true
        if (result) { persist(); changes.value += 1 }
        return result
    }

    fun anonymousName(clientId: String): String = "Anon-" + clientId.hashCode().toUInt().toString(16).take(6).uppercase()

    @Synchronized
    private fun persist() {
        val file = storageFile ?: return
        try {
            val savedRooms = JSONObject()
            rooms.forEach { (room, messages) ->
                val array = JSONArray()
                messages.forEach { message ->
                    val item = JSONObject().put("id", message.id).put("text", message.text).put("sentAt", message.sentAt).put("ownerId", message.ownerId ?: "")
                    message.attachment?.let { item.put("attachment", JSONObject().put("data", it.dataUrl).put("name", it.name).put("type", it.mimeType)) }
                    array.put(item)
                }
                savedRooms.put(room, array)
            }
            val temporary = File(file.parentFile, "${file.name}.tmp")
            temporary.writeText(JSONObject().put("rooms", savedRooms).toString())
            if (!temporary.renameTo(file)) { file.writeText(temporary.readText()); temporary.delete() }
        } catch (_: Exception) { }
    }

    const val MAX_MESSAGE_LENGTH = 1_000
    const val MAX_ATTACHMENT_CHARS = 7_000_000
    private const val MAX_MESSAGES_PER_ROOM = 200
}
