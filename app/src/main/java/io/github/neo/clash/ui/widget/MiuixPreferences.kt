// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

/*
 * Single entry points for every selection row.
 *
 * Routing them through one file means the choice of widget lives in one place instead of being
 * repeated per screen.
 *
 * These deliberately use the *classic* spinner and dropdown, not the glass variants. The glass
 * popup re-styled the selector itself the moment OS4 was enabled, so choosing OS4 changed the very
 * control the user was touching — a settings row should look identical regardless of the
 * presentation family. Only the navigation chrome and the top bar change between OS3 and OS4.
 */

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