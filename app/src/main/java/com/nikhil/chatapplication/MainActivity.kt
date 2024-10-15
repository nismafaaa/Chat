package com.nikhil.chatapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatApp()
        }
    }
}

@Composable
fun ChatApp() {
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()

    // Fetch messages from Firestore
    LaunchedEffect(Unit) {
        db.collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    val fetchedMessages = snapshot.documents.map {
                        Message(
                            message = it.getString("message") ?: "",
                            senderId = it.getString("senderId") ?: "",
                            timestamp = it.getTimestamp("timestamp")?.toDate().toString()
                        )
                    }
                    messages = fetchedMessages
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages.size) { index ->
                MessageItem(
                    message = messages[index],
                    isCurrentUser = messages[index].senderId == currentUser?.uid
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.LightGray, CircleShape)
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (messageText.text.isNotBlank()) {
                        scope.launch {
                            val messageData = mapOf(
                                "message" to messageText.text,
                                "senderId" to currentUser?.uid,
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )
                            db.collection("messages").add(messageData)
                            messageText = TextFieldValue("")
                        }
                    }
                }
            ) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, isCurrentUser: Boolean) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 1.dp,
            color = backgroundColor
        ) {
            Text(
                text = "${message.message}\n${message.timestamp}",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

data class Message(
    val message: String,
    val senderId: String,
    val timestamp: String
)
