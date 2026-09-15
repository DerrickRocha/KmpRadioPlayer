package com.radio.agilesouthwest.kmpradioplayer.di

import com.radio.agilesouthwest.kmpradioplayer.media.IosRadioPlayer
import com.radio.agilesouthwest.kmpradioplayer.media.RadioPlayer
import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformModule(): Module = module {
    single<RadioPlayer> { IosRadioPlayer() }
}
