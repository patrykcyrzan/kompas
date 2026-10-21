package dev.cyrzan.skeleton.app.legacy

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.R
import javax.inject.Inject

/** Legacy login in a separate Activity. RESULT_OK tells the caller to resume the pending link. */
@AndroidEntryPoint
class LoginActivity : FragmentActivity(R.layout.activity_login) {

    @Inject
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.log_in).setOnClickListener {
            session.logIn()
            setResult(Activity.RESULT_OK)
            finish()
        }
        findViewById<Button>(R.id.cancel).setOnClickListener { finish() }
    }
}
