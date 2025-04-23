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

data class ChatUser(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0
)

sealed class HomeState {
    object Loading : HomeState()
    object Success : HomeState()
    data class Error(val message: String) : HomeState()
    object Nothing : HomeState()
}

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var chatListener: ValueEventListener? = null

    private val _searchResults = MutableStateFlow<List<ChatUser>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _chatUsers = MutableStateFlow<List<ChatUser>>(emptyList())
    val chatUsers = _chatUsers.asStateFlow()

    private val _state = MutableStateFlow<HomeState>(HomeState.Nothing)
    val state = _state.asStateFlow()

    init {
        observeChats()
    }

    private fun observeChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        chatListener = database.getReference("chats")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch {
                        try {
                            val users = mutableListOf<ChatUser>()
                            
                            for (chatSnapshot in snapshot.children) {
                                val participants = chatSnapshot.child("participants").children.mapNotNull { it.key }
                                val otherUserId = participants.find { it != currentUserId } ?: continue
                                
                                // Get the other user's data
                                val userSnapshot = database.getReference("users")
                                    .child(otherUserId)
                                    .get()
                                    .await()
                                
                                val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                                val lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                                
                                val userData = userSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                                if (userData != null) {
                                    users.add(
                                        ChatUser(
                                            userId = otherUserId,
                                            email = userData["email"] as? String ?: "",
                                            displayName = userData["displayName"] as? String ?: "",
                                            lastMessage = lastMessage,
                                            lastMessageTime = lastMessageTime
                                        )
                                    )
                                }
                            }
                            
                            _chatUsers.value = users.sortedByDescending { it.lastMessageTime }
                            _state.value = HomeState.Success
                        } catch (e: Exception) {
                            _state.value = HomeState.Success // Don't show error state
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _state.value = HomeState.Success // Don't show error state
                }
            })
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _state.value = HomeState.Loading
                val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                val usersSnapshot = database.getReference("users")
                    .get()
                    .await()

                val users = mutableListOf<ChatUser>()
                val typeIndicator = object : GenericTypeIndicator<Map<String, Any>>() {}
                
                for (userSnapshot in usersSnapshot.children) {
                    val userData = userSnapshot.getValue(typeIndicator)
                    if (userData != null && userSnapshot.key != currentUserId) {
                        val email = userData["email"] as? String ?: ""
                        val displayName = userData["displayName"] as? String ?: ""
                        
                        if (email.contains(query, ignoreCase = true) || 
                            displayName.contains(query, ignoreCase = true)) {
                            users.add(
                                ChatUser(
                                    userId = userSnapshot.key ?: "",
                                    email = email,
                                    displayName = displayName
                                )
                            )
                        }
                    }
                }
                
                _searchResults.value = users
                _state.value = HomeState.Success
                
            } catch (e: Exception) {
                _state.value = HomeState.Error("Failed to search users")
                _searchResults.value = emptyList()
            }
        }
    }

    fun startChat(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = if (currentUserId < userId) {
                    "${currentUserId}_${userId}"
                } else {
                    "${userId}_${currentUserId}"
                }

                val chatData = mapOf(
                    "participants" to mapOf(
                        currentUserId to true,
                        userId to true
                    ),
                    "lastMessage" to "",
                    "lastMessageTime" to System.currentTimeMillis(),
                    "unreadCount" to 0
                )

                database.getReference("chats")
                    .child(chatId)
                    .setValue(chatData)
                    .await()

            } catch (e: Exception) {
                _state.value = HomeState.Error("Failed to start chat")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.let { listener ->
            database.getReference("chats").removeEventListener(listener)
        }
    }
}


