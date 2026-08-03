package com.course.challengeme.navigations

sealed class Navigation(val route: String) {
    object Launch : Navigation("launch")
    object Login : Navigation("login")
    object Register : Navigation("register")
    object Home : Navigation("home")
    object MyAccount : Navigation("my_account")
    object CreateChallenge : Navigation("create_challenge")
    object JoinChallenge : Navigation("join_challenge")
    object ChallengePage : Navigation("challenge/{challengeId}") {
        fun createRoute(challengeId: String) = "challenge/$challengeId"
    }
    object Leaderboard : Navigation("leaderboard/{challengeId}") {
        fun createRoute(challengeId: String) = "leaderboard/$challengeId"
    }
    object InviteCode : Navigation("invite_code/{challengeId}") {
        fun createRoute(challengeId: String) = "invite_code/$challengeId"
    }
}