package io.github.mohuddle.skerry.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun overlayPermissionIntent(packageName: String): Intent {
    return Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
    )
}
