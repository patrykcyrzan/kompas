package dev.cyrzan.skeleton.feature.chat.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the user left the chat button – one place for all screens, for as long as the process lives.
 *
 * Kept as fractions of the area the button can move in (0 = left/top, 1 = right/bottom), not pixels:
 * screens differ in size and bottom padding, and "bottom right" must stay bottom right on each of them.
 */
@Singleton
internal class ChatButtonPosition @Inject constructor() {

    var fraction: Offset by mutableStateOf(Offset(1f, 1f))
}
