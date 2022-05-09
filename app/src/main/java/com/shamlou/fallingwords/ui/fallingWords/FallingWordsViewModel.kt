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


    // keeps all words and the state of loading it
    private val _allWords = MutableStateFlow<Resource<List<ResponseWord>?>>(Resource.success(null))
    val allWords: StateFlow<Resource<List<ResponseWord>?>>
        get() = _allWords

    // i keep count down time here so it can survive configure changes
    private val _timeLeft = MutableStateFlow(START_TIME_IN_MILLIS)
    val timeLeft: StateFlow<Long>
        get() = _timeLeft

    // event of starting timer, Long value is the duration of timer
    private val _startTimerEvent = MutableSharedFlow<Event<Long>?>(1)
    val startTimerEvent: SharedFlow<Event<Long>?>
        get() = _startTimerEvent

    // event of starting animation every time from top to bottom,
    // [ResponseWord] is the corresponding item and game speed is used to set speed of animation
    private val _startAnimEvent = MutableSharedFlow<Event<Pair<ResponseWord, GameSpeed>>?>(1)
    val startAnimEvent: SharedFlow<Event<Pair<ResponseWord, GameSpeed>>?>
        get() = _startAnimEvent

    // view state is the single source of truth of the UI of this fragment
    // default value is set up game because we want to start with setting game configs
    // take a look at [ViewState] it is a sealed class with 3 state of screen
    private val _viewState = MutableStateFlow<ViewState>(ViewState.SetUpGame())
    val viewState: StateFlow<ViewState>
        get() = _viewState

    // button should be enabled when all words are loaded successfully,
    // and the game speed and game time should be selected
    val startGameButtonEnable = viewState.combine(allWords) { viewState, allWords ->

        allWords.isSuccess() && viewState is ViewState.SetUpGame && viewState.gameSpeed != GameSpeed.NOT_SELECTED && viewState.gameTime != GameTime.NOT_SELECTED
    }

    init {

        // get all words at the begining
        getAllWords()
    }

    //set new time left every second
    fun setTimeLeft(timeLeft: Long) {

        _timeLeft.tryEmit(timeLeft)
    }

    //when game is over
    fun timerFinished() {


        (viewState.value as? ViewState.Gaming)?.let { lastValue ->

            //change the state to result set parameters so it could be showed
            _viewState.tryEmit(
                ViewState.Result(
                    lastValue.currentIndex + 1,
                    lastValue.currectAnswers,
                    lastValue.wrongAnswers
                )
            )
        }
    }

    // called every time animation finished
    // updates state and sends an event for new word
    fun animationFinished() {

        (viewState.value as? ViewState.Gaming)?.let { lastValue ->

            // update the index
            _viewState.tryEmit(lastValue.copy(currentIndex = lastValue.currentIndex + 1))
            // event for new item animation
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

    //changes the state to gaming
    //creates a shuffled version of words
    //starts timer and animations
    fun startGameClicked() {

        // all correct items
        val correctWords = allWords.value.data ?: return
        val shuffledWords = correctWords.map {
            //half of times uses correct item
            //half of times places wrong spanish text (wrong spanish word is choosen randomly)
            if ((0..1).random() == 0) it.copy(isCorrect = true) else it.copy(
                text_spa = correctWords[(correctWords.indices).random()].text_spa,
                isCorrect = false
            )
        }
        (viewState.value as? ViewState.SetUpGame)?.let {


            //keeping speed and words for further uses
            _viewState.tryEmit(ViewState.Gaming(shuffledWords, it.gameSpeed))
            //start the timer (game is started)
            _startTimerEvent.tryEmit(Event(timeLeft.value))
            //start showing items (started from the first item)
            _startAnimEvent.tryEmit(Event(Pair(shuffledWords[0], it.gameSpeed)))
        }
    }

    // only called when user is on set up state,
    // updates the state with new game time
    fun timeSelected(gameTime: GameTime) {

        (_viewState.value as? ViewState.SetUpGame)?.let { lastValue ->

            _timeLeft.tryEmit(gameTime.time)
            _viewState.tryEmit(lastValue.copy(gameTime = gameTime))
        }
    }
    // only called when user is on set up state,
    // updates the state with new game speed
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

    // called when user click correct button
    // checks whether user is correct or not
    // and calls [addAnswer] to update the state
    fun correctClicked() {

        (_viewState.value as? ViewState.Gaming)?.let { lastValue ->

            val answerIsCorrect = lastValue.shuffledWords[lastValue.currentIndex].isCorrect
            addAnswer(answerIsCorrect)
        }
    }
    // called when user click wrong button
    // checks whether user is correct or not
    // and calls [addAnswer] to update the state
    fun wrongClicked() {

        (_viewState.value as? ViewState.Gaming)?.let { lastValue ->

            val answerIsCorrect = lastValue.shuffledWords[lastValue.currentIndex].isCorrect.not()
            addAnswer(answerIsCorrect)
        }
    }

    // adds answer to state and starts new animation
    // current index will be increased by 1 cuz user is done with this question
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

    //called when user clicks reset game button and we simply change the state
    fun resetGameClicked() {

        _viewState.tryEmit(ViewState.SetUpGame())
    }

    // staring animation again when user was on gaming
    // state and fragment resume called [aka configure changes]
    fun fragmentResume() {

        // we want to start anim again only when user is on gaming state
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