package dev.cyrzan.skeleton.core.model

import kotlinx.serialization.Serializable

/*
 * Typed ids. They travel between modules (navigation keys, repositories, results), where a bare
 * String/Long says nothing: an OrderId cannot be passed where an ItemId is expected.
 * Value classes – no allocation, serialized as the bare value.
 */

@Serializable
@JvmInline
value class ItemId(val value: String) {
    override fun toString(): String = value
}

@Serializable
@JvmInline
value class OrderId(val value: Long) {
    override fun toString(): String = value.toString()
}
