package com.example.pitchmasterbeta.ui.login


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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.MainGradientBrush
import com.example.pitchmasterbeta.ui.theme.PitchMasterBetaTheme
import com.example.pitchmasterbeta.ui.theme.PurpleLight10

@Composable
fun RegistrationSuccessScreen(navigateToLoginScreen: () -> Unit = {}) {
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
                    text = "Registration Success",
                    color = Color.White,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W700
                )
            }
            Box(Modifier.padding(20.dp)) {
                Image(
                    painterResource(id = R.drawable.success_icon_big_outline),
                    contentDescription = "paper plane icon",
                    modifier = Modifier.size(80.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFD6C0F0))
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "You are now ready to go!",
                fontSize = 17.sp,
                color = Color.White,
            )
            Text(
                modifier = Modifier.clickable {
                    navigateToLoginScreen()
                },
                text = "Log In",
                fontSize = 17.sp,
                color = PurpleLight10,
                fontWeight = FontWeight.W700,
                textDecoration = TextDecoration.Underline
            )
            Spacer(modifier = Modifier.weight(1f))
        }

    }
}


@Preview(showBackground = true)
@Composable
fun RegistrationSuccessScreenPreview(
) {
    MainActivity.isPreview = true
    PitchMasterBetaTheme {
        RegistrationSuccessScreen()
    }
}