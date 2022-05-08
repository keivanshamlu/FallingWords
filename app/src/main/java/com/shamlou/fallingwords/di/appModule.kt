package com.shamlou.fallingwords.di

import com.google.gson.Gson
import com.shamlou.fallingwords.data.ReadFileFromAssets
import com.shamlou.fallingwords.repo.WordsRepository
import com.shamlou.fallingwords.ui.fallingWords.FallingWordsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { ReadFileFromAssets(androidContext()) }
    single { Gson() }
    single { WordsRepository(get(), get()) }
    viewModel { FallingWordsViewModel(get()) }
}