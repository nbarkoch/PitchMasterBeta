package com.example.pitchmasterbeta.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pitchmasterbeta.AuthIntro
import com.example.pitchmasterbeta.WorkspaceIntro
import com.example.pitchmasterbeta.ui.login.AuthRouter
import com.example.pitchmasterbeta.ui.login.AuthViewModel
import com.example.pitchmasterbeta.ui.workspace.WorkspaceSurface
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel


@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    val startDestination: Any = if (authViewModel.checkLoginStatus()) {
        WorkspaceIntro
    } else {
        AuthIntro
    }

    NavHost(navController, startDestination = startDestination) {
        composable<AuthIntro> {
            AuthRouter(
                navigateToWorkspace = { jwtToken ->
                    workspaceViewModel.setJwtToken(jwtToken)
                    navController.navigate(WorkspaceIntro) {
                        popUpTo(AuthIntro) { inclusive = true }
                    }
                }
            )
        }
        composable<WorkspaceIntro> {
            WorkspaceSurface()
        }
    }

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(AuthIntro) {
                popUpToRoute?.let { popUpTo(it) { inclusive = true } }
            }
        }
    }
}