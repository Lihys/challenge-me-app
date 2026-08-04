package com.course.challengeme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import com.course.challengeme.navigations.Navigation
import androidx.compose.runtime.getValue

@Composable
fun LaunchScreen(navController: NavController) {
    val backgroundColor = Color(0xFF022A8B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Challenge me text + animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp) // gap between them
            ) {
                Image(
                    painter = painterResource(id = R.drawable.launcher_logo),
                    contentDescription = "Challenge Me logo",
                    modifier = Modifier.size(350.dp)
                )

                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.handshake_icon)
                )
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = Int.MAX_VALUE
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(250.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // so the button wil be at the bottom

            IconButton(
                onClick = {
                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    val destination =
                        if (isLoggedIn) Navigation.Home.route else Navigation.Login.route
                    navController.navigate(destination) {
                        popUpTo(Navigation.Launch.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = Color(0xFFF9F0E3))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Continue",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


