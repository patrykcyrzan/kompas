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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ProfileRoute(
    onSignInClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    ProfileScreen(
        isSignedIn = isSignedIn,
        onSignInClick = onSignInClick,
        onSignOutClick = viewModel::signOut,
        onOrdersClick = onOrdersClick,
        onSettingsClick = onSettingsClick,
        onAboutClick = onAboutClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (isSignedIn) {
                ListItem(headlineContent = { Text("Jan Kowalski") }, supportingContent = { Text("jan@example.com") })
                HorizontalDivider()
            } else {
                ProfileRow("Sign in", "Full screen, above the tabs", onSignInClick)
            }
            ProfileRow("My orders", "Switches to the Orders tab", onOrdersClick)
            ProfileRow("Settings", "Pushed on the Profile tab", onSettingsClick)
            ProfileRow("About", "Pushed on the Profile tab", onAboutClick)
            if (isSignedIn) {
                ProfileRow("Sign out", "Resets every tab", onSignOutClick)
            }
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
