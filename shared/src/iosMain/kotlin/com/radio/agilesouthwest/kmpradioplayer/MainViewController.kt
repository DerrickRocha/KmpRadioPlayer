package com.radio.agilesouthwest.kmpradioplayer

import androidx.compose.ui.window.ComposeUIViewController
import com.radio.agilesouthwest.kmpradioplayer.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
