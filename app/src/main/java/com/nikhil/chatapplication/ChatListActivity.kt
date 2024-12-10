package com.nikhil.chatapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.google.firebase.firestore.FirebaseFirestore

data class ChatItem(
    val name: String,
    var lastMessage: String = "",
    var timestamp: String = ""
)

class ChatListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatListScreen()
        }
    }
}

@Composable
fun ChatListScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // Define chat list with multiple chat items
    val chatList = remember { mutableStateListOf<ChatItem>() }

    // Fetch chat list and last messages from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("chats") // Replace with your actual collection path
            .get()
            .addOnSuccessListener { querySnapshot ->
                val groupedChats = querySnapshot.documents.groupBy { document ->
                    // Group by a unique identifier, e.g., chatId or userId
                    document.getString("chatId") ?: "unknown"
                }

                chatList.clear()
                groupedChats.forEach { (_, messages) ->
                    // Get the latest message from each chat group
                    val latestMessage = messages.maxByOrNull { it.getLong("time") ?: 0L }
                    if (latestMessage != null) {
                        val name = latestMessage.getString("name") ?: "Dhea Ayu"
                        val lastMessage = latestMessage.getString("message") ?: "No messages yet"
                        val timestamp = formatTimestamp(latestMessage.getLong("time") ?: System.currentTimeMillis())

                        chatList.add(ChatItem(name, lastMessage, timestamp))
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace() // Log the error or show a message
            }
    }

    // Layout for Chat List Screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title for Chat List
        Text(
            text = "Chat",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )

        // Render each chat item
        if (chatList.isNotEmpty()) {
            for (chat in chatList) {
                ChatListItem(chat = chat, onClick = {
                    // Navigate to MainActivity (ChatScreen)
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                })
            }
        } else {
            Text(
                text = "No chats available",
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ChatListItem(chat: ChatItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture Placeholder
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Picture for ${chat.name}",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Chat Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.Black
            )
            Text(
                text = chat.lastMessage.ifEmpty { "No messages yet" },
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                color = Color.Gray,
                maxLines = 1
            )
        }

        // Timestamp
        Text(
            text = chat.timestamp.ifEmpty { "--:--" },
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = Color.Gray
        )
    }
}
