package com.example.pitchmasterbeta.ui.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.ui.theme.PurpleDark10
import com.example.pitchmasterbeta.ui.theme.PurpleDarkWeak10
import com.example.pitchmasterbeta.ui.theme.PurpleLight10

data class TextVisibilityProps(
    val textVisible: Boolean = true,
    val onVisibleIconClick: (Boolean) -> Unit,
)
@Composable
fun AuthOutlinedTextField(
    placeHolder: String,
    text: String,
    leadingIconResourceId: Int,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visibilityProps: TextVisibilityProps? = null,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = onValueChange,
        textStyle = TextStyle.Default.copy(
            fontSize = 18.sp
        ),
        shape = MaterialTheme.shapes.small,
        placeholder = { Text(placeHolder, color = PurpleDarkWeak10) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = PurpleDarkWeak10,
            cursorColor = Color.White,
            textColor = Color.White,
            backgroundColor = PurpleDark10,
            disabledBorderColor = Color.Gray,
            disabledLabelColor = Color.Gray,
            disabledPlaceholderColor = Color.Gray,
            disabledTextColor = Color.Gray
        ),
        visualTransformation = if (visibilityProps?.textVisible != false) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = {
            Image(
                painterResource(id = leadingIconResourceId),
                contentDescription = "",
                modifier = Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        singleLine = true,
        isError = isError,
        trailingIcon = {
            if (visibilityProps != null) {
                Image(
                    painterResource(id = if (visibilityProps.textVisible) R.drawable.off_outlined_visibility else R.drawable.outlined_visibility_eye),
                    contentDescription = if (visibilityProps.textVisible) "Hide password" else "Show password",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { visibilityProps.onVisibleIconClick(!visibilityProps.textVisible) },
                    colorFilter = ColorFilter.tint(PurpleLight10)
                )
            }
        }
    )
}