package io.github.neo.clash

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.neo.clash.common.util.intent
import io.github.neo.clash.ui.ClashNeoComposeActivity
import io.github.neo.clash.design.R as DesignR

/**
 * Launcher entry point.
 *
 * Historically this was `BaseActivity<MainDesign>` and hosted the XML main screen. The app is now
 * driven by the Miuix Compose UI, so this activity only performs the one-time platform setup that
 * must happen before any UI is shown (the notification permission request and the profile shortcut)
 * and then hands over to [ClashNeoComposeActivity].
 *
 * It is kept as a separate, thin activity rather than deleting it so that:
 * - the existing `MainActivityAlias` (which owns the LAUNCHER intent filter) keeps working, and
 * - `ExternalControlActivity` and the quick-settings tile, which target this component by name,
 *   do not need to change.
 */
class MainActivity : BaseActivity<io.github.neo.clash.design.MainDesign>() {

    private val requestNotificationPermission =
        registerForActivityResult(RequestPermission()) { }

    /**
     * `BaseActivity.main` is still required by the base class, but nothing is rendered here: the
     * XML design is immediately replaced by the Compose screen. The base class is kept because it
     * supplies the `clashRunning` plumbing the permission flow depends on.
     */
    override suspend fun main() {
        ensureNotificationPermission()
        setupShortcut()
        startActivity(ClashNeoComposeActivity::class.intent)
        finish()
    }

    private fun ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU)
            return

        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Pins a shortcut to the settings screen, as the previous implementation did.
     *
     * Failures are ignored on purpose: shortcut pinning is unsupported by some launchers and must
     * never block startup.
     */
    private fun setupShortcut() {
        runCatching {
            val shortcut = ShortcutInfoCompat.Builder(this, "settings")
                .setShortLabel(getString(DesignR.string.settings))
                .setLongLabel(getString(DesignR.string.settings))
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(SettingsActivity::class.intent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
        }
    }
}
