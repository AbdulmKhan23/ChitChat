package com.khan.chitchat.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ChatState {
    object Loading : ChatState()
    object Success : ChatState()
    object Error : ChatState()
    object Nothing : ChatState()
}

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var messageListener: ValueEventListener? = null
    private var currentChatId: String? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _state = MutableStateFlow<ChatState>(ChatState.Nothing)
    val state = _state.asStateFlow()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _otherUserName = MutableStateFlow("")
    val otherUserName = _otherUserName.asStateFlow()

    fun loadChat(otherUserId: String) {
        viewModelScope.launch {
            try {
                _state.value = ChatState.Loading
                val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                // Load other user's name
                val userSnapshot = database.getReference("users")
                    .child(otherUserId)
                    .get()
                    .await()
                
                val userData = userSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                _otherUserName.value = userData?.get("displayName") as? String ?: "Chat"
                
                // Create or get chat ID
                currentChatId = if (currentUserId < otherUserId) {
                    "${currentUserId}_${otherUserId}"
                } else {
                    "${otherUserId}_${currentUserId}"
                }

                // Initialize chat structure if it doesn't exist
                val chatRef = database.getReference("chats").child(currentChatId!!)
                val chatSnapshot = chatRef.get().await()
                
                if (!chatSnapshot.exists()) {
                    // Create initial chat structure
                    val initialChatData = mapOf(
                        "participants" to mapOf(
                            currentUserId to true,
                            otherUserId to true
                        ),
                        "messages" to mapOf<String, Any>(),
                        "lastMessage" to "",
                        "lastMessageTime" to System.currentTimeMillis()
                    ) as Map<String, Any>
                    
                    chatRef.setValue(initialChatData).await()
                }

                // Observe messages
                messageListener?.let { listener ->
                    chatRef.child("messages").removeEventListener(listener)
                }

                messageListener = chatRef.child("messages")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val messageList = mutableListOf<ChatMessage>()
                            for (messageSnapshot in snapshot.children) {
                                val messageData = messageSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                                if (messageData != null) {
                                    messageList.add(
                                        ChatMessage(
                                            id = messageSnapshot.key ?: "",
                                            senderId = messageData["senderId"] as? String ?: "",
                                            senderName = messageData["senderName"] as? String ?: "",
                                            message = messageData["message"] as? String ?: "",
                                            timestamp = (messageData["timestamp"] as? Long) ?: 0L
                                        )
                                    )
                                }
                            }
                            _messages.value = messageList.sortedBy { it.timestamp }
                            _state.value = ChatState.Success
                        }

                        override fun onCancelled(error: DatabaseError) {
                            _state.value = ChatState.Success // Don't show error state
                        }
                    })

            } catch (e: Exception) {
                _state.value = ChatState.Success // Don't show error state
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val chatId = currentChatId ?: throw Exception("No active chat")
                
                val chatRef = database.getReference("chats").child(chatId)
                
                // Generate message ID
                val messageId = chatRef.child("messages").push().key ?: return@launch

                val messageData = mapOf(
                    "senderId" to currentUser.uid,
                    "senderName" to (currentUser.displayName ?: "Anonymous"),
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                ) as Map<String, Any>

                // Create a map of updates
                val updates = hashMapOf<String, Any>(
                    "messages/$messageId" to messageData,
                    "lastMessage" to message,
                    "lastMessageTime" to System.currentTimeMillis()
                )

                // Update everything at once
                chatRef.updateChildren(updates).await()

            } catch (e: Exception) {
                _state.value = ChatState.Success // Don't show error state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.let { listener ->
            currentChatId?.let { chatId ->
                database.getReference("chats")
                    .child(chatId)
                    .child("messages")
                    .removeEventListener(listener)
            }
        }
    }
}


