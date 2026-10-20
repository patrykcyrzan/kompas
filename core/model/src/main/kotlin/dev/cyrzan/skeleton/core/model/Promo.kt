package dev.cyrzan.skeleton.core.model

/** Content from the backend that points somewhere via a [link] (an app link or an external page). */
data class Promo(
    val title: String,
    val link: String,
)
