package com.onionhost.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onionhost.app.common.generateQrCodeBitmap
import com.onionhost.app.database.entity.WebsiteType
import com.onionhost.app.tor.TorState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val activeWebsite by viewModel.activeWebsite.collectAsState()
    val torStatus by viewModel.torStatus.collectAsState()
    val metrics by viewModel.systemMetrics.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val type = if (it.toString().endsWith(".zip")) WebsiteType.ZIP else WebsiteType.SINGLE_FILE
            viewModel.importAndHost(context, it, type)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAndHost(context, it, WebsiteType.FOLDER)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OnionHost",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Anonymous Tor Web Server",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { /* Refresh metrics */ }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        // Main Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (torStatus.state) {
                    TorState.RUNNING -> Color(0xFF064E3B)
                    TorState.BOOTSTRAPPING, TorState.STARTING -> Color(0xFF78350F)
                    TorState.ERROR -> Color(0xFF7F1D1D)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (torStatus.state) {
                        TorState.RUNNING -> Icons.Default.Public
                        TorState.BOOTSTRAPPING, TorState.STARTING -> Icons.Default.Sync
                        TorState.ERROR -> Icons.Default.Warning
                        else -> Icons.Default.PublicOff
                    },
                    contentDescription = "Status Icon",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (torStatus.state) {
                        TorState.RUNNING -> "Hosting Live on Tor"
                        TorState.BOOTSTRAPPING -> "Bootstrapping Tor (${torStatus.bootstrapProgress}%)"
                        TorState.STARTING -> "Starting Engine..."
                        TorState.ERROR -> "Tor Error"
                        TorState.STOPPED -> "Hosting Inactive"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (torStatus.state == TorState.BOOTSTRAPPING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = torStatus.bootstrapProgress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // One Button Host Control
                if (torStatus.state == TorState.RUNNING || torStatus.state == TorState.BOOTSTRAPPING) {
                    Button(
                        onClick = { viewModel.stopHosting(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Hosting")
                    }
                } else {
                    Button(
                        onClick = {
                            activeWebsite?.let { viewModel.startHosting(context, it.id) }
                                ?: folderPickerLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Hosting")
                    }
                }
            }
        }

        // Active Website Details & Onion Address Card
        activeWebsite?.let { website ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Site: ${website.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (website.onionAddress.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "http://${website.onionAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Onion Address", "http://${website.onionAddress}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied Onion Address!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }

                        // QR Code display
                        website.onionAddress.generateQrCodeBitmap()?.let { qrBitmap ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Onion Address QR",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Import Buttons
        Text(
            text = "Select Content to Host",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Folder", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { filePickerLauncher.launch("application/zip") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FolderZip, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ZIP File", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Single File", fontSize = 12.sp)
            }
        }

        // Dashboard Metrics Card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Dashboard Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricTile("Total Visits", "${activeWebsite?.totalVisits ?: 0}", Icons.Default.Visibility)
                    MetricTile("Downloads", "${activeWebsite?.totalDownloads ?: 0}", Icons.Default.Download)
                    MetricTile("Memory", "${metrics.memoryUsageMb} MB", Icons.Default.Memory)
                }
            }
        }
    }
}

@Composable
fun MetricTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
