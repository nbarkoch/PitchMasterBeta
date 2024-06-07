package com.example.pitchmasterbeta.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.getAuthViewModel
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.login.components.AuthOutlinedTextField
import com.example.pitchmasterbeta.ui.login.components.DialogButton
import com.example.pitchmasterbeta.ui.login.components.LoadingOverlay
import com.example.pitchmasterbeta.ui.login.components.MainFooterButton
import com.example.pitchmasterbeta.ui.login.components.MainTitle
import com.example.pitchmasterbeta.ui.login.components.TextVisibilityProps
import com.example.pitchmasterbeta.ui.theme.MainGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme

@Composable
fun RegisterScreen(
    navigateToLoginScreen: () -> Unit = {},
    navigateToVerificationScreen: (email: String, userName: String) -> Unit = { _, _ -> }
) {
    val viewModel = getAuthViewModel()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var loading by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(true) }

    val isValidInput: (text: String, maxLength: Int, noSpacing: Boolean) -> Boolean =
        { it, maxLength, noSpacing ->
            it.length < maxLength && !(noSpacing && it.isNotEmpty() && it.last() == ' ')
        }

    var userName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    var userNameErrorText by rememberSaveable { mutableStateOf("") }
    var passwordErrorStack by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var emailErrorText by rememberSaveable { mutableStateOf("") }

    var registrationError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val enabledConfirm = userNameErrorText.isEmpty() &&
            userName.isNotEmpty() &&
            passwordErrorStack.isEmpty() &&
            password.isNotEmpty() &&
            confirmPassword.isNotEmpty() &&
            emailErrorText.isEmpty() &&
            email.isNotEmpty()

    val onRegisterPress: () -> Unit = {
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
    }

    @Composable
    fun RegisterForm() {
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
                "Verify Password",
                password,
                R.drawable.key_icon_outline,
                isError = passwordErrorStack.isNotEmpty(),
                keyboardType = KeyboardType.Password,
                visibilityProps = TextVisibilityProps(passwordVisible) { passwordVisible = it }
            ) {
                if (isValidInput(it, 40, true)) {
                    password = it
                    passwordErrorStack = viewModel.isPasswordValid(it, confirmPassword)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            AuthOutlinedTextField(
                "Verify Password",
                confirmPassword,
                R.drawable.key_icon_outline,
                isError = passwordErrorStack.isNotEmpty(),
                keyboardType = KeyboardType.Password,
                visibilityProps = TextVisibilityProps(passwordVisible) { passwordVisible = it }
            ) {
                if (isValidInput(it, 40, true)) {
                    confirmPassword = it
                    passwordErrorStack = viewModel.isPasswordValid(it, password)
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
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = MainGradientBrush)
            .padding(10.dp)
    ) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MainTitle(Modifier.padding(top = 50.dp), text = "Sign Up")
                RegisterForm()
                registrationError?.let {
                    Text(
                        text = it, color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                DialogButton(
                    questionText = "Already have an account?",
                    buttonText = "Login Now",
                    onClick = navigateToLoginScreen
                )
                MainFooterButton(
                    text = "REGISTER",
                    enabled = enabledConfirm,
                    onClick = onRegisterPress
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(Modifier.weight(1f)) {
                    MainTitle(text = "Sign Up")
                    LazyColumn {
                        item {
                            RegisterForm()
                        }
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(10.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(Modifier.padding(10.dp)) {
                        registrationError?.let {
                            Text(
                                text = it,
                                color = Color.Red,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    DialogButton(
                        questionText = "Already have an account?",
                        buttonText = "Login Now",
                        onClick = navigateToLoginScreen
                    )
                    MainFooterButton(
                        text = "REGISTER",
                        enabled = enabledConfirm,
                        onClick = onRegisterPress
                    )
                }
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