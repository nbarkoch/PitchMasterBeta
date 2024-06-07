package com.example.pitchmasterbeta.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight.Companion.W600
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.MainActivity.Companion.getAuthViewModel
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.model.LoginSharedPreferences
import com.example.pitchmasterbeta.ui.login.components.AuthOutlinedTextField
import com.example.pitchmasterbeta.ui.login.components.DialogButton
import com.example.pitchmasterbeta.ui.login.components.LoadingOverlay
import com.example.pitchmasterbeta.ui.login.components.MainFooterButton
import com.example.pitchmasterbeta.ui.login.components.MainTitle
import com.example.pitchmasterbeta.ui.login.components.TextVisibilityProps
import com.example.pitchmasterbeta.ui.theme.MainGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.theme.PurpleLight10
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navigateToWorkspace: (String) -> Unit = {},
    navigateToRegistrationScreen: () -> Unit = {}
) {
    val viewModel = getAuthViewModel()
    val coroutineScope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var loading by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var userName by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }
    var verificationError by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var checkBoxChecked by rememberSaveable {
        mutableStateOf(false)
    }

    val isValidInput: (text: String, maxLength: Int, noSpacing: Boolean) -> Boolean =
        { it, maxLength, noSpacing ->
            it.length < maxLength && !(noSpacing && it.isNotEmpty() && it.last() == ' ')
        }

    val onLoginPress: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        loading = true
        viewModel.login(userName, password, onCompletion = { jwtToken ->
            loading = false
            coroutineScope.launch {
                if (checkBoxChecked) {
                    LoginSharedPreferences.saveLastLoginUser(userName, password)
                } else {
                    LoginSharedPreferences.forgetLastLoginUser()
                }
            }
            navigateToWorkspace(jwtToken)
        }, onFailure = { errorMessage ->
            loading = false
            verificationError = errorMessage
        })
    }


    LaunchedEffect(Unit) {
        if (userName.isEmpty() && password.isEmpty()) {
            coroutineScope.launch {
                val lastLoginUser = LoginSharedPreferences.getLastLoginUser()
                if (lastLoginUser != null) {
                    userName = lastLoginUser.username
                    password = lastLoginUser.password
                    checkBoxChecked = true
                }
            }
        }
    }

    @Composable
    fun LoginForm() {
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
                keyboardType = KeyboardType.Password,
                visibilityProps = TextVisibilityProps(passwordVisible) {
                    passwordVisible = it
                }
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
                    modifier = Modifier.clickable { },
                    text = "Forgot Password?",
                    fontWeight = W600,
                    fontSize = 13.sp,
                    color = PurpleLight10,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkBoxChecked, onCheckedChange = {
                        checkBoxChecked = it
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PurpleLight10,
                        uncheckedColor = Color.White
                    )
                )
                Text(
                    modifier = Modifier.clickable {},
                    text = "Remember me",
                    fontWeight = W600,
                    fontSize = 12.sp,
                    color = PurpleLight10,
                )
            }
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
                MainTitle(Modifier.padding(top = 50.dp), text = "Sign In")
                LoginForm()
                verificationError?.let {
                    Text(
                        text = it, color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                DialogButton(
                    questionText = "Don't have an account?",
                    buttonText = "Sign Up Now",
                    onClick = navigateToRegistrationScreen
                )
                MainFooterButton(
                    text = "LOGIN",
                    enabled = userName.isNotEmpty() &&
                            password.isNotEmpty(),
                    onClick = onLoginPress
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(Modifier.weight(1f)) {
                    MainTitle(text = "Sign In")
                    LoginForm()
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(10.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(Modifier.padding(10.dp)) {
                        verificationError?.let {
                            Text(
                                text = it,
                                color = Color.Red,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    DialogButton(
                        questionText = "Don't have an account?",
                        buttonText = "Sign Up Now",
                        onClick = navigateToRegistrationScreen
                    )
                    MainFooterButton(
                        text = "LOGIN",
                        enabled = userName.isNotEmpty() &&
                                password.isNotEmpty(),
                        onClick = onLoginPress
                    )
                }
            }
        }
    }
    LoadingOverlay(loading)
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