package io.github.mohuddle.skerry.allowlist

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class LaunchableApp(
    val packageName: String,
    val label: String,
)

fun distinctLaunchableApps(entries: List<LaunchableApp>): List<LaunchableApp> {
    return entries
        .distinctBy { it.packageName }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}

fun filterLaunchableApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
    val needle = query.trim()
    if (needle.isEmpty()) return apps
    return apps.filter { app ->
        app.label.contains(needle, ignoreCase = true) ||
            app.packageName.contains(needle, ignoreCase = true)
    }
}

fun loadLaunchableApps(packageManager: PackageManager): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }
    return distinctLaunchableApps(
        infos.map { info ->
            LaunchableApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(packageManager).toString(),
            )
        },
    )
}
