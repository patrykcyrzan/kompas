package dev.cyrzan.skeleton.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String) =
    findLibrary(alias).get()

internal fun VersionCatalog.pluginId(alias: String): String =
    findPlugin(alias).get().get().pluginId

/** Derives an Android namespace from the Gradle path, e.g. `:feature:home:impl` -> `dev.cyrzan.skeleton.feature.home.impl`. */
internal val Project.defaultNamespace: String
    get() = "dev.cyrzan.skeleton" + path.replace(':', '.').replace('-', '_')
