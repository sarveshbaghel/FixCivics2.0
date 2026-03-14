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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.LoginRequest
import com.civicfix.app.ui.theme.CivicFixBlue
import com.civicfix.app.ui.theme.CivicFixBlueDark
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignup: () -> Unit
) {
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
                // Logo
                Icon(
                    Icons.Outlined.ReportProblem,
                    contentDescription = null,
                    tint = CivicFixBlue,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "CivicFix",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = CivicFixBlue
                )
                Text(
                    "Help improve your community today",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Error
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            it,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(24.dp))

                // Login Button
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val response = RetrofitClient.api.login(
                                    LoginRequest(email, password)
                                )
                                onLoginSuccess(response.accessToken)
                            } catch (e: Exception) {
                                if (e is retrofit2.HttpException) {
                                    val errorBody = e.response()?.errorBody()?.string()
                                    Log.e("LoginScreen", "Server Error: $errorBody")
                                    try {
                                        if (errorBody != null) {
                                            val jsonObject = org.json.JSONObject(errorBody)
                                            if (jsonObject.has("detail")) {
                                                val detail = jsonObject.get("detail")
                                                if (detail is org.json.JSONArray) {
                                                    val field = detail.getJSONObject(0).getJSONArray("loc").getString(1)
                                                    val msg = detail.getJSONObject(0).getString("msg")
                                                    error = "Login failed: $field - $msg"
                                                } else {
                                                    error = "Login failed: ${jsonObject.getString("detail")}"
                                                }
                                            } else {
                                                error = "Login failed: Server Error"
                                            }
                                        } else {
                                            error = "Login failed: Server Error"
                                        }
                                    } catch (jsonEx: Exception) {
                                        error = "Login failed: Server Error"
                                    }
                                } else {
                                    Log.e("LoginScreen", "Error: ${e.message}", e)
                                    error = "Login failed: ${e.localizedMessage}"
                                }
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicFixBlue)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Signing in...")
                    } else {
                        Icon(Icons.Outlined.ExitToApp, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign In", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Signup link
                TextButton(onClick = onNavigateToSignup) {
                    Text(
                        "New to CivicFix? Create account",
                        color = CivicFixBlue,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
