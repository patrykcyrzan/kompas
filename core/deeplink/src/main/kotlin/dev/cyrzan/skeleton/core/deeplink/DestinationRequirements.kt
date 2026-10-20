package dev.cyrzan.skeleton.core.deeplink

/*
 * Markers a feature puts on its navigation key to declare security requirements.
 * The feature only *declares* them – guards configured by the app *enforce* them.
 */

/** Needs a signed-in user. Without a session the user is redirected (see [AuthenticationGuard]). */
interface RequiresAuthentication

/** May be opened only by the app itself ([DeepLinkSource.InApp]), never from outside (see [InAppOnlyGuard]). */
interface InAppOnly

/** Exists for development only; rejected in release builds (see [DebugOnlyGuard]). */
interface DebugOnly
