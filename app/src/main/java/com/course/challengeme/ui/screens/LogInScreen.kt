package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.auth.FirebaseAuth
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.TextField
import com.course.challengeme.ui.components.GoogleSignInButton
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import kotlinx.coroutines.launch

@Composable
fun LogInScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val authRepository = remember { FirebaseAuth() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun goHomeAndClearBackStack() {
        navController.navigate(Navigation.Home.route) {
            popUpTo(Navigation.Login.route) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome back!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppText)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, keyboardType = KeyboardType.Password)

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Log In",
            isLoading = isLoading,
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    authRepository.signIn(email, password)
                        .onSuccess { goHomeAndClearBackStack() }
                        .onFailure { errorMessage = it.localizedMessage ?: "Login failed" }
                    isLoading = false
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text("or", color = AppText.copy(alpha = 0.5f), fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))

        GoogleSignInButton(
            onClick = {
                coroutineScope.launch {
                    authRepository.signInWithGoogle(context)
                        .onSuccess { goHomeAndClearBackStack() }
                        .onFailure { errorMessage = it.localizedMessage ?: "Google sign-in failed" }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ", color = AppText.copy(alpha = 0.7f), fontSize = 14.sp)
            TextButton(onClick = { navController.navigate(Navigation.Register.route) }) {
                Text("Register", color = ButtonDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}