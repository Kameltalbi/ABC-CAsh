package com.abccash.app.treasury.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun AccountSetupScreen(
    repository: TreasuryRepository,
    onSetupComplete: (User) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val emailRequired = stringResource(R.string.email_required)
    val passwordMinChars = stringResource(R.string.password_min_chars)
    val passwordsDontMatch = stringResource(R.string.passwords_dont_match)
    val errorGeneric = stringResource(R.string.error_generic)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = AppColors.Border,
        focusedTextColor = AppColors.TextPrimary,
        unfocusedTextColor = AppColors.TextPrimary,
        focusedPlaceholderColor = AppColors.TextSecondary,
        unfocusedPlaceholderColor = AppColors.TextSecondary,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = AppColors.TextSecondary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_abc_cash_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .padding(bottom = 20.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = stringResource(R.string.account_setup_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.account_setup_subtitle),
            fontSize = 14.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                AuthLabeledField(
                    label = stringResource(R.string.email),
                    value = email,
                    onValueChange = { email = it; error = null },
                    placeholder = stringResource(R.string.email_placeholder),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = false,
                    errorText = null,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    fieldColors = fieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthLabeledField(
                    label = stringResource(R.string.phone),
                    value = telephone,
                    onValueChange = { telephone = it; error = null },
                    placeholder = stringResource(R.string.phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    isError = false,
                    errorText = null,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    fieldColors = fieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthLabeledField(
                    label = stringResource(R.string.password),
                    value = password,
                    onValueChange = { password = it; error = null },
                    placeholder = stringResource(R.string.password_placeholder_min),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    isError = false,
                    errorText = null,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    fieldColors = fieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthLabeledField(
                    label = stringResource(R.string.confirm_password),
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    placeholder = stringResource(R.string.repeat_password),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    isError = false,
                    errorText = null,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    visualTransformation = PasswordVisualTransformation(),
                    fieldColors = fieldColors
                )

                error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        when {
                            email.isBlank() -> error = emailRequired
                            password.length < 6 -> error = passwordMinChars
                            password != confirmPassword -> error = passwordsDontMatch
                            else -> scope.launch {
                                isLoading = true
                                error = null
                                val setupError = repository.completeAccountSetup(email, telephone, password)
                                if (setupError != null) {
                                    error = setupError
                                } else {
                                    val user = repository.login(email, password)
                                    if (user != null) {
                                        onSetupComplete(user)
                                    } else {
                                        error = errorGeneric
                                    }
                                }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.account_setup_button),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
