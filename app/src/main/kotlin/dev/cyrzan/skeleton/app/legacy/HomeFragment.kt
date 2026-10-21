package dev.cyrzan.skeleton.app.legacy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.R
import dev.cyrzan.skeleton.app.compose.ComposeHostFragment
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import javax.inject.Inject

/** Legacy start screen: XML layout, navigation via NavController and R.id.* – the "old way". */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    @Inject
    lateinit var session: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()

        // Old code, untouched: XML -> XML with NavController directly.
        view.findViewById<Button>(R.id.open_order).setOnClickListener {
            navController.navigate(R.id.orderDetailsFragment, OrderDetailsFragment.args(42L))
        }

        // XML -> Compose: the new feature's NavKey is handed over to the Compose host.
        view.findViewById<Button>(R.id.open_item).setOnClickListener {
            navController.navigate(R.id.composeHostFragment, ComposeHostFragment.args(listOf(DetailsNavKey("3"))))
        }
        view.findViewById<Button>(R.id.open_settings).setOnClickListener {
            navController.navigate(R.id.composeHostFragment, ComposeHostFragment.args(listOf(SettingsNavKey)))
        }

        // Guarded screen opened through the deep link pipeline (in-app link) -> login redirect when logged out.
        view.findViewById<Button>(R.id.open_profile).setOnClickListener {
            deepLinks.openInApp("kompas://profile")
        }

        // Link "from the backend": same validation and guards as external links.
        view.findViewById<Button>(R.id.open_promo).setOnClickListener {
            openBackendLink(PROMO_LINK)
        }
        view.findViewById<Button>(R.id.open_foreign).setOnClickListener {
            openBackendLink(FOREIGN_LINK)
        }
    }

    override fun onResume() {
        super.onResume()
        requireView().findViewById<TextView>(R.id.session_status).text =
            if (session.isLoggedIn) "Sesja: zalogowany" else "Sesja: niezalogowany"
    }

    private fun openBackendLink(link: String) {
        val result = deepLinks.openInApp(link)
        // Not an app link -> open plain https outside. Blocked links are never opened elsewhere.
        if (result == OpenLinkResult.Unsupported && link.startsWith("https://")) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addCategory(Intent.CATEGORY_BROWSABLE))
            } catch (_: ActivityNotFoundException) {
                // No browser – nothing to do.
            }
        }
    }

    private companion object {
        const val PROMO_LINK = "https://kompas.example.com/items/7"
        const val FOREIGN_LINK = "https://developer.android.com/guide/navigation"
    }
}
