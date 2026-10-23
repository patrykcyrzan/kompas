package dev.cyrzan.skeleton.feature.profile.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(headlineContent = { Text("Jan Kowalski") }, supportingContent = { Text("jan@example.com") })
            HorizontalDivider()
            ProfileRow("My orders", "Switches to the Orders tab", onOrdersClick)
            ProfileRow("Settings", "Pushed on the Profile tab", onSettingsClick)
            ProfileRow("About", "Pushed on the Profile tab", onAboutClick)
        }
    }
}

@Composable
private fun ProfileRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Kompas 0.1.0")
        }
    }
}
