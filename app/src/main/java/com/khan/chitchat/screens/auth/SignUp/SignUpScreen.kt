package com.khan.chitchat.screens.auth.SignUp

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khan.chitchat.ViewModel.SignUpState
import com.khan.chitchat.ViewModel.SignUpViewModel


@Composable
fun SignUpScreen(navController: NavController){
    val viewModel: SignUpViewModel = hiltViewModel()
    val uiState = viewModel.state.collectAsState()
    var username by remember {
        mutableStateOf("")
    }
    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var ConfirmPassword by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.value){
        if(uiState.value == SignUpState.Success){
            navController.navigate("home")
        }
        if(uiState.value == SignUpState.Error){
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold (modifier = Modifier.fillMaxSize()){
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(it)
            .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {

            OutlinedTextField(value = username,
                onValueChange = {username = it},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "User Name") })

            OutlinedTextField(value = email,
                onValueChange = {email = it},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Email") })

            OutlinedTextField(value = password,
                onValueChange = {password = it},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(value = ConfirmPassword,
                onValueChange = {ConfirmPassword = it},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = password.isNotEmpty() && ConfirmPassword.isNotEmpty() && password != ConfirmPassword
            )

            Spacer(modifier = Modifier.padding(16.dp))

            if(uiState.value == SignUpState.Loading){
                CircularProgressIndicator()
            }
            else {
                Button(onClick = {
                    viewModel.signUp(username, email, password)
                }, modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && ConfirmPassword.isNotEmpty() && password == ConfirmPassword) {
                    Text(text = "Sign Up")
                }

                TextButton(onClick = { navController.popBackStack() }) {
                    Text(text = "Already have an account? Sign In")
                }
            }
        }
    }
}