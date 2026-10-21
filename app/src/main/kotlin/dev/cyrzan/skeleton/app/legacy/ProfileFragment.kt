package dev.cyrzan.skeleton.app.legacy

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.R
import javax.inject.Inject

/** Legacy screen behind login (LegacyDestination.Profile is RequiresAuthentication). */
@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    @Inject
    lateinit var session: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.log_out).setOnClickListener {
            session.logOut()
            findNavController().popBackStack()
        }
    }
}
