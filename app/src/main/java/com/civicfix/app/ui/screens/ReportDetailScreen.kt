package com.civicfix.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.AsyncImage
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.ReportResponse
import com.civicfix.app.ui.theme.CivicFixBlue
import com.civicfix.app.ui.theme.PendingYellow
import com.civicfix.app.ui.theme.ResolvedGreen
import com.civicfix.app.ui.theme.RejectedRed
import com.civicfix.app.util.TwitterShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    token: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<ReportResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(reportId) {
        try {
            report = RetrofitClient.api.getReport("Bearer $token", reportId)
        } catch (e: Exception) {
            actionMessage = "Failed to load report"
        } finally {
            loading = false
        }
    }

    if (actionMessage != null) {
        Toast.makeText(context, actionMessage, Toast.LENGTH_SHORT).show()
        actionMessage = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A202C)
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicFixBlue)
            }
        } else if (report != null) {
            val r = report!!
            
            // Format dates
            val formattedDate = try {
                val zdt = ZonedDateTime.parse(r.createdAt)
                zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"))
            } catch (e: Exception) {
                r.createdAt
            }

            val statusColor = when (r.status.lowercase()) {
                "pending" -> PendingYellow
                "resolved" -> ResolvedGreen
                "rejected" -> RejectedRed
                "running" -> Color(0xFFF97316) // Orange
                "approved" -> CivicFixBlue
                else -> Color(0xFF94A3B8)
            }

            val statusBg = when (r.status.lowercase()) {
                "pending" -> Color(0xFFFFFBEB)
                "resolved" -> Color(0xFFECFDF5)
                "rejected" -> Color(0xFFFEF2F2)
                "running" -> Color(0xFFFFF7ED)
                "approved" -> Color(0xFFEFF6FF)
                else -> Color(0xFFF8FAFC)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Main Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.issueType, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Surface(shape = RoundedCornerShape(999.dp), color = statusBg) {
                                Text(
                                    r.status.replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray)
                        Text(r.description, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))

                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, tint = CivicFixBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(r.address ?: "${r.latitude}, ${r.longitude}", fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Event, null, tint = CivicFixBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(formattedDate, fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(16.dp))
                        
                        // Map showing coordinates
                        val mapLocation = LatLng(r.latitude, r.longitude)
                        val cameraPositionState = remember(r.latitude, r.longitude) {
                            CameraPositionState(CameraPosition.fromLatLngZoom(mapLocation, 15f))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = false,
                                    scrollGesturesEnabled = false,
                                    zoomGesturesEnabled = false,
                                    tiltGesturesEnabled = false,
                                    rotationGesturesEnabled = false,
                                    mapToolbarEnabled = false
                                )
                            ) {
                                Marker(
                                    state = MarkerState(position = mapLocation),
                                    title = r.issueType,
                                    snippet = r.address
                                )
                            }
                        }
                    }
                }

                if (r.imageUrl != null) {
                    AsyncImage(
                        model = RetrofitClient.getFullImageUrl(r.imageUrl),
                        contentDescription = "Report Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Actions
                val canReupload = r.status.lowercase() != "resolved"

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                var imageUri: Uri? = null
                                // If we have a URL, cache it to share it
                                if (r.imageUrl != null) {
                                    imageUri = withContext(Dispatchers.IO) { downloadImageToCache(context, r.imageUrl) }
                                }
                                
                                TwitterShareManager.shareReportToTwitter(
                                    context = context,
                                    issueType = r.issueType,
                                    description = r.description,
                                    locationAddress = r.address ?: "${r.latitude}, ${r.longitude}",
                                    latitude = r.latitude,
                                    longitude = r.longitude,
                                    dateTime = formattedDate,
                                    imageUri = imageUri
                                )
                            } catch (e: Exception) {
                                actionMessage = "Failed to launch X: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2)) // Twitter Blue
                ) {
                    Icon(Icons.Outlined.Share, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Repost to X", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (!canReupload) return@OutlinedButton
                        scope.launch {
                            isUploading = true
                            try {
                                var tempFile: File? = null
                                if (r.imageUrl != null) {
                                    val uri = withContext(Dispatchers.IO) { downloadImageToCache(context, r.imageUrl) }
                                    if (uri != null) {
                                        // create file from uri
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        tempFile = File(context.cacheDir, "reupload_${System.currentTimeMillis()}.jpg")
                                        tempFile.outputStream().use { inputStream?.copyTo(it) }
                                    }
                                }

                                if (tempFile != null) {
                                    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                                    
                                    RetrofitClient.api.createReport(
                                        token = "Bearer $token",
                                        image = imagePart,
                                        issueType = r.issueType.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        description = r.description.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        latitude = r.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                        longitude = r.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                        timestamp = null
                                    )
                                    actionMessage = "Re-uploaded Successfully"
                                    onBack()
                                } else {
                                    actionMessage = "Original image required to re-upload"
                                }
                            } catch (e: Exception) {
                                actionMessage = "Re-upload Failed: ${e.message}"
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = canReupload && !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.CloudUpload, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (canReupload) "Re-upload to Server" else "Issue Resolved (Cannot Re-upload)")
                    }
                }
            }
        }
    }
}

// Helper to download an image url to a local cache file, returning its content:// URI
private fun downloadImageToCache(context: Context, urlString: String): Uri? {
    try {
        val fullUrl = RetrofitClient.getFullImageUrl(urlString) ?: return null
        val url = URL(fullUrl)
        val connection = url.openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        val file = File(context.cacheDir, "temp_share_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
