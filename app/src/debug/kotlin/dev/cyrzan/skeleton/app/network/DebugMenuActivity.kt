package dev.cyrzan.skeleton.app.network

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chuckerteam.chucker.api.Chucker
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import javax.inject.Inject

/**
 * Debug-only screen with its own launcher icon ("Skeleton Debug"), like LeakCanary's.
 * Lives outside the feature modules so release builds carry no trace of it.
 */
@AndroidEntryPoint
internal class DebugMenuActivity : ComponentActivity() {

    @Inject
    lateinit var mockSettings: MockSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SkeletonTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Debug", style = MaterialTheme.typography.headlineMedium)

                        var mocksEnabled by remember { mutableStateOf(mockSettings.isEnabled) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Mock API")
                                Text(
                                    "Answer from assets/mocks; endpoints without a file hit the backend.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                checked = mocksEnabled,
                                onCheckedChange = {
                                    mockSettings.isEnabled = it
                                    mocksEnabled = it
                                },
                            )
                        }

                        Button(onClick = { startActivity(Chucker.getLaunchIntent(this@DebugMenuActivity)) }) {
                            Text("Open Chucker")
                        }
                    }
                }
            }
        }
    }
}
