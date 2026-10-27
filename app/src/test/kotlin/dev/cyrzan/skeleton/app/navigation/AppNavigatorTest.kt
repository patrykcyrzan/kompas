package dev.cyrzan.skeleton.app.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.result.ResultEventBus
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.app.deeplink.createDeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.PendingDeepLinkStore
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.core.navigation.BackStackNavigator
import dev.cyrzan.skeleton.core.navigation.NavigationState
import dev.cyrzan.skeleton.core.navigation.TabbedBackStack
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.details.api.ItemPickerNavKey
import dev.cyrzan.skeleton.feature.details.api.ItemPickerResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {

    private val navigator = AppNavigator(
        DeepLinkDispatcher(createDeepLinkResolver(routes = emptyList()), PendingDeepLinkStore()),
        ResultEventBus(),
    )

    private val tabs = TopLevelTab.roots
    private val state = NavigationState(
        tabs = TabbedBackStack(tabs, tabs.associateWith { BackStackNavigator(NavBackStack(it)) }, mutableIntStateOf(0)),
        flow = NavBackStack(),
        isFullScreen = { false },
    )

    @Test
    fun `using the navigator before the state is attached fails with a clear message`() {
        val error = assertThrows(IllegalStateException::class.java) { navigator.navigate(DetailsNavKey(ItemId("7"))) }
        assertTrue(error.message!!.contains("rememberAppNavigation"))
    }

    @Test
    fun `drives the attached state`() {
        navigator.attach(state)

        navigator.navigate(DetailsNavKey(ItemId("7")))
        navigator.navigate(ItemPickerNavKey())
        navigator.goBackWithResult(ItemPickerResult, ItemId("5"))

        assertEquals(DetailsNavKey(ItemId("7")), state.current)
    }
}
