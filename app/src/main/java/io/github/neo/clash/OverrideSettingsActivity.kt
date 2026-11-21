package io.github.neo.clash

import android.content.pm.PackageManager
import io.github.neo.clash.common.compat.getDrawableCompat
import io.github.neo.clash.common.constants.Metadata
import io.github.neo.clash.core.Clash
import io.github.neo.clash.design.OverrideSettingsDesign
import io.github.neo.clash.design.model.AppInfo
import io.github.neo.clash.design.util.toAppInfo
import io.github.neo.clash.service.store.ServiceStore
import io.github.neo.clash.util.withClash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class OverrideSettingsActivity : BaseActivity<OverrideSettingsDesign>() {
    override suspend fun main() {
        val configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }
        val service = ServiceStore(this)

        defer {
            withClash {
                patchOverride(Clash.OverrideSlot.Persist, configuration)
            }
        }

        val design = OverrideSettingsDesign(
            this,
            configuration
        )

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        OverrideSettingsDesign.Request.ResetOverride -> {
                            if (design.requestResetConfirm()) {
                                defer {
                                    withClash {
                                        clearOverride(Clash.OverrideSlot.Persist)
                                    }
                                }

                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}