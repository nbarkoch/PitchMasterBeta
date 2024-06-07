package com.example.pitchmasterbeta.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.login.components.LoadingOverlay
import com.example.pitchmasterbeta.ui.login.components.MainFooterButton
import com.example.pitchmasterbeta.ui.login.components.MainTitle
import com.example.pitchmasterbeta.ui.login.components.OTPTextField
import com.example.pitchmasterbeta.ui.theme.MainGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.theme.PurpleLight10
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun EmailVerificationScreen(
    emailToVerify: String,
    username: String,
    navigateToRegistrationSuccessScreen: () -> Unit = {}
) {
    val viewModel = MainActivity.getAuthViewModel()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val otpLength = 6

    var verificationCode by remember {
        mutableStateOf("")
    }
    var loading by remember {
        mutableStateOf(false)
    }
    var verificationSpecialMessage by remember {
        mutableStateOf("")
    }
    var somethingIsWrong by remember {
        mutableStateOf(false)
    }
    var redEnv by remember {
        mutableStateOf(false)
    }

    val onVerificationCodeEntered = { code: String ->
        verificationSpecialMessage = ""
        somethingIsWrong = false
        loading = true
        focusManager.clearFocus()
        keyboardController?.hide()
        viewModel.confirmVerificationCode(username, code, onCompletion = {
            loading = false
            navigateToRegistrationSuccessScreen()
        }, onFailure = {
            somethingIsWrong = true
            verificationSpecialMessage = "Wrong code has entered, try again.."
            loading = false
        })
    }

    val resendVerificationToEmail = {
        viewModel.resendVerificationToEmail(username, onCompletion = {
            focusManager.clearFocus()
            keyboardController?.hide()
            verificationSpecialMessage =
                "We sent you another verification code,\n check it out!"
            somethingIsWrong = false
            verificationCode = ""
        }, onFailure = {
            verificationSpecialMessage =
                "Something went wrong, Please try again"
            somethingIsWrong = true
        })
    }

    val onCodeTextInputChanged: (String) -> Unit = {
        verificationCode = it
        if (verificationSpecialMessage.isNotEmpty()) {
            verificationSpecialMessage = ""
            somethingIsWrong = false
        }
        if (it.length == otpLength) {
            onVerificationCodeEntered(it)
        }
    }

    LaunchedEffect(somethingIsWrong) {
        if (somethingIsWrong) {
            withContext(Dispatchers.Main) {
                redEnv = true
                delay(2000)
                redEnv = false
                verificationCode = ""
            }
        } else {
            redEnv = false
        }
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
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                MainTitle(Modifier.padding(top = 50.dp), text = "Verify Your Email")
                PaperPlaneImage()
                Text(textAlign = TextAlign.Center, text = emailToVerify, color = PurpleLight10)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    textAlign = TextAlign.Center,
                    text = "To complete the registration\nyou'll need to verify your email address",
                    color = Color.White
                )
            } else {
                MainTitle(text = "Verify Your Email")
                Text(textAlign = TextAlign.Center, text = emailToVerify, color = PurpleLight10)
                Text(
                    textAlign = TextAlign.Center,
                    text = "To complete the registration you'll need to verify your email address",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            OTPTextField(
                otp = verificationCode,
                boxSize = 48,
                digitSize = 40,
                digitSpacing = 8,
                otpLength = otpLength,
                hasError = redEnv,
                onTextChanged = onCodeTextInputChanged,
            )
            if (verificationSpecialMessage.isNotEmpty()) {
                Text(
                    textAlign = TextAlign.Center,
                    text = verificationSpecialMessage,
                    color = if (somethingIsWrong) Color.Red else Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            SimpleBaseButton(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .align(Alignment.CenterHorizontally),
                text = "Send Verification Code Again",
                onClick = resendVerificationToEmail
            )
            MainFooterButton(text = "CONFIRM", enabled = verificationCode.length == otpLength) {
                onVerificationCodeEntered(verificationCode)
            }
        }
    }
    LoadingOverlay(loading)
}


@Composable
fun PaperPlaneImage() {
    Box(Modifier.padding(20.dp)) {
        Image(
            painterResource(id = R.drawable.paper_plane_icon_big_outline),
            contentDescription = "paper plane icon",
            modifier = Modifier.size(60.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}

@Composable
fun SimpleBaseButton(modifier: Modifier, text: String, onClick: () -> Unit) {
    Box(modifier) {
        Text(
            modifier = Modifier.clickable(onClick = onClick),
            text = text,
            fontSize = 14.sp,
            color = PurpleLight10,
            fontWeight = FontWeight.W700,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmailVerificationScreenPreview(
) {
    MainActivity.isPreview = true
    PitchMasterBetaTheme {
        EmailVerificationScreen("pumpityouknow@gmail.com", "pumpit")
    }
}