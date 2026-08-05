package com.course.challengeme.navigations

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.course.challengeme.ui.screens.LaunchScreen
import com.course.challengeme.ui.screens.HomePageScreen
import com.course.challengeme.ui.screens.ChallengePage
import com.course.challengeme.ui.screens.LeaderboardPage
import com.course.challengeme.ui.screens.JoinViaCode
import com.course.challengeme.ui.screens.LogInScreen
import com.course.challengeme.ui.screens.MyAccount
import com.course.challengeme.ui.screens.NewChallengeScreen
import com.course.challengeme.ui.screens.RegisterScreen
import com.course.challengeme.ui.screens.InviteCodeScreen

@Composable
fun NavigationHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Navigation.Launch.route) {
        composable(Navigation.Launch.route) { LaunchScreen(navController) }
        composable(Navigation.Login.route) { LogInScreen(navController) }
        composable(Navigation.Register.route) { RegisterScreen(navController) }
        composable(Navigation.Home.route) { HomePageScreen(navController) }
        composable(Navigation.MyAccount.route) { MyAccount(navController) }
        composable(Navigation.CreateChallenge.route) { NewChallengeScreen(navController) }


        composable(Navigation.InviteCode.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("challengeId")
            InviteCodeScreen(navController, challengeId = id)
        }
        composable(Navigation.JoinChallenge.route) {
            JoinViaCode(navController)
        }
        composable(Navigation.ChallengePage.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("challengeId")
            ChallengePage(navController, challengeId = id)
        }
        composable(Navigation.Leaderboard.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("challengeId")
            LeaderboardPage(navController, challengeId = id)
        }
    }
}