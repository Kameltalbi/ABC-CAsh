package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUserScreen(
    onBack: () -> Unit,
    onSave: (String, String, String, String, UserRole, Set<UserPermission>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STAFF) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var selectedPermissions by remember {
        mutableStateOf(setOf(UserPermission.VIEW_INVOICES, UserPermission.ADD_PAYMENTS))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvel utilisateur") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
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
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Téléphone") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mot de passe initial") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = roleMenuExpanded,
                onExpandedChange = { roleMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedRole.name,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("Rôle") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { roleMenuExpanded = false }
                ) {
                    UserRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                selectedRole = role
                                if (role == UserRole.ADMIN) {
                                    selectedPermissions = UserPermission.entries.toSet()
                                }
                                roleMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "Permissions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            UserPermission.entries.forEach { permission ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedRole == UserRole.ADMIN || permission in selectedPermissions,
                        enabled = selectedRole != UserRole.ADMIN,
                        onCheckedChange = { checked ->
                            selectedPermissions = if (checked) {
                                selectedPermissions + permission
                            } else {
                                selectedPermissions - permission
                            }
                        }
                    )
                    Text(permission.label, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val userPermissions = if (selectedRole == UserRole.ADMIN) {
                        UserPermission.entries.toSet()
                    } else {
                        selectedPermissions
                    }
                    onSave(name, email, phone, password, selectedRole, userPermissions)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = name.isNotBlank() &&
                    email.isNotBlank() &&
                    phone.isNotBlank() &&
                    password.length >= 6,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enregistrer l'utilisateur")
            }
        }
    }
}
