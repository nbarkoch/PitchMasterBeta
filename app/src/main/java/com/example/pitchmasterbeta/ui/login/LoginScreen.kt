package com.example.pitchmasterbeta.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W600
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.getAuthViewModel
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.login.components.AuthOutlinedTextField
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme

@Composable
fun LoginScreen(
    navigateToWorkspace: (String) -> Unit = {},
    navigateToRegistrationScreen: () -> Unit = {}
) {
    val viewModel = getAuthViewModel()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var loading by remember { mutableStateOf(false) }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF403C63),
            Color(0xFF2E265E),
            Color(0xFF121314),
        ),
    )
    var userName by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var verificationError by remember {
        mutableStateOf<String?>(null)
    }

    val isValidInput: (text: String, maxLength: Int, noSpacing: Boolean) -> Boolean =
        { it, maxLength, noSpacing ->
            it.length < maxLength && !(noSpacing && it.isNotEmpty() && it.last() == ' ')
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .padding(top = 75.dp, bottom = 20.dp)
                    .padding(10.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Sign In",
                    color = Color.White,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W700
                )
            }
            Column(
                modifier = Modifier
                    .border(
                        border = BorderStroke(width = 1.dp, color = Color(0x86362E69)),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = Color(0x632E265E))
                    .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthOutlinedTextField(
                    "User Name",
                    userName,
                    R.drawable.user_icon_outline,
                    isError = verificationError != null,
                    keyboardType = KeyboardType.Text
                ) {
                    if (isValidInput(it, 25, false)) {
                        userName = it
                        verificationError = null
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                AuthOutlinedTextField(
                    "Password",
                    password,
                    R.drawable.key_icon_outline,
                    isError = verificationError != null,
                    keyboardType = KeyboardType.Password
                ) {
                    if (isValidInput(it, 40, true)) {
                        password = it
                        verificationError = null
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp), contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        modifier = Modifier.clickable {},
                        text = "Forgot Password?",
                        fontWeight = W600,
                        fontSize = 13.sp,
                        color = Color(0xFFD59EFD),
                    )
                }
                verificationError?.let {
                    Text(
                        text = it, color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .padding(vertical = 25.dp, horizontal = 10.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Row {
                    Text(
                        text = "Don't have an account? ",
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                    Text(
                        modifier = Modifier.clickable {

                            navigateToRegistrationScreen()
                        },
                        text = "Sign Up Now",
                        fontSize = 14.sp,
                        color = Color(0xFFD59EFD),
                        fontWeight = FontWeight.W700,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
            Button(colors = ButtonDefaults.buttonColors(
                Color.White, disabledContainerColor = Color.LightGray
            ),
                enabled = userName.isNotEmpty() && password.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    loading = true
                    viewModel.login(userName, password, onCompletion = { jwtToken ->
                        loading = false
                        navigateToWorkspace(jwtToken)
                    }, onFailure = { errorMessage ->
                        loading = false
                        verificationError = errorMessage
                    })
                }) {
                Text(
                    text = "LOGIN", color = Color.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xC8AC90E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Please Wait..", color = Color.White,
                fontSize = 20.sp, fontWeight = FontWeight.W700
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(
) {
    MainActivity.isPreview = true
    PitchMasterBetaTheme {
        LoginScreen()
    }
}