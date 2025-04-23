package com.khan.chitchat.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel(){

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val _state  = MutableStateFlow<SignUpState>(SignUpState.Nothing)
    val state = _state.asStateFlow()

    fun signUp(username:String, email: String, password: String){
        _state.value = SignUpState.Loading
        Log.d("SignUpViewModel", "Starting sign up process for email: $email")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener{task ->
                if(task.isSuccessful){
                    Log.d("SignUpViewModel", "User created successfully in Authentication")
                    task.result.user?.let { user ->
                        // Update display name in Authentication
                        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(username).build())
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.d("SignUpViewModel", "Profile updated successfully")
                                    // Store user data in Realtime Database
                                    val userData = hashMapOf(
                                        "email" to email,
                                        "displayName" to username,
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    
                                    Log.d("SignUpViewModel", "Storing user data in Realtime Database: $userData")
                                    
                                    database.getReference("users")
                                        .child(user.uid)
                                        .setValue(userData)
                                        .addOnSuccessListener {
                                            Log.d("SignUpViewModel", "User data stored successfully in Realtime Database")
                                            _state.value = SignUpState.Success
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("SignUpViewModel", "Error storing user data in Realtime Database", e)
                                            _state.value = SignUpState.Error
                                        }
                                } else {
                                    Log.e("SignUpViewModel", "Error updating profile", profileTask.exception)
                                    _state.value = SignUpState.Error
                                }
                            }
                    } ?: run {
                        Log.e("SignUpViewModel", "User object is null after successful creation")
                        _state.value = SignUpState.Error
                    }
                } else {
                    Log.e("SignUpViewModel", "Error creating user in Authentication", task.exception)
                    _state.value = SignUpState.Error
                }
            }
    }
}

sealed class SignUpState{
    object Nothing : SignUpState()
    object Loading : SignUpState()
    object Success : SignUpState()
    object Error : SignUpState()
}