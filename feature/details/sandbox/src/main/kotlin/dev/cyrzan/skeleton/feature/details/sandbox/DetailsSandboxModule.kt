package dev.cyrzan.skeleton.feature.details.sandbox

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.model.ItemCategory
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchResultsNavKey
import dev.cyrzan.skeleton.sandbox.Sandbox
import dev.cyrzan.skeleton.sandbox.Scenario

@Module
@InstallIn(SingletonComponent::class)
internal object DetailsSandboxModule {

    @Provides
    fun provideSandbox() = Sandbox(
        name = "Details",
        scenarios = listOf(
            Scenario("Item 7", DetailsNavKey(ItemId("7"))),
            // Back walks the whole chain, like a user who drilled down through related items.
            Scenario("Related items chain", DetailsNavKey(ItemId("1")), DetailsNavKey(ItemId("2")), DetailsNavKey(ItemId("3"))),
            Scenario("Unknown item", DetailsNavKey(ItemId("does-not-exist"))),
            Scenario("Item picker (with result)", PickerHarnessNavKey),
            // Search is a real screen here (see build.gradle.kts), not a placeholder.
            Scenario("From search results", SearchResultsNavKey(SearchCriteria(category = ItemCategory.Tools))),
        ),
    )
}
