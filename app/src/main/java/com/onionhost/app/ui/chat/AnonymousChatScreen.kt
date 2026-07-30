package com.onionhost.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onionhost.app.ui.home.HomeViewModel

@Composable
fun AnonymousChatScreen(viewModel: HomeViewModel) {
    val activeWebsite by viewModel.activeWebsite.collectAsState()
    val activeRoom by viewModel.activeChatRoom.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }
    var roomName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Anonymous Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (activeWebsite?.onionAddress.isNullOrBlank()) "Start hosting to enable chat" else "Messages from your Onion invite link",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Create or join a room", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. friends or private-alice") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.selectChatRoom(roomName) }, enabled = activeWebsite != null && roomName.isNotBlank()) { Text("Open") }
                }
                if (activeRoom.isNotBlank() && activeWebsite?.onionAddress?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val invite = "http://${activeWebsite?.onionAddress}/chat/$activeRoom"
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("Anonymous Chat Invite", invite))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copy invite for this room") }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeWebsite == null) {
                        ChatEmptyState("No active host", "Go to Home, choose content, and start hosting first.")
                    } else if (messages.isEmpty()) {
                        ChatEmptyState("No messages yet", "Share the Anonymous Chat Invite from Home. Incoming messages will appear here.")
                    } else {
                        messages.takeLast(100).forEach { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (message.text.isNotBlank()) Text(message.text, style = MaterialTheme.typography.bodyMedium)
                                    message.attachment?.let { Text("Attachment: ${it.name}", style = MaterialTheme.typography.labelSmall) }
                                }
                                IconButton(onClick = { viewModel.deleteChatMessage(message.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete message")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(1_000) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a reply…") },
                        singleLine = true,
                        enabled = activeWebsite != null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        enabled = activeWebsite != null && draft.isNotBlank(),
                        onClick = {
                            viewModel.sendChatMessage(draft)
                            draft = ""
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
