package com.course.challengeme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.course.challengeme.auth.FirebaseAuth
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.components.TextField
import com.course.challengeme.ui.components.PrimaryButton
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.ButtonDark
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val authRepository = remember { FirebaseAuth() }
    val coroutineScope = rememberCoroutineScope()

    fun goHomeAndClearBackStack() {
        navController.navigate(Navigation.Home.route) {
            popUpTo(Navigation.Register.route) { inclusive = true }
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
        Text("Create account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppText)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(value = name, onValueChange = { name = it }, label = "Name")
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, keyboardType = KeyboardType.Password)
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm password", isPassword = true, keyboardType = KeyboardType.Password)

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Create Account",
            isLoading = isLoading,
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            onClick = {
                if (password != confirmPassword) {
                    errorMessage = "Passwords don't match"
                    return@PrimaryButton
                }
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    authRepository.register(email, password)
                        .onSuccess { goHomeAndClearBackStack() }
                        .onFailure { errorMessage = it.localizedMessage ?: "Registration failed" }
                    isLoading = false
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account? ", color = AppText.copy(alpha = 0.7f), fontSize = 14.sp)
            TextButton(
                onClick = {
                    navController.navigate(Navigation.Login.route) {
                        popUpTo(Navigation.Register.route) { inclusive = true }
                    }
                }
            ) {
                Text("Log In", color = ButtonDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}