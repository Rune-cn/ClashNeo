package io.github.neo.clash.design

import android.content.Context
import android.view.View
import io.github.neo.clash.design.databinding.DesignSettingsCommonBinding
import io.github.neo.clash.design.preference.category
import io.github.neo.clash.design.preference.clickable
import io.github.neo.clash.design.preference.preferenceScreen
import io.github.neo.clash.design.preference.tips
import io.github.neo.clash.design.util.applyFrom
import io.github.neo.clash.design.util.bindAppBarElevation
import io.github.neo.clash.design.util.layoutInflater
import io.github.neo.clash.design.util.root

class ApkBrokenDesign(context: Context) : Design<ApkBrokenDesign.Request>(context) {
    data class Request(val url: String)

    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.surface = surface

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val screen = preferenceScreen(context) {
            tips(R.string.application_broken_tips)

            category(R.string.reinstall)

            clickable(
                title = R.string.github_releases,
                summary = R.string.meta_github_url
            ) {
                clicked {
                    requests.trySend(Request(context.getString(R.string.meta_github_url)))
                }
            }
        }

        binding.content.addView(screen.root)
    }
}