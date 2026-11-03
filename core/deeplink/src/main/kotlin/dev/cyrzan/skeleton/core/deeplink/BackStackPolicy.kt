package dev.cyrzan.skeleton.core.deeplink

/**
 * Builds the synthetic back stack for a deep link destination. Lives in the app (not in features),
 * so features don't need to know each other to describe "what's underneath".
 */
fun interface BackStackPolicy {
    fun backStackFor(destination: Destination): List<Destination>
}

/** Always puts [root] at the bottom, so "back" from a deep-linked screen lands on the start screen. */
class RootedBackStackPolicy(private val root: Destination) : BackStackPolicy {
    override fun backStackFor(destination: Destination): List<Destination> =
        if (destination == root) listOf(root) else listOf(root, destination)
}

/**
 * Builds the stack by walking up [parentOf] until a destination without a parent (a root),
 * e.g. About → Settings → Profile gives `[Profile, Settings, About]`.
 *
 * With bottom tabs the roots are the tab roots, so the first element tells the platform adapter
 * which tab to select.
 */
class ParentBackStackPolicy(
    private val parentOf: (Destination) -> Destination?,
) : BackStackPolicy {
    override fun backStackFor(destination: Destination): List<Destination> {
        val stack = ArrayDeque<Destination>()
        var current: Destination? = destination
        while (current != null) {
            check(current !in stack) { "Cycle in the destination hierarchy at $current" }
            stack.addFirst(current)
            current = parentOf(current)
        }
        return stack
    }
}
