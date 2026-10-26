package dev.cyrzan.skeleton.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Identity of a back stack entry: which stack, which position, which key.
 *
 * Nav3 keys saved UI state and ViewModels by `NavEntry.contentKey`, which by default is the key itself.
 * Arguments alone are not an identity: item #7 → item #8 → item #7 puts an equal key on the stack
 * twice, and both entries would share one ViewModel and one saved state (and clash while animating).
 *
 * The position is stable for the entry's whole life – stacks only change at the top, and `resetTo`
 * keeps the common prefix – and the key makes a new screen at a reused position a new entry.
 */
internal fun entryId(scope: String, index: Int, key: NavKey): String = "$scope/$index/$key"
