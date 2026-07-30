package com.onionhost.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.onionhost.app.R.drawable.app_logo),
            contentDescription = "OnionHost Logo",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OnionHost v1.1.0",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Production-grade Android Onion Web Hosting",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Open Source License", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "MIT License - Free and open source forever. Built with Tor v3 Hidden Services, Jetpack Compose, Hilt, Room, and NanoHTTPD/Ktor.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Created by Security Talent",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = { uriHandler.openUri("https://securitytalent.net") }) {
            Text("securitytalent.net")
        }

        Text(
            text = "Follow Security Talent",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocialLink("X (Twitter)", Icons.Filled.AlternateEmail, "https://x.com/Securi3yTalent")
            SocialLink("GitHub", Icons.Filled.Code, "https://github.com/securityTalent/")
            SocialLink("Telegram", Icons.AutoMirrored.Filled.Send, "https://t.me/Securi3yTalent")
            SocialLink("Facebook", Icons.Filled.ThumbUp, "https://www.facebook.com/Securi3ytalent")
            SocialLink("YouTube", Icons.Filled.PlayCircle, "https://www.youtube.com/@SecurityTalent")
            SocialLink("Instagram", Icons.Filled.PhotoCamera, "https://www.instagram.com/Securi3ytalent")
        }
    }
}

@Composable
private fun SocialLink(label: String, icon: ImageVector, url: String) {
    val uriHandler = LocalUriHandler.current

    IconButton(onClick = { uriHandler.openUri(url) }) {
        Icon(imageVector = icon, contentDescription = "$label profile")
    }
}
