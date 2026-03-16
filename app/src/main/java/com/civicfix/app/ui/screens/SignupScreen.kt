package com.civicfix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.FirebaseLoginRequest
import com.civicfix.app.ui.theme.CivicFixBlue
import com.civicfix.app.ui.theme.CivicFixBlueDark
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(CivicFixBlue, CivicFixBlueDark, Color(0xFF0A3D6B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Create Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CivicFixBlue)
                Text("Join CivicFix today", fontSize = 14.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(24.dp))

                error?.let {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(8.dp)) {
                        Text(it, color = Color(0xFFEF4444), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()

                        if (trimmedName.isEmpty() || trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                            error = "All fields are required"
                            return@Button
                        }

                        scope.launch {
                            loading = true; error = null
                            try {
                                val response = RetrofitClient.api.signup(
                                    com.civicfix.app.data.models.SignupRequest(
                                        email = trimmedEmail,
                                        password = trimmedPassword,
                                        displayName = trimmedName
                                    )
                                )
                                Log.i("SignupScreen", "Signup successful for $trimmedEmail")
                                onSignupSuccess(response.accessToken)
                            } catch (e: retrofit2.HttpException) {
                                error = if (e.code() == 400) "An account with this email already exists." else "Signup failed: ${e.message()}"
                            } catch (e: java.io.IOException) {
                                error = "Network error. Please check your connection."
                            } catch (e: Exception) {
                                Log.e("SignupScreen", "Signup error: ${e.message}", e)
                                error = "Signup failed: ${e.localizedMessage}"
                            } finally { loading = false }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicFixBlue)
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp)); Text("Creating...")
                    } else {
                        Text("Create Account", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onNavigateToLogin) {
                    Text("Already have an account? Sign in", color = CivicFixBlue, fontSize = 14.sp)
                }
            }
        }
    }
}
