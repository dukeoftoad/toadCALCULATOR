// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

/** Stable test tags for Compose UI tests. */
object CalcTags {
    const val EXPRESSION = "expression_display"
    const val RESULT = "result_preview"
    const val ERROR = "error_inline"

    fun key(key: KeypadKey) = "key_${key.text}"
}
