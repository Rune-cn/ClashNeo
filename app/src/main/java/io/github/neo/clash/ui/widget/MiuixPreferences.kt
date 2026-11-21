// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.neo.clash.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

/*
 * Single entry points for selection rows.
 *
 * Every selection row goes through this file so the OS3/OS4 decision is made once, from the ambient
 * scope. OS4 genuinely uses a different selector — a glass dropdown — and that difference is the
 * point: it belongs to the glass presentation family, just like the glass top bar and navigation
 * bar. Callers never pass a style flag.
 *
 * The glass dropdown can only draw plain text labels. Options carrying an icon, a summary or
 * children fall back to the classic spinner instead of silently losing that content.
 */

private fun List<DropdownItem>.arePlainOptions(): Boolean = all {
    it.enabled && it.icon == null && it.summary == null && it.children.isNullOrEmpty()
}

/**
 * Selection row over plain labels, e.g. a language or theme-mode picker.
 */
@Composable
fun MiuixDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
) {
    val scope = LocalMiuixComponentScope.current
    val useOs4 = scope.useOs4 || AppTheme.useMiuixOs4

    if (useOs4) {
        GlassDropdownPreference(
            onSelectedIndexChange = onSelectedIndexChange,
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            backdrop = scope.pageBackdrop,
            visuals = GlassPopupDefaults.visuals(style = GlassDefaults.style(AppTheme.isDark)),
            modifier = modifier,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            renderInRootScaffold = scope.popupInRootScaffold,
            summary = summary,
        )
    } else {
        WindowDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}

/**
 * Selection row over rich [DropdownItem]s, e.g. the palette or predictive-back picker.
 */
@Composable
fun MiuixSpinnerPreference(
    items: List<DropdownItem>,
    selectedIndex: Int,
    title: String,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
) {
    val scope = LocalMiuixComponentScope.current
    val useOs4 = scope.useOs4 || AppTheme.useMiuixOs4

    if (useOs4 && items.arePlainOptions()) {
        GlassDropdownPreference(
            onSelectedIndexChange = { index ->
                onSelectedIndexChange(index)
                items[index].onClick?.invoke()
            },
            items = items.map { it.text },
            selectedIndex = selectedIndex,
            title = title,
            backdrop = scope.pageBackdrop,
            visuals = GlassPopupDefaults.visuals(style = GlassDefaults.style(AppTheme.isDark)),
            modifier = modifier,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            renderInRootScaffold = scope.popupInRootScaffold,
            summary = summary,
        )
    } else {
        WindowSpinnerPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}