package com.example.pitchmasterbeta.ui.login.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainTitle(modifier: Modifier = Modifier, text: String) {
    Box(
        modifier
            .padding(vertical = 20.dp)
            .padding(10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 23.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W700
        )
    }
}