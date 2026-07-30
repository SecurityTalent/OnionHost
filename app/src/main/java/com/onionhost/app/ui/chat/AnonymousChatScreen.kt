package com.onionhost.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Lock
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
    val rooms by viewModel.chatRooms.collectAsState()
    val actionInProgress by viewModel.chatActionInProgress.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }
    var roomName by rememberSaveable { mutableStateOf("") }
    var chatTab by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.sendChatAttachment(context, draft, it); draft = "" }
    }

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
                TabRow(selectedTabIndex = chatTab) {
                    listOf("Personal chat", "Rooms").forEachIndexed { index, title ->
                        Tab(selected = chatTab == index, onClick = { chatTab = index }, text = { Text(title) })
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (chatTab == 0) {
                    Text("One-to-one chat", fontWeight = FontWeight.SemiBold)
                    Text("Create a private link and share it with one person.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.createPrivateChatRoom() }, enabled = activeWebsite != null, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Lock, null); Spacer(Modifier.width(6.dp)); Text("New personal chat link")
                    }
                    val personalRooms = rooms.filter { it.startsWith("personal-") || it.startsWith("private-") }
                    if (personalRooms.isNotEmpty()) {
                        Text("Saved personal chats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        personalRooms.takeLast(4).forEach { savedRoom ->
                            TextButton(onClick = { viewModel.selectChatRoom(savedRoom) }) { Text(savedRoom.removePrefix("personal-").removePrefix("private-").take(18)) }
                        }
                    }
                } else {
                    Text("Group rooms", fontWeight = FontWeight.SemiBold)
                    Text("Everyone with the same room link can chat together.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = roomName, onValueChange = { roomName = it }, modifier = Modifier.weight(1f), placeholder = { Text("e.g. friends") }, singleLine = true)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.createRoom(roomName); roomName = "" }, enabled = activeWebsite != null && roomName.isNotBlank()) { Text("Create") }
                    }
                    val groupRooms = rooms.filterNot { it.startsWith("personal-") || it.startsWith("private-") }
                    if (groupRooms.isNotEmpty()) Text("Saved rooms: ${groupRooms.joinToString().take(100)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (activeRoom.isNotBlank() && activeWebsite?.onionAddress?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        val invite = "http://${activeWebsite?.onionAddress}/chat/$activeRoom"
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Anonymous Chat Invite", invite))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Copy active chat link") }
                    TextButton(onClick = { viewModel.deleteActiveChatRoom() }) { Text("Delete active chat") }
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
                                IconButton(onClick = { viewModel.deleteChatMessage(message.id) }, enabled = !actionInProgress) {
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
                        enabled = activeWebsite != null && !actionInProgress
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { attachmentPicker.launch("*/*") }, enabled = activeWebsite != null && !actionInProgress) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach photo, video, or audio")
                    }
                    IconButton(
                        enabled = activeWebsite != null && draft.isNotBlank() && !actionInProgress,
                        onClick = {
                            viewModel.sendChatMessage(draft)
                            draft = ""
                        }
                    ) {
                        if (actionInProgress) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Send, contentDescription = "Send message")
                    }
                }
                if (actionInProgress) Text("Sending or updating chat…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
