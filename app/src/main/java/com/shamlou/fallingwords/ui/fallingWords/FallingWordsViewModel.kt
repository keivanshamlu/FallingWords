package com.shamlou.fallingwords.ui.fallingWords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamlou.fallingwords.data.models.ResponseWord
import com.shamlou.fallingwords.repo.WordsRepository
import com.shamlou.fallingwords.utility.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
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

    private val _viewState = MutableStateFlow<ViewState>(ViewState.SetUpGame())
    val viewState: StateFlow<ViewState>
        get() = _viewState

    val startGameButtonEnable = viewState.combine(allWords){ viewState , allWords ->

        allWords.isSuccess() && viewState is ViewState.SetUpGame && viewState.gameSpeed != GameSpeed.NOT_SELECTED && viewState.gameTime != GameTime.NOT_SELECTED
    }

    init {

        getAllWords()
    }

    //set new time left every second
    fun setTimeLeft(timeLeft: Long) {

        _timeLeft.tryEmit(timeLeft)
    }

    //when game is over
    fun timerFinished() {


    }


    fun timeSelected(gameTime: GameTime) {


        (_viewState.value as? ViewState.SetUpGame)?.let { lastValue ->

            _timeLeft.tryEmit(gameTime.time)
            _viewState.tryEmit(lastValue.copy(gameTime = gameTime))
        }
    }
    fun speedSelected(gameSpeed: GameSpeed) {


        (_viewState.value as? ViewState.SetUpGame)?.let { lastValue ->

            _viewState.tryEmit(lastValue.copy(gameSpeed = gameSpeed))
        }
    }

    //called at the beginning to get all words
    private fun getAllWords() = viewModelScope.launch {

        repo.getWords().collect {
            _allWords.value = it
        }
    }
}

sealed class ViewState() {

    data class SetUpGame(
        val gameTime: GameTime = GameTime.NOT_SELECTED,
        val gameSpeed: GameSpeed = GameSpeed.NOT_SELECTED
    ) : ViewState()

    object Gaming : ViewState()
    object Result : ViewState()
}

enum class GameTime(val time: Long, val title: String) {
    TWENTY(20_000, "20 sec"),
    FOUTTY(40_000, "40 sec"),
    ONE_MINUTE(60_000, "60 sec"),
    NOT_SELECTED(0, "not seleceted")
}

enum class GameSpeed(val title: String) {
    SLOW("slow"),
    MEDIUM("medium"),
    FAST("fast"),
    NOT_SELECTED("not seleceted")
}