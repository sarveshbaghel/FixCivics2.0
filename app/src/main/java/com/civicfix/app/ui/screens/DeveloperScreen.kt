package com.civicfix.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicfix.app.R
import com.civicfix.app.ui.theme.CivicFixBlue
import com.civicfix.app.ui.theme.CivicFixBlueDark

// ── Colors ──────────────────────────────────────────────────────────────────
private val FadeGreen = Color(0xFF34D399)       // card accent / header tint
private val FadeGreenDark = Color(0xFF059669)    // darker green for gradient
private val GlowGreen = Color(0xFF10B981)        // ambient green glow
private val DarkCard = Color(0xFF1E293B)          // dark middle area for orgs

// ── Organisation data ───────────────────────────────────────────────────────
private data class Organisation(
    val name: String,
    val role: String
)

// ── Developer data ──────────────────────────────────────────────────────────
private data class Developer(
    val name: String,
    val role: String,
    val department: String,
    val email: String,
    val linkedInUrl: String,
    val githubUrl: String,
    /** Drawable resource ID – put photos in res/drawable as dev_priyanjal.jpg / dev_sarvesh.jpg */
    val photoResId: Int?,
    val organisations: List<Organisation>
)

private val allDevelopers = listOf(
    Developer(
        name = "Priyanjal Paliwal",
        role = "Full-Stack Developer",
        department = "Computer Science and Engineering",
        email = "paliwalpriyanjal@gmail.com",
        linkedInUrl = "https://www.linkedin.com/in/priyanjal-paliwal-806534331",
        githubUrl = "https://github.com/paliwalpriyanjal-hash",
        photoResId = getDrawableOrNull("dev_priyanjal"),
        organisations = listOf(
            Organisation("Madhav Institute of Technology & Science", "B.Tech — AI & ML"),
        )
    ),
    Developer(
        name = "Sarvesh Baghel",
        role = "Full-Stack Developer",
        department = "Computer Science and Engineering",
        email = "sarveshsingh8462@gmail.com",
        linkedInUrl = "https://www.linkedin.com/in/sarvesh-baghel-b3a726274",
        githubUrl = "https://github.com/sarveshbaghel",
        photoResId = getDrawableOrNull("dev_sarvesh"),
        organisations = listOf(
            Organisation("Madhav Institute of Technology & Science", "B.Tech — CSE"),
            Organisation("SunitIQ", "Member"),
        )
    )
)

/**
 * Safely resolve a drawable resource ID by name.
 * Returns null if the resource doesn't exist yet (so the app still compiles
 * before the developer photos are added).
 */
private fun getDrawableOrNull(name: String): Int? {
    return try {
        val field = R.drawable::class.java.getField(name)
        field.getInt(null)
    } catch (_: Exception) {
        null
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    val developers = remember { allDevelopers.shuffled() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(FadeGreenDark, FadeGreen)
                        )
                    )
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Meet the Team",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The minds behind CivicFix",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            developers.forEach { dev ->
                DeveloperProfileCard(dev)
                Spacer(Modifier.height(24.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "--",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Developer Card ──────────────────────────────────────────────────────────
@Composable
private fun DeveloperProfileCard(developer: Developer) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Green ambient glow ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .greenGlow(
                    color = GlowGreen,
                    blurRadius = 36.dp,
                    alpha = 0.4f
                )
        )

        // ── Card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                // ═══ Collapsed: green header + avatar + name ═══
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    FadeGreen.copy(alpha = 0.15f),
                                    FadeGreen.copy(alpha = 0.05f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(FadeGreen, FadeGreenDark)
                                    ),
                                    shape = CircleShape
                                )
                                .background(FadeGreen.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (developer.photoResId != null) {
                                Image(
                                    painter = painterResource(id = developer.photoResId),
                                    contentDescription = "${developer.name} photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback: initials
                                Text(
                                    developer.name.split(" ").map { it.first() }.joinToString(""),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FadeGreenDark
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                developer.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                developer.role,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = FadeGreen
                            )
                        }

                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(chevronRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ═══ Expanded content ═══
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Column {
                        // ── Dark middle: organisations ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard)
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Business,
                                        contentDescription = null,
                                        tint = FadeGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Organisations",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FadeGreen
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                developer.organisations.forEachIndexed { index, org ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(FadeGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.AccountBalance,
                                                contentDescription = null,
                                                tint = FadeGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                org.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                lineHeight = 18.sp
                                            )
                                            Text(
                                                org.role,
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                    if (index < developer.organisations.size - 1) {
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Department chip
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(FadeGreen.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "</>",
                                        fontSize = 12.sp,
                                        color = FadeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        developer.department,
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        // ── Bottom: social icons ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SocialIconButton(
                                icon = Icons.Outlined.Email,
                                contentDescription = "Email",
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${developer.email}"))
                                    )
                                }
                            )
                            Spacer(Modifier.width(20.dp))
                            SocialIconButton(
                                icon = Icons.Outlined.Person,
                                contentDescription = "LinkedIn",
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(developer.linkedInUrl))
                                    )
                                }
                            )
                            Spacer(Modifier.width(20.dp))
                            SocialIconButton(
                                icon = Icons.Outlined.Code,
                                contentDescription = "GitHub",
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(developer.githubUrl))
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Green glow modifier ─────────────────────────────────────────────────────
private fun Modifier.greenGlow(
    color: Color,
    blurRadius: Dp,
    alpha: Float
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val nativePaint = paint.asFrameworkPaint()
        nativePaint.isAntiAlias = true
        nativePaint.color = color.copy(alpha = alpha).toArgb()
        nativePaint.setShadowLayer(
            blurRadius.toPx(),
            0f,
            4f,
            color.copy(alpha = alpha).toArgb()
        )
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = 20.dp.toPx(),
            radiusY = 20.dp.toPx(),
            paint = paint
        )
    }
}

// ── Social Icon Button ──────────────────────────────────────────────────────
@Composable
private fun SocialIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, FadeGreen.copy(alpha = 0.3f), CircleShape)
            .background(FadeGreen.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = FadeGreen,
            modifier = Modifier.size(20.dp)
        )
    }
}
