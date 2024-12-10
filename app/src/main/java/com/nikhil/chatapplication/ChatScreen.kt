package com.nikhil.chatapplication

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    LaunchedEffect(Unit) {
        firestore.collection("chats")
            .orderBy("time")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    chatHistory = snapshots.map { doc ->
                        ChatMessage(
                            message = doc.getString("message") ?: "",
                            time = formatTimestamp(doc.getLong("time") ?: System.currentTimeMillis()),
                            isCurrentUser = doc.getBoolean("isCurrentUser") ?: false,
                            profileImage = null
                        )
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = Color(0xFF9806E5),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dhea Ayu",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            val intent = Intent(context, ChatListActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to Chat List",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.Transparent
                    )
                )
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        items(chatHistory) { chatMessage ->
                            ChatBubble(
                                message = chatMessage.message,
                                time = chatMessage.time,
                                isCurrentUser = chatMessage.isCurrentUser,
                                profileImage = chatMessage.profileImage
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color(0xFFF3F3F3),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("Type your message") }
                    )
                    IconButton(
                        onClick = {
                            if (message.isNotBlank()) {
                                val chatMessage = mapOf(
                                    "message" to message,
                                    "time" to System.currentTimeMillis(),
                                    "isCurrentUser" to true
                                )
                                firestore.collection("chats")
                                    .add(chatMessage)
                                    .addOnSuccessListener {
                                        message = "" // Reset the input field
                                    }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9806E5), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ChatBubble(
    message: String,
    time: String,
    isCurrentUser: Boolean,
    profileImage: Int? = null // Make this optional
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            if (profileImage != null) {
                // Use image resource if provided
                Image(
                    painter = painterResource(id = profileImage),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Use Jetpack Compose built-in icon
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Profile Icon",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .background(
                    color = if (isCurrentUser) Color(0xFF8A2BE2) else Color(0xFFF3F3F3),
                    shape = ChatBubbleShape(isCurrentUser)
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = time,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Default Profile Icon",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                tint = Color.Gray
            )
        }
    }
}

private fun ChatBubbleShape(isCurrentUser: Boolean): androidx.compose.ui.graphics.Shape {
    return if (isCurrentUser) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 4.dp,
            bottomEnd = 12.dp
        )
    }
}

data class ChatMessage(
    val message: String,
    val time: String,
    val isCurrentUser: Boolean,
    val profileImage: Int?
)