package com.example.pitchmasterbeta.ui.login

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
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
import com.example.pitchmasterbeta.ui.login.components.LoadingOverlay
import com.example.pitchmasterbeta.ui.theme.MainGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.theme.PurpleLight10

@Composable
fun RegisterScreen(
    navigateToLoginScreen: () -> Unit = {},
    navigateToVerificationScreen: (email: String, userName: String) -> Unit = { _, _ -> }
) {
    val viewModel = getAuthViewModel()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var loading by remember { mutableStateOf(false) }

    val isValidInput: (text: String, maxLength: Int, noSpacing: Boolean) -> Boolean =
        { it, maxLength, noSpacing ->
            it.length < maxLength && !(noSpacing && it.isNotEmpty() && it.last() == ' ')
        }

    var userName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var userNameErrorText by remember { mutableStateOf("") }
    var passwordErrorStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var emailErrorText by remember { mutableStateOf("") }

    var registrationError by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = MainGradientBrush)
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
                    text = "Sign Up",
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
                    .padding(20.dp),
            ) {
                AuthOutlinedTextField(
                    "User Name",
                    userName,
                    R.drawable.user_icon_outline,
                    isError = userNameErrorText.isNotEmpty(),
                    keyboardType = KeyboardType.Text
                ) {
                    if (isValidInput(it, 25, false)) {
                        userName = it
                        userNameErrorText = viewModel.isUsernameValid(it)
                    }
                }
                if (userNameErrorText.isNotEmpty()) {
                    Text(
                        text = userNameErrorText,
                        color = Color.Red,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                AuthOutlinedTextField(
                    "Password",
                    password,
                    R.drawable.key_icon_outline,
                    isError = passwordErrorStack.isNotEmpty(),
                    keyboardType = KeyboardType.Password
                ) {
                    if (isValidInput(it, 40, true)) {
                        password = it
                        passwordErrorStack = viewModel.isPasswordValid(it)
                    }
                }
                passwordErrorStack.map {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                AuthOutlinedTextField(
                    "Email",
                    email,
                    R.drawable.mail_icon_outline,
                    isError = emailErrorText.isNotEmpty(),
                    keyboardType = KeyboardType.Email
                ) {
                    if (isValidInput(it, 40, true)) {
                        email = it
                        emailErrorText = viewModel.isEmailValid(it)
                    }
                }
                if (emailErrorText.isNotEmpty()) {
                    Text(
                        text = emailErrorText,
                        color = Color.Red,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                registrationError?.let {
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
                        text = "Already have an account? ",
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                    Text(
                        modifier = Modifier.clickable {
                            navigateToLoginScreen()
                        },
                        text = "Login Now",
                        fontSize = 14.sp,
                        color = PurpleLight10,
                        fontWeight = FontWeight.W700,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
            Button(enabled = userNameErrorText.isEmpty() && userName.isNotEmpty() &&
                    passwordErrorStack.isEmpty() && password.isNotEmpty() &&
                    emailErrorText.isEmpty() && email.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    Color.White, disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    loading = true
                    viewModel.signUp(userName, password, email, onCompletion = {
                        loading = false
                        navigateToVerificationScreen(email, userName)
                    }, onFailure = { errorMessage ->
                        loading = false
                        registrationError = errorMessage
                    })
                }) {
                Text(
                    text = "REGISTER", color = Color.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    LoadingOverlay(loading)
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview(
) {
    MainActivity.isPreview = true
    PitchMasterBetaTheme {
        RegisterScreen()
    }
}