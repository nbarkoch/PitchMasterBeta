package com.example.pitchmasterbeta.ui.login.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainFooterButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            Color.White, disabledContainerColor = Color.LightGray
        ),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
        onClick = onClick
    ) {
        Text(
            text = text, color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}