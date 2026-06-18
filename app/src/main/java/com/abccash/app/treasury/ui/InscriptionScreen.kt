package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.viewmodel.InscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InscriptionScreen(
    onBack: () -> Unit,
    onInscriptionSuccess: (User) -> Unit,
    viewModel: InscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Configurer le callback de succès
    LaunchedEffect(Unit) {
        viewModel.onInscriptionSuccess = { user ->
            onInscriptionSuccess(user)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un compte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête
            Text(
                text = "Inscription Admin",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Créez votre compte pour devenir administrateur de votre entreprise",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Formulaire
            // Nom
            OutlinedTextField(
                value = uiState.nom,
                onValueChange = { viewModel.updateNom(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom complet *") },
                placeholder = { Text("Votre nom") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = uiState.nomError != null,
                supportingText = if (uiState.nomError != null) {
                    { Text(uiState.nomError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nom de l'entreprise
            OutlinedTextField(
                value = uiState.nomEntreprise,
                onValueChange = { viewModel.updateNomEntreprise(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom de l'entreprise *") },
                placeholder = { Text("Nom de votre entreprise") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                isError = uiState.nomEntrepriseError != null,
                supportingText = if (uiState.nomEntrepriseError != null) {
                    { Text(uiState.nomEntrepriseError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email avec vérification anti-doublon
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { 
                    viewModel.updateEmail(it)
                    // Vérification en temps réel après un délai
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email *") },
                placeholder = { Text("exemple@email.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = uiState.emailError != null,
                supportingText = if (uiState.emailError != null) {
                    { Text(uiState.emailError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Téléphone avec vérification anti-doublon
            OutlinedTextField(
                value = uiState.telephone,
                onValueChange = { 
                    viewModel.updateTelephone(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Téléphone *") },
                placeholder = { Text("+216 XX XXX XXX") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                isError = uiState.telephoneError != null,
                supportingText = if (uiState.telephoneError != null) {
                    { Text(uiState.telephoneError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mot de passe
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.updatePassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mot de passe *") },
                placeholder = { Text("Minimum 6 caractères") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                        )
                    }
                },
                isError = uiState.passwordError != null,
                supportingText = if (uiState.passwordError != null) {
                    { Text(uiState.passwordError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirmation mot de passe
            var confirmPasswordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = { viewModel.updateConfirmPassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmer le mot de passe *") },
                placeholder = { Text("Répétez votre mot de passe") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Masquer" else "Afficher"
                        )
                    }
                },
                isError = uiState.confirmPasswordError != null,
                supportingText = if (uiState.confirmPasswordError != null) {
                    { Text(uiState.confirmPasswordError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )
            
            // Erreur générale
            uiState.generalError?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bouton d'inscription
            Button(
                onClick = { viewModel.inscrire() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading &&
                         uiState.nom.isNotBlank() &&
                         uiState.nomEntreprise.isNotBlank() &&
                         uiState.email.isNotBlank() &&
                         uiState.telephone.isNotBlank() &&
                         uiState.password.isNotBlank() &&
                         uiState.confirmPassword.isNotBlank() &&
                         uiState.emailError == null &&
                         uiState.telephoneError == null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Créer mon compte",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vous serez automatiquement désigné comme administrateur",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
