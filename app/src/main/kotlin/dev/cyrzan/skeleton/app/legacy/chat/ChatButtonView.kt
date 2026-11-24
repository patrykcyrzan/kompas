package dev.cyrzan.skeleton.app.legacy.chat

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.app.R
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import dev.cyrzan.skeleton.feature.chat.api.ChatButton

/**
 * [ChatButton.Overlay] for XML layouts – a plain tag, no code in the screen that uses it.
 * Make it the last child of the root, over the whole screen:
 *
 * ```
 * <dev.cyrzan.skeleton.app.legacy.chat.ChatButtonView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:chatBottomPadding="88dp"
 *     app:chatSource="legacy_screen" />  <!-- optional, only for tracking -->
 * ```
 *
 * Covering the screen does not block it: a touch that hits nothing with pointer input in the composition
 * is not handled by this view, so the parent passes it on to the views below. Do not make the view itself
 * clickable or focusable – that would take every touch.
 *
 * Gets the button from Hilt itself, so the host does not have to be `@AndroidEntryPoint`.
 * Taps navigate through `LocalNavigator`, which reaches this view because in Kompas every XML layout
 * is embedded in the Compose navigation (`AndroidView` passes the composition locals down the View tree).
 * Outside of it – XML Fragments navigated by a NavController – the button needs a navigation contract
 * from DI instead (e.g. `ChatLauncher.open(activity)` implemented by the app).
 */
class ChatButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    /** Only for tracking – `null` reports the click without a source. */
    private val source: String?

    /** In pixels; `null` = the overlay's default. */
    private val bottomPaddingPx: Int?

    init {
        val styled = context.obtainStyledAttributes(attrs, R.styleable.ChatButtonView, defStyleAttr, 0)
        try {
            source = styled.getString(R.styleable.ChatButtonView_chatSource)
            bottomPaddingPx = if (styled.hasValue(R.styleable.ChatButtonView_chatBottomPadding)) {
                styled.getDimensionPixelSize(R.styleable.ChatButtonView_chatBottomPadding, 0)
            } else {
                null
            }
        } finally {
            styled.recycle()
        }
        // In a Fragment the View dies before the Fragment: dispose with the view's lifecycle, not on detach.
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    // Lazily: the layout preview inflates the view without a Hilt application.
    private val chatButton: ChatButton by lazy {
        EntryPointAccessors.fromApplication<ChatButtonEntryPoint>(context.applicationContext).chatButton()
    }

    @Composable
    override fun Content() {
        SkeletonTheme {
            val bottomPadding = bottomPaddingPx?.let { px -> with(LocalDensity.current) { px.toDp() } }
            if (bottomPadding != null) {
                chatButton.Overlay(source = source, bottomPadding = bottomPadding)
            } else {
                chatButton.Overlay(source = source)
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ChatButtonEntryPoint {
    fun chatButton(): ChatButton
}
