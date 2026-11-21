package io.github.neo.clash.design

import android.content.Context
import android.net.Uri
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

class HelpDesign(
    context: Context,
    openLink: (Uri) -> Unit,
) : Design<Unit>(context) {
    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.surface = surface

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val screen = preferenceScreen(context) {
            tips(R.string.tips_help)

            category(R.string.document)

            clickable(
                title = R.string.clash_wiki,
                summary = R.string.clash_wiki_url
            ) {
                clicked {
                    openLink(Uri.parse(context.getString(R.string.clash_wiki_url)))
                }
            }

            clickable(
                title = R.string.mihomo_wiki,
                summary = R.string.mihomo_wiki_url
            ) {
                clicked {
                    openLink(Uri.parse(context.getString(R.string.mihomo_wiki_url)))
                }
            }

            category(R.string.sources)

            clickable(
                title = R.string.mihomo_core,
                summary = R.string.mihomo_core_url
            ) {
                clicked {
                    openLink(Uri.parse(context.getString(R.string.mihomo_core_url)))
                }
            }

            // ClashNeo itself (this fork)
            clickable(
                title = R.string.clash_neo_for_android,
                summary = R.string.meta_github_url
            ) {
                clicked {
                    openLink(Uri.parse(context.getString(R.string.meta_github_url)))
                }
            }

            // Upstream project this fork is based on (GPL-3.0 attribution)
            clickable(
                title = R.string.clash_meta_for_android,
                summary = R.string.upstream_github_url
            ) {
                clicked {
                    openLink(Uri.parse(context.getString(R.string.upstream_github_url)))
                }
            }
        }

        binding.content.addView(screen.root)
    }
}