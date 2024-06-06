package com.example.pitchmasterbeta.ui.login

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

// Helper function to navigate and pop up to a specific route


@Composable
fun AuthRouter(navigateToWorkspace: (String) -> Unit = {}) {
    val navController = rememberNavController()

    fun navigateWithPopUpTo(route: Any, popUpToRoute: Any) {
        navController.navigate(route) {
            popUpTo(popUpToRoute) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = LoginScreenIntro) {
        composable<LoginScreenIntro> {
            LoginScreen(navigateToWorkspace, navigateToRegistrationScreen = {
                navigateWithPopUpTo(RegisterScreenIntro, LoginScreenIntro)
            })
        }
        composable<RegisterScreenIntro> {
            RegisterScreen(navigateToLoginScreen = {
                navigateWithPopUpTo(LoginScreenIntro, RegisterScreenIntro)
            }, navigateToVerificationScreen = { email, userName ->
                navController.navigate(EmailVerificationIntro(email, userName))
            })
        }
        composable<EmailVerificationIntro> {
            val emailToVerify = it.toRoute<EmailVerificationIntro>().emailToVerify
            val username = it.toRoute<EmailVerificationIntro>().username
            EmailVerificationScreen(emailToVerify, username, navigateToRegistrationSuccessScreen = {
                navController.navigate(RegisterSuccessScreenIntro) {
                    popUpTo(EmailVerificationIntro(emailToVerify, username)) { inclusive = true }
                    popUpTo(RegisterScreenIntro) { inclusive = true }
                }
            })
        }
        composable<RegisterSuccessScreenIntro> {
            RegistrationSuccessScreen(navigateToLoginScreen = {
                navigateWithPopUpTo(LoginScreenIntro, RegisterSuccessScreenIntro)
            })
        }
    }
}

@Serializable
object LoginScreenIntro

@Serializable
object RegisterScreenIntro

@Serializable
data class EmailVerificationIntro(
    val emailToVerify: String, val username: String
)

@Serializable
object RegisterSuccessScreenIntro
