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
