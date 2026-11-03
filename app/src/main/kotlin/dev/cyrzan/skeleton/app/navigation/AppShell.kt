package dev.cyrzan.skeleton.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cyrzan.skeleton.core.navigation.NavigationDisplay
import dev.cyrzan.skeleton.core.navigation.NavigationState

/**
 * The app's top-level layout: screens above, the tab bar below.
 * The tab bar is hidden while a flow is open – full-screen screens get the whole window.
 */
@Composable
internal fun AppShell(
    navigation: AppNavigation,
    modifier: Modifier = Modifier,
) {
    val state = navigation.state
    val showBottomBar = !state.isFlowOpen

    Column(modifier) {
        NavigationDisplay(
            state = state,
            entryProvider = navigation.entryProvider,
            // The bottom bar draws behind the system navigation bar – screens above it must not pad for it again.
            modifier = Modifier
                .weight(1f)
                .then(if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) else Modifier),
            resultEventBus = navigation.resultEventBus,
            onBack = { navigation.navigator.goBack() },
        )
        AnimatedVisibility(visible = showBottomBar, enter = expandVertically(), exit = shrinkVertically()) {
            AppBottomBar(state)
        }
    }
}

@Composable
private fun AppBottomBar(state: NavigationState) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = state.tabs.currentTab == tab.root,
                onClick = { state.selectTab(tab.root) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
