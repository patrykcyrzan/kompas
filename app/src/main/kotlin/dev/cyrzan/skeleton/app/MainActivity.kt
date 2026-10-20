package dev.cyrzan.skeleton.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.navigation.SkeletonNavHost
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SkeletonTheme {
                SkeletonNavHost(entryProviderInstallers = entryProviderInstallers)
            }
        }
    }
}
