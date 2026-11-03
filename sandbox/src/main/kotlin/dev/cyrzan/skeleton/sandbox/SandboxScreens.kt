package dev.cyrzan.skeleton.sandbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import kotlinx.serialization.Serializable

/** The launcher: the root of the sandbox's only tab. */
@Serializable
internal data object SandboxHomeNavKey : NavKey

@Composable
internal fun SandboxHomeScreen(
    sandbox: Sandbox,
    session: SessionRepository,
    onScenarioClick: (Scenario) -> Unit,
) {
    val isSignedIn by session.isSignedIn.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.safeDrawingPadding()) {
            item {
                Text(
                    text = "${sandbox.name} sandbox",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Signed in") },
                    supportingContent = { Text("Fake session, shared by all scenarios") },
                    trailingContent = {
                        Switch(checked = isSignedIn, onCheckedChange = { if (it) session.signIn() else session.signOut() })
                    },
                )
                HorizontalDivider()
            }
            items(sandbox.scenarios, key = Scenario::name) { scenario ->
                ListItem(
                    headlineContent = { Text(scenario.name) },
                    supportingContent = { Text(scenario.backStack.joinToString(" → ")) },
                    modifier = Modifier.clickable { onScenarioClick(scenario) },
                )
            }
        }
    }
}

/** Another feature's screen: its impl is not part of this sandbox. Shows where the feature wanted to go. */
@Composable
internal fun NotInSandboxScreen(key: NavKey, onBack: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Not in this sandbox", style = MaterialTheme.typography.headlineSmall)
            Text(key.toString(), fontFamily = FontFamily.Monospace)
            Text(
                text = "Navigation works – this screen belongs to another feature.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
