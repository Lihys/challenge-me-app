package com.course.challengeme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.course.challengeme.navigations.Navigation

@Composable
fun LaunchScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Launch Screen")
    }
    /*Button(onClick = { navController.navigate(Navigation.Login.route) }) {
        Text("Go to Login")
    }*/

}
