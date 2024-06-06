package com.example.pitchmasterbeta.ui.login.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OTPTextField(
    otp: String,
    boxSize: Int = 48,
    digitSize: Int = 40,
    digitSpacing: Int = 8,
    otpLength: Int,
    hasError: Boolean = false,
    onTextChanged: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        TextField(
            value = otp,
            onValueChange = {
                if (it.length <= otpLength) {
                    onTextChanged(it)
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = Color.Transparent,
                textColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
            ),
        )
        Row {
            repeat(otpLength) { index ->
                val isHolder = otp.length - 1 < index
                if (index > 0 && index < (otpLength)) {
                    Spacer(modifier = Modifier.width(digitSpacing.dp))
                }
                Box(
                    modifier = Modifier
                        .size(digitSize.dp)
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (hasError) Color.Red else weakColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
                ) {
                    if (isHolder) {
                        Text(
                            textAlign = TextAlign.Center, text = "*", color = weakColor
                        )
                    } else {
                        Text(
                            textAlign = TextAlign.Center,
                            text = otp[index].toString(),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}