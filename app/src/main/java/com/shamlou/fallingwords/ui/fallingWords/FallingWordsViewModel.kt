package com.shamlou.fallingwords.ui.fallingWords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamlou.fallingwords.data.models.ResponseWord
import com.shamlou.fallingwords.repo.WordsRepository
import com.shamlou.fallingwords.utility.Event
import com.shamlou.fallingwords.utility.Resource
import kotlinx.coroutines.flow.*
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

    private val _startTimerEvent = MutableSharedFlow<Event<Long>?>(1)
    val startTimerEvent: SharedFlow<Event<Long>?>
        get() = _startTimerEvent

    private val _startAnimEvent = MutableSharedFlow<Event<Pair<ResponseWord, GameSpeed>>?>(1)
    val startAnimEvent: SharedFlow<Event<Pair<ResponseWord, GameSpeed>>?>
        get() = _startAnimEvent

    private val _viewState = MutableStateFlow<ViewState>(ViewState.SetUpGame())
    val viewState: StateFlow<ViewState>
        get() = _viewState

    val startGameButtonEnable = viewState.combine(allWords) { viewState, allWords ->

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


        (viewState.value as? ViewState.Gaming)?.let { lastValue ->

            _viewState.tryEmit(
                ViewState.Result(
                    lastValue.currentIndex + 1,
                    lastValue.currectAnswers,
                    lastValue.wrongAnswers
                )
            )
        }
    }

    fun animationFinished() {

        (viewState.value as? ViewState.Gaming)?.let { lastValue ->

            _viewState.tryEmit(lastValue.copy(currentIndex = lastValue.currentIndex + 1))
            _startAnimEvent.tryEmit(
                Event(
                    Pair(
                        lastValue.shuffledWords[lastValue.currentIndex + 1],
                        lastValue.speed
                    )
                )
            )
        }
    }

    fun startGameClicked() {

        val correctWords = allWords.value.data ?: return
        val shuffledWords = correctWords.map {
            if ((0..1).random() == 0) it.copy(isCorrect = true) else it.copy(
                text_spa = correctWords[(correctWords.indices).random()].text_spa,
                isCorrect = false
            )
        }
        (viewState.value as? ViewState.SetUpGame)?.let {


            _viewState.tryEmit(ViewState.Gaming(shuffledWords, it.gameSpeed))
            _startTimerEvent.tryEmit(Event(timeLeft.value))
            _startAnimEvent.tryEmit(Event(Pair(shuffledWords[0], it.gameSpeed)))
        }
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

    fun currectClicked() {

        (_viewState.value as? ViewState.Gaming)?.let { lastValue ->

            val answerIsCorrect = lastValue.shuffledWords[lastValue.currentIndex].isCorrect
            addAnswer(answerIsCorrect)
        }
    }

    fun wrongClicked() {

        (_viewState.value as? ViewState.Gaming)?.let { lastValue ->

            val answerIsCorrect = lastValue.shuffledWords[lastValue.currentIndex].isCorrect.not()
            addAnswer(answerIsCorrect)
        }
    }

    private fun addAnswer(answerIsCorrect: Boolean) {

        (_viewState.value as? ViewState.Gaming)?.let { lastValue ->

            _viewState.tryEmit(
                lastValue.copy(
                    currentIndex = lastValue.currentIndex + 1,
                    currectAnswers = if (answerIsCorrect) lastValue.currectAnswers + 1 else lastValue.currectAnswers,
                    wrongAnswers = if (answerIsCorrect) lastValue.wrongAnswers else lastValue.wrongAnswers + 1,
                )
            )
            _startAnimEvent.tryEmit(
                Event(
                    Pair(
                        lastValue.shuffledWords[lastValue.currentIndex + 1],
                        lastValue.speed
                    )
                )
            )
        }
    }

    fun resetGameClicked() {

        _viewState.tryEmit(ViewState.SetUpGame())
    }

    fun fragmentResume() {

        (viewState.value as? ViewState.Gaming)?.let { lastValue ->

            _startAnimEvent.tryEmit(
                Event(
                    Pair(
                        lastValue.shuffledWords[lastValue.currentIndex],
                        lastValue.speed
                    )
                )
            )
        }
    }

}

sealed class ViewState {

    data class SetUpGame(
        val gameTime: GameTime = GameTime.NOT_SELECTED,
        val gameSpeed: GameSpeed = GameSpeed.NOT_SELECTED
    ) : ViewState()

    data class Gaming(
        val shuffledWords: List<ResponseWord>,
        val speed: GameSpeed,
        val currentIndex: Int = 0,
        val currectAnswers: Int = 0,
        val wrongAnswers: Int = 0
    ) : ViewState()

    data class Result(
        val allQuestions: Int,
        val currectAnswers: Int,
        val wrongAnswers: Int
    ) : ViewState()
}

enum class GameTime(val time: Long, val title: String) {
    TWENTY(20_000, "20 sec"),
    FOUTTY(40_000, "40 sec"),
    ONE_MINUTE(60_000, "60 sec"),
    NOT_SELECTED(0, "not seleceted")
}

enum class GameSpeed(val title: String, val duration: Long) {
    SLOW("slow", 7_000),
    MEDIUM("medium", 5_000),
    FAST("fast", 3_000),
    NOT_SELECTED("not seleceted", 0)
}