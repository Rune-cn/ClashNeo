package io.github.neo.clash.design.util

import io.github.neo.clash.design.view.ObservableScrollView

val ObservableScrollView.isTop: Boolean
    get() = scrollX == 0 && scrollY == 0
