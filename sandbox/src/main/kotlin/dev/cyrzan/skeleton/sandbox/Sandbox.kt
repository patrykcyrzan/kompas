package dev.cyrzan.skeleton.sandbox

import androidx.navigation3.runtime.NavKey

/**
 * What a `:feature:x:sandbox` app offers on its launcher. Provided once by the sandbox module:
 *
 * ```
 * @Provides
 * fun provideSandbox(session: SessionRepository) = Sandbox(
 *     name = "Orders",
 *     scenarios = listOf(
 *         Scenario("Orders list", OrdersNavKey),
 *         Scenario("Order #1001, signed in", OrdersNavKey, OrderDetailsNavKey(OrderId(1001))) { session.signIn() },
 *     ),
 * )
 * ```
 */
class Sandbox(
    val name: String,
    val scenarios: List<Scenario>,
) {
    init {
        val duplicates = scenarios.groupBy(Scenario::name).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Scenario names must be unique: $duplicates" }
    }
}

/**
 * A ready-made starting point: [backStack] is opened above the launcher (so "back" from its first screen
 * returns there), after [prepare] has set up whatever the screens expect (session, fake data...).
 * Full-screen keys (`fullScreen()` on their entry) open as a flow, like in the app.
 */
class Scenario(
    val name: String,
    val backStack: List<NavKey>,
    val prepare: () -> Unit = {},
) {
    constructor(name: String, vararg backStack: NavKey, prepare: () -> Unit = {}) :
        this(name, backStack.toList(), prepare)

    init {
        require(backStack.isNotEmpty()) { "Scenario '$name' has an empty back stack" }
    }
}
