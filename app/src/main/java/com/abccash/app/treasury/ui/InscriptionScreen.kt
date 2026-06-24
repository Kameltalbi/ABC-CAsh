package com.abccash.app.treasury.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abccash.app.R
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.viewmodel.InscriptionViewModel
import com.abccash.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InscriptionScreen(
    onInscriptionSuccess: (User) -> Unit,
    googleBackupManager: GoogleBackupManager,
    onGoogleConnected: (String?) -> Unit,
    onRestoreFromGoogle: ((User?, String?) -> Unit) -> Unit,
    viewModel: InscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }
    var connectedGoogleEmail by remember {
        mutableStateOf(googleBackupManager.getSignedInEmail())
    }

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

    LaunchedEffect(Unit) {
        viewModel.onInscriptionSuccess = onInscriptionSuccess
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                text = stringResource(R.string.signup_admin_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.signup_admin_subtitle),
                fontSize = 15.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            AuthGoogleSection(
                googleBackupManager = googleBackupManager,
                connectedEmail = connectedGoogleEmail,
                onRestoreFromGoogle = onRestoreFromGoogle,
                onRestoreSuccess = onInscriptionSuccess,
                onGoogleConnected = { email ->
                    connectedGoogleEmail = email
                    onGoogleConnected(email)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthSectionDivider(label = stringResource(R.string.signup_new_business))

            Spacer(modifier = Modifier.height(20.dp))

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
                        label = "${stringResource(R.string.full_name)} *",
                        value = uiState.nom,
                        onValueChange = viewModel::updateNom,
                        placeholder = stringResource(R.string.your_name),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = uiState.nomError != null,
                        errorText = uiState.nomError?.let { stringResource(authErrorRes(it)) },
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthLabeledField(
                        label = "${stringResource(R.string.company_name)} *",
                        value = uiState.nomEntreprise,
                        onValueChange = viewModel::updateNomEntreprise,
                        placeholder = stringResource(R.string.company_name_placeholder),
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        isError = uiState.nomEntrepriseError != null,
                        errorText = uiState.nomEntrepriseError?.let { stringResource(authErrorRes(it)) },
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthLabeledField(
                        label = "${stringResource(R.string.email)} *",
                        value = uiState.email,
                        onValueChange = viewModel::updateEmail,
                        placeholder = stringResource(R.string.email_placeholder),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        isError = uiState.emailError != null,
                        errorText = uiState.emailError?.let { stringResource(authErrorRes(it)) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthLabeledField(
                        label = stringResource(R.string.phone),
                        value = uiState.telephone,
                        onValueChange = viewModel::updateTelephone,
                        placeholder = stringResource(R.string.phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        isError = uiState.telephoneError != null,
                        errorText = uiState.telephoneError?.let { stringResource(authErrorRes(it)) },
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthLabeledField(
                        label = "${stringResource(R.string.password)} *",
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        placeholder = stringResource(R.string.password_placeholder_min),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        isError = uiState.passwordError != null,
                        errorText = uiState.passwordError?.let { stringResource(authErrorRes(it)) },
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    text = stringResource(
                                        if (showPassword) R.string.hide_password else R.string.show_password
                                    ),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthLabeledField(
                        label = "${stringResource(R.string.confirm_password)} *",
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        placeholder = stringResource(R.string.repeat_password),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        isError = uiState.confirmPasswordError != null,
                        errorText = uiState.confirmPasswordError?.let { stringResource(authErrorRes(it)) },
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    text = stringResource(
                                        if (showPassword) R.string.hide_password else R.string.show_password
                                    ),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        fieldColors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppColors.InfoBackground
                    ) {
                        Text(
                            text = stringResource(R.string.signup_solo_note),
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    uiState.generalError?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = viewModel::inscrire,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.signup_create_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun authErrorRes(key: String): Int = when (key) {
    "email_required" -> R.string.email_required
    "password_required" -> R.string.password_required
    "login_failed" -> R.string.login_failed
    "password_min_chars" -> R.string.password_min_chars
    "passwords_dont_match" -> R.string.passwords_dont_match
    "email_taken" -> R.string.email_taken
    "name_required" -> R.string.name_required
    "company_required" -> R.string.company_required
    "phone_taken" -> R.string.phone_taken
    else -> R.string.error_generic
}
