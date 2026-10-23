package dev.cyrzan.skeleton.app.lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val PIN_LENGTH = 4

/**
 * Covers the whole app while [AppLock] is locked. Opaque and swallows touches; what is underneath
 * is hidden from accessibility services by the caller.
 *
 * Demo: PIN only. Biometrics would be a second way to call `unlock` – `BiometricPrompt` (needs a
 * `FragmentActivity`), ideally with a `CryptoObject` bound to a Keystore key that decrypts the session.
 */
@Composable
internal fun LockScreen(appLock: AppLock, modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    // Back never reveals the app: it only sends it to the background.
    BackHandler { activity?.moveTaskToBack(true) }

    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    fun onDigit(digit: Char) {
        if (pin.length == PIN_LENGTH) return
        pin += digit
        if (pin.length < PIN_LENGTH) return
        message = when (val result = appLock.unlock(pin)) {
            UnlockResult.Unlocked -> null
            is UnlockResult.WrongPin -> "Wrong PIN, ${result.attemptsLeft} attempt(s) left"
            UnlockResult.SignedOut -> null
        }
        pin = ""
    }

    Surface(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Enter PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            PinDots(filled = pin.length)
            Text(
                text = message.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.height(40.dp).padding(top = 12.dp),
            )
            Keypad(onDigit = ::onDigit, onDelete = { pin = pin.dropLast(1) })
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = appLock::signOut) { Text("Not you? Sign out") }
        }
    }
}

@Composable
private fun PinDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PIN_LENGTH) { index ->
            val color = if (index < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(16.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val rows = listOf("123", "456", "789", " 0<")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        when (key) {
                            ' ' -> Unit
                            '<' -> TextButton(onClick = onDelete) { Text("⌫", style = MaterialTheme.typography.headlineSmall) }
                            else -> TextButton(onClick = { onDigit(key) }) {
                                Text(key.toString(), style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
