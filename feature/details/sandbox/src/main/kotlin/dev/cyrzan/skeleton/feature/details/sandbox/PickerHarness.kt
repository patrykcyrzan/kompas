package dev.cyrzan.skeleton.feature.details.sandbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.core.navigation.ResultEffect
import dev.cyrzan.skeleton.feature.details.api.ItemPickerNavKey
import dev.cyrzan.skeleton.feature.details.api.ItemPickerResult
import javax.inject.Inject

/**
 * Sandbox-only screen: plays the part of a caller that waits for the picker's result
 * (in the app – e.g. a form in another feature). Contributed the same way a feature contributes its screens.
 */
@kotlinx.serialization.Serializable
internal data object PickerHarnessNavKey : NavKey

internal class PickerHarnessEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<PickerHarnessNavKey> {
            PickerHarnessScreen(onOpenPicker = { selected -> navigator.navigate(ItemPickerNavKey(selected)) })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface PickerHarnessModule {

    @Binds
    @IntoSet
    fun bindPickerHarnessEntryProvider(impl: PickerHarnessEntryProvider): EntryProviderInstaller
}

@Composable
private fun PickerHarnessScreen(onOpenPicker: (ItemId?) -> Unit) {
    // Survives rotation and process death, like a real caller's state would.
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var resultCount by rememberSaveable { mutableStateOf(0) }

    ResultEffect(ItemPickerResult) { itemId ->
        picked = itemId.value
        resultCount++
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Item picker harness", style = MaterialTheme.typography.headlineSmall)
            Text("Picked: ${picked ?: "nothing yet"}")
            // Each pick must arrive exactly once – also after rotating on the picker.
            Text("Results received: $resultCount", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onOpenPicker(picked?.let(::ItemId)) }) { Text("Open picker") }
        }
    }
}
