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

const val START_TIME_IN_MILLIS: Long = 50_000

class FallingWordsViewModel(
    private val repo: WordsRepository
) : ViewModel() {


    private val _allWords = MutableStateFlow<Resource<List<ResponseWord>?>>(Resource.success(null))
    val allWords: StateFlow<Resource<List<ResponseWord>?>>
        get() = _allWords

    private val _timeLeft = MutableStateFlow(START_TIME_IN_MILLIS)
    val timeLeft: StateFlow<Long>
        get() = _timeLeft


    init {

        getAllWords()
    }

    //set new time left every second
    fun setTimeLeft(timeLeft: Long){

        _timeLeft.tryEmit(timeLeft)
    }

    //when game is over
    fun timerFinished(){


    }

    //called at the beginning to get all words
    private fun getAllWords() = viewModelScope.launch {

        repo.getWords().collect {
            _allWords.value = it
        }
    }
}