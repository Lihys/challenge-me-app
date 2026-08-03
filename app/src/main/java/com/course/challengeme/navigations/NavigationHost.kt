package com.course.challengeme.navigations

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.course.challengeme.LaunchScreen
import com.course.challengeme.ChallengePage
import com.course.challengeme.HomePage
import com.course.challengeme.LeaderboardPage
import com.course.challengeme.JoinViaCode
import com.course.challengeme.LogIn
import com.course.challengeme.MyAccount
import com.course.challengeme.NewChallenge
import com.course.challengeme.Register
import com.course.challengeme.InviteCodeScreen

@Composable
fun NavigationHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Navigation.Launch.route) {
        composable(Navigation.Launch.route) { LaunchScreen(navController) }
        composable(Navigation.Login.route) { LogIn(navController) }
        composable(Navigation.Register.route) { Register(navController) }
        composable(Navigation.Home.route) { HomePage(navController) }
        composable(Navigation.MyAccount.route) { MyAccount(navController) }
        composable(Navigation.CreateChallenge.route) { NewChallenge(navController) }
        composable(Navigation.InviteCode.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("challengeId")
            InviteCodeScreen(navController, challengeId = id)
        }
        composable(Navigation.InviteCode.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("challengeId")
            JoinViaCode(navController, challengeId = id)
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