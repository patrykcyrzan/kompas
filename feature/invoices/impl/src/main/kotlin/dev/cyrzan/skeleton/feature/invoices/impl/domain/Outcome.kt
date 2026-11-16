package dev.cyrzan.skeleton.feature.invoices.impl.domain

/**
 * Success or a typed error. Unlike [Result], the error is part of the signature – callers see what can go wrong
 * and `when` over it is exhaustive. Lives here until a second feature needs it, then moves to a core module.
 */
internal sealed interface Outcome<out T, out E> {
    data class Success<out T>(val value: T) : Outcome<T, Nothing>
    data class Failure<out E>(val error: E) : Outcome<Nothing, E>
}
