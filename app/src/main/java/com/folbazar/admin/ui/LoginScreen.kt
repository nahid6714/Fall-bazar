package com.folbazar.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.folbazar.admin.data.Repository
import com.folbazar.admin.data.Session
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth()) {
            Text("ফল বাজার Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("লগইন করে চালিয়ে যান", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("ইমেইল") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("পাসওয়ার্ড") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        Repository().signIn(email.trim(), password).fold(
                            onSuccess = { token ->
                                Session.save(context, token, email.trim())
                                loading = false
                                onLoggedIn()
                            },
                            onFailure = { e ->
                                error = e.message
                                loading = false
                            }
                        )
                    }
                },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "লগইন হচ্ছে…" else "লগইন")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "এই Supabase প্রজেক্টে অ্যাডমিনের জন্য একটি Auth ইউজার (ইমেইল/পাসওয়ার্ড) তৈরি থাকতে হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
