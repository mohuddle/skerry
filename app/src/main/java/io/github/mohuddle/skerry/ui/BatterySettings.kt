package io.github.mohuddle.skerry.ui

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

fun batteryUnrestricted(context: Context): Boolean {
    val power = context.getSystemService(PowerManager::class.java)
    return power.isIgnoringBatteryOptimizations(context.packageName)
}

fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
