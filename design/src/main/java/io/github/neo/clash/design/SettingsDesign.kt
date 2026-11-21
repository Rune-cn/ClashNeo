package io.github.neo.clash.design

import android.content.Context
import android.view.View
import io.github.neo.clash.design.databinding.DesignSettingsBinding
import io.github.neo.clash.design.util.applyFrom
import io.github.neo.clash.design.util.bindAppBarElevation
import io.github.neo.clash.design.util.layoutInflater
import io.github.neo.clash.design.util.root

class SettingsDesign(context: Context) : Design<SettingsDesign.Request>(context) {
    enum class Request {
        StartApp, StartNetwork, StartOverride, StartMetaFeature,
    }

    private val binding = DesignSettingsBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)
    }

    fun request(request: Request) {
        requests.trySend(request)
    }
}