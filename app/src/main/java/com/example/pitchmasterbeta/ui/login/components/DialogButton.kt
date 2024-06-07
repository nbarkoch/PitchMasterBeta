package com.example.pitchmasterbeta.ui.login.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.ui.theme.PurpleLight10

@Composable
fun DialogButton(
    modifier: Modifier = Modifier,
    questionText: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(
        modifier.padding(vertical = 25.dp, horizontal = 10.dp)
    ) {
        Row {
            Text(
                text = "$questionText ",
                fontSize = 14.sp,
                color = Color.White,
            )
            Text(
                modifier = Modifier.clickable(onClick = onClick),
                text = buttonText,
                fontSize = 14.sp,
                color = PurpleLight10,
                fontWeight = FontWeight.W700,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}