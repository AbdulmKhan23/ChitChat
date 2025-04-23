package com.khan.chitchat.screens.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khan.chitchat.Screen
import com.khan.chitchat.ViewModel.ChatUser
import com.khan.chitchat.ViewModel.HomeState
import com.khan.chitchat.ViewModel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val chatUsers by viewModel.chatUsers.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            is HomeState.Error -> {
                Toast.makeText(context, (state as HomeState.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ChitChat") } )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp,2.dp)

        ) {
            TextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.isNotBlank()) {
                        viewModel.searchUsers(it)
                        isSearching = true
                    } else {
                        isSearching = false
                    }
                },
                placeholder = { Text("Search by email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.large,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isSearching) {
                when (state) {
                    is HomeState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    is HomeState.Error -> {
                        Text(
                            text = "Error searching users",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {
                        LazyColumn {
                            items(searchResults) { user ->
                                SearchResultItem(
                                    user = user,
                                    onClick = {
                                        viewModel.startChat(user.userId)
                                        isSearching = false
                                        searchQuery = ""
                                        navController.navigate(Screen.Chat.createRoute(user.userId))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Chat List below search bar
            when (state) {
                is HomeState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is HomeState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading chats",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    if (chatUsers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No chats yet. Search for users to start chatting!",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn {
                            items(chatUsers) { user ->
                                ChatListItem(
                                    user = user,
                                    onClick = {
                                        navController.navigate(Screen.Chat.createRoute(user.userId))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    user: ChatUser,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(user.displayName) },
        supportingContent = { Text(user.email) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun ChatListItem(
    user: ChatUser,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedTime = remember(user.lastMessageTime) {
        dateFormat.format(Date(user.lastMessageTime))
    }

    ListItem(
        headlineContent = { Text(user.displayName) },
        supportingContent = {
            Text(
                text = user.lastMessage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            if (user.unreadCount > 0) {
                Badge {
                    Text(user.unreadCount.toString())
                }
            }
        },
        trailingContent = {
            Text(
                text = formattedTime,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

