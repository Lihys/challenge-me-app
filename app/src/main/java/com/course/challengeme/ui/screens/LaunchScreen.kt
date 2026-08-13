package com.course.challengeme.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.course.challengeme.R
import com.course.challengeme.auth.FirebaseAuth
import com.course.challengeme.navigations.Navigation
import com.course.challengeme.ui.theme.AppBackground
import com.course.challengeme.ui.theme.AppText
import com.course.challengeme.ui.theme.BlueLauncherBg
import com.course.challengeme.ui.theme.ButtonDark

@Composable
fun LaunchScreen(navController: NavController) {
    val authRepository = remember { FirebaseAuth() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueLauncherBg)
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
                verticalArrangement = Arrangement.spacedBy(2.dp)
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

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    val destination =
                        if (authRepository.isLoggedIn) Navigation.Home.route else Navigation.Login.route
                    navController.navigate(destination) {
                        popUpTo(Navigation.Launch.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = AppBackground)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Continue",
                    tint = AppText
                )
            }


        }
        //Spacer(modifier = Modifier.height(12.dp))



    }
}