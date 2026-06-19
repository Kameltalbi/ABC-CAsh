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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUserScreen(
    onBack: () -> Unit,
    onSave: (String, String, String, String, UserRole, Set<UserPermission>, (String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var selectedPermissions by remember {
        mutableStateOf(setOf(UserPermission.VIEW_INVOICES, UserPermission.ADD_PAYMENTS))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_user)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                label = { Text(stringResource(R.string.name)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.email)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.phone)) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.initial_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            OutlinedTextField(
                value = "STAFF",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.role)) },
                readOnly = true,
                supportingText = { Text(stringResource(R.string.only_initial_admin)) }
            )

            saveError?.let { error ->
                Text(text = error, color = Color(0xFFF44336), fontSize = 13.sp)
            }

            Text(
                text = stringResource(R.string.permissions),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            UserPermission.entries.forEach { permission ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = permission in selectedPermissions,
                        onCheckedChange = { checked ->
                            selectedPermissions = if (checked) {
                                selectedPermissions + permission
                            } else {
                                selectedPermissions - permission
                            }
                        }
                    )
                    Text(permission.localizedLabel(), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    saveError = null
                    onSave(name, email, phone, password, UserRole.STAFF, selectedPermissions) { error ->
                        if (error == null) {
                            onBack()
                        } else {
                            saveError = error
                        }
                    }
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
                Text(stringResource(R.string.save_user))
            }
        }
    }
}
