package com.shamlou.fallingwords.ui.fallingWords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamlou.fallingwords.data.models.ResponseWord
import com.shamlou.fallingwords.repo.WordsRepository
import com.shamlou.fallingwords.utility.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FallingWordsViewModel(
    private val repo: WordsRepository
):ViewModel() {


    private val _allWords = MutableStateFlow<Resource<List<ResponseWord>?>>(Resource.success(null))
    val allWords: StateFlow<Resource<List<ResponseWord>?>>
        get() = _allWords

    init {

        getAllWords()
    }

    private fun getAllWords() = viewModelScope.launch {

        repo.getWords().collect {
            _allWords.value = it
        }
    }
}