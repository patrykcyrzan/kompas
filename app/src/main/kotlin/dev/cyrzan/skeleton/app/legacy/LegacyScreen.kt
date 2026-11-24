package dev.cyrzan.skeleton.app.legacy

import android.view.LayoutInflater
import android.widget.Button
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
import androidx.compose.ui.viewinterop.AndroidView
import dev.cyrzan.skeleton.app.R

/** Only the frame is Compose – the content is the XML layout, like an old Fragment's. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegacyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legacy (XML)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AndroidView(
            factory = { context ->
                LayoutInflater.from(context).inflate(R.layout.legacy_screen, null, false).apply {
                    // Proof that the chat overlay above does not swallow touches.
                    var taps = 0
                    findViewById<Button>(R.id.legacy_bottom_button).setOnClickListener { button ->
                        taps++
                        (button as Button).text = "Tapped $taps× through the overlay"
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
