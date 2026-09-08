package com.civicfix.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.ReportResponse
import com.civicfix.app.ui.theme.CivicFixBlue
import com.civicfix.app.ui.theme.PendingYellow
import com.civicfix.app.ui.theme.ResolvedGreen
import com.civicfix.app.ui.theme.RejectedRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    token: String?,
    onBack: () -> Unit
) {
    var reports by remember { mutableStateOf<List<ReportResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val data = RetrofitClient.api.getReports("Bearer $token")
            reports = data.reports
        } catch (e: Exception) {
            // Handle error
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CivicFixBlue)
            }
        } else if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCBD5E1)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No reports yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        "Submit an issue to see it here",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        "Recent Submissions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(reports) { report ->
                    ReportCard(report)
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportResponse) {
    val statusColor = when (report.status) {
        "pending" -> PendingYellow
        "resolved" -> ResolvedGreen
        "rejected" -> RejectedRed
        "approved" -> CivicFixBlue
        else -> Color(0xFF94A3B8)
    }

    val statusBg = when (report.status) {
        "pending" -> Color(0xFFFFFBEB)
        "resolved" -> Color(0xFFECFDF5)
        "rejected" -> Color(0xFFFEF2F2)
        "approved" -> Color(0xFFEFF6FF)
        else -> Color(0xFFF8FAFC)
    }

    val issueEmoji = when (report.issueType) {
        "Pothole" -> "🕳️"
        "Garbage" -> "🗑️"
        "Broken streetlight" -> "💡"
        "Water leakage" -> "💧"
        else -> "📋"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail
            if (report.thumbnailUrl != null || report.imageUrl != null) {
                AsyncImage(
                    model = report.thumbnailUrl ?: report.imageUrl,
                    contentDescription = "Report image",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$issueEmoji ${report.issueType}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusBg
                    ) {
                        Text(
                            report.status.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    report.description,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        report.address ?: "${report.latitude}, ${report.longitude}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    report.createdAt.take(10),
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}
