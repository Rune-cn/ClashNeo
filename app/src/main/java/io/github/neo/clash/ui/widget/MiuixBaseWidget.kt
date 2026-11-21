// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Switch

/*
 * Boolean settings rows.
 *
 * These wrap `BasicComponent` rather than a hand-built row so the press feedback, the row height
 * and the trailing control inset all come from Miuix. The whole row is clickable in addition to the
 * control itself, which is what makes a settings list feel native: tapping anywhere on the line
 * toggles it.
 */

/**
 * A settings row backed by a boolean, with a switch as the trailing control.
 */
@Composable
fun MiuixSwitchWidget(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    BasicComponent(
        title = title,
        summary = description,
        enabled = enabled,
        modifier = modifier,
        // Guarding here (rather than relying on `enabled`) keeps a disabled row from calling back
        // into a caller that may not expect it.
        onClick = { if (enabled) onCheckedChange(!checked) },
        endActions = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

/**
 * A settings row backed by a boolean, with a checkbox as the trailing control.
 *
 * Used where the value is one of many (a list of selectable entries) rather than an immediate
 * on/off effect, so the checkbox shape communicates "part of a set".
 */
@Composable
fun MiuixCheckboxWidget(
    title: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    BasicComponent(
        title = title,
        summary = description,
        enabled = enabled,
        modifier = modifier,
        onClick = { if (enabled) onCheckedChange(!checked) },
        endActions = {
            Checkbox(
                state = ToggleableState(value = checked),
                onClick = { onCheckedChange(!checked) },
                colors = CheckboxDefaults.checkboxColors(),
                enabled = enabled,
            )
        },
    )
}
