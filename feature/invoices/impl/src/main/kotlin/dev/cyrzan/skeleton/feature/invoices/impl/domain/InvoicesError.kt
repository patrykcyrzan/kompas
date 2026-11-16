package dev.cyrzan.skeleton.feature.invoices.impl.domain

/** What can go wrong, in terms the screen can act on – no HTTP codes or exception types. */
internal sealed interface InvoicesError {
    /** No connection or a timeout – worth retrying. */
    data object Network : InvoicesError

    /** The backend doesn't have it (any more). */
    data object NotFound : InvoicesError

    /** Server error, unexpected response, a file that couldn't be written. */
    data object Unknown : InvoicesError
}
