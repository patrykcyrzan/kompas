package dev.cyrzan.skeleton.feature.chat.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cyrzan.skeleton.feature.chat.impl.sdk.FakeChatSdk

/** Our frame (top bar, back) around the SDK's conversation – like a screen hosting the vendor's Fragment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatRoute(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        FakeChatSdk.Conversation(Modifier.fillMaxSize().padding(padding))
    }
}
