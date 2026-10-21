package dev.cyrzan.skeleton.app.legacy

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import dev.cyrzan.skeleton.app.R

/** Legacy screen with an argument read from the Bundle – how the old code passes data. */
class OrderDetailsFragment : Fragment(R.layout.fragment_order_details) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val orderId = requireArguments().getLong(ARG_ORDER_ID)
        view.findViewById<TextView>(R.id.order_title).text = "Zamówienie #$orderId"

        // Legacy -> new feature through a link: the legacy screen doesn't need to know the NavKey at all.
        view.findViewById<Button>(R.id.open_order_item).setOnClickListener {
            deepLinks.openInApp("kompas://items/$orderId")
        }
    }

    companion object {
        const val ARG_ORDER_ID = "orderId"

        fun args(orderId: Long): Bundle = bundleOf(ARG_ORDER_ID to orderId)
    }
}
