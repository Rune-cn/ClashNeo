package io.github.neo.clash.common.constants

import android.content.ComponentName
import io.github.neo.clash.common.util.packageName

object Components {
    private const val componentsPackageName = "io.github.neo.clash"

    val MAIN_ACTIVITY = ComponentName(packageName, "$componentsPackageName.MainActivity")
    val PROPERTIES_ACTIVITY = ComponentName(packageName, "$componentsPackageName.PropertiesActivity")
}