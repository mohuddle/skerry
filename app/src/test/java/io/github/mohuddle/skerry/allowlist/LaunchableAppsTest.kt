package io.github.mohuddle.skerry.allowlist

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchableAppsTest {

    @Test
    fun keepsOneRowPerPackageAndSortsByLabel() {
        val apps = distinctLaunchableApps(
            listOf(
                LaunchableApp("com.zebra", "Zebra"),
                LaunchableApp("com.alpha", "alpha"),
                LaunchableApp("com.zebra", "Zebra duplicate"),
                LaunchableApp("com.beta", "Beta"),
            ),
        )

        assertEquals(listOf("alpha", "Beta", "Zebra"), apps.map { it.label })
        assertEquals(listOf("com.alpha", "com.beta", "com.zebra"), apps.map { it.packageName })
    }

    @Test
    fun filtersByLabelOrPackageAndIgnoresBlankQueries() {
        val apps = listOf(
            LaunchableApp("com.spotify.music", "Spotify"),
            LaunchableApp("com.google.android.apps.messaging", "Messages"),
        )

        assertEquals(apps, filterLaunchableApps(apps, "  "))
        assertEquals(listOf(apps[0]), filterLaunchableApps(apps, "spot"))
        assertEquals(listOf(apps[1]), filterLaunchableApps(apps, "MESSAGING"))
        assertEquals(emptyList<LaunchableApp>(), filterLaunchableApps(apps, "calendar"))
    }
}
