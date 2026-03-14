package com.civicfix.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.IssueType
import com.civicfix.app.ui.theme.CivicFixBlue
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    token: String?,
    onReportSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var issueType by remember { mutableStateOf<IssueType?>(null) }
    var description by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationText by remember { mutableStateOf("Tap to detect location") }
    var loading by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var successResponse by remember { mutableStateOf<String?>(null) }

    // Camera & Permissions state
    var showCamera by remember { mutableStateOf(false) }

    // Map Location Picker launcher
    val mapPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            data?.let {
                latitude = it.getDoubleExtra("LATITUDE", 0.0)
                longitude = it.getDoubleExtra("LONGITUDE", 0.0)
                val addr = it.getStringExtra("ADDRESS")
                locationText = addr ?: "📍 ${latitude?.format(4)}, ${longitude?.format(4)}"
            }
        }
    }

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            getLocation(context) { lat, lon, address ->
                latitude = lat
                longitude = lon
                locationText = address
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { imageUri = it } }

    // Camera permission
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCamera = true
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report an Issue", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A202C)
                )
            )
        }
    ) { padding ->
        if (showCamera) {
            CameraPreviewScreen(
                onImageCaptured = { uri ->
                    imageUri = uri
                    showCamera = false
                },
                onError = { exc ->
                    Toast.makeText(context, "Camera failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    showCamera = false
                }
            )
        } else if (successResponse != null) {
            // Success state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("✅", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("Report Submitted!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Thank you for helping improve your community.",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(24.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF6FF))
                ) {
                    Text(
                        successResponse!!,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onReportSubmitted,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicFixBlue)
                ) {
                    Text("Done")
                }
            }
        } else {
            // Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Image Upload
                Text("Upload Photo", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }

                // Issue Type Dropdown
                Text("Issue Type", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = issueType?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Select issue type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        IssueType.all().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    issueType = type
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    placeholder = { Text("Describe the issue...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    supportingText = { Text("${description.length}/500") }
                )

                // Location
                Text("Location", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Live Location", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, LocationPickerActivity::class.java)
                            if (latitude != null && longitude != null) {
                                intent.putExtra("LATITUDE", latitude!!)
                                intent.putExtra("LONGITUDE", longitude!!)
                            }
                            mapPickerLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Map, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Select from Map", fontSize = 12.sp)
                    }
                }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latitude != null) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn, null,
                            tint = if (latitude != null) Color(0xFF10B981) else CivicFixBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (latitude != null) "Location Selected" else "No Location Detected",
                                fontWeight = FontWeight.Medium, fontSize = 14.sp
                            )
                            Text(locationText, fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            try {
                                val file = copyUriToFile(context, imageUri!!)
                                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                                val response = RetrofitClient.api.createReport(
                                    token = "Bearer $token",
                                    image = imagePart,
                                    issueType = issueType!!.displayName.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    latitude = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                    longitude = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                )
                                successResponse = response.complaintText
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && imageUri != null && issueType != null
                            && description.isNotBlank() && latitude != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicFixBlue)
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Submitting...")
                    } else {
                        Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Submit Report", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// Utility functions
private fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Suppress("MissingPermission")
private fun getLocation(context: Context, onResult: (Double, Double, String) -> Unit) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fusedClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val address = getAddressFromContext(context, location.latitude, location.longitude)
            onResult(location.latitude, location.longitude, address)
        } else {
            // Default mock location for testing
            val address = getAddressFromContext(context, 40.7128, -74.0060)
            onResult(40.7128, -74.0060, address)
        }
    }.addOnFailureListener {
        val address = getAddressFromContext(context, 40.7128, -74.0060)
        onResult(40.7128, -74.0060, address) // fallback
    }
}

private fun getAddressFromContext(context: Context, lat: Double, lon: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val maxLine = address.maxAddressLineIndex
            if (maxLine >= 0) {
                address.getAddressLine(0)
            } else {
                "${address.locality ?: ""}, ${address.adminArea ?: ""}".trim(',', ' ')
            }
        } else {
            "Lat: ${lat.format(4)}, Lon: ${lon.format(4)}"
        }
    } catch (e: Exception) {
        "Lat: ${lat.format(4)}, Lon: ${lon.format(4)}"
    }
}

private fun copyUriToFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)!!
    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { output -> inputStream.copyTo(output) }
    return file
}
