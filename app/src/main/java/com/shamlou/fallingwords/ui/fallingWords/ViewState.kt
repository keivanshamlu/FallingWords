package com.shamlou.fallingwords.ui.fallingWords

import com.shamlou.fallingwords.data.models.ResponseWord

//all the state of our game
sealed class ViewState {

    // when user is entering the config of game:
    // speed and time
    // they should be not selected at the beginning
    data class SetUpGame(
        val gameTime: GameTime = GameTime.NOT_SELECTED,
        val gameSpeed: GameSpeed = GameSpeed.NOT_SELECTED
    ) : ViewState()

    //when user is playing game
    //shuffled words: new items every time user start game, contains correct and wrong items)
    //speed: selected speed by the user, used when we are staring animation
    //currentIndex: index of current item that is currently on the screen
    data class Gaming(
        val shuffledWords: List<ResponseWord>,
        val speed: GameSpeed,
        val currentIndex: Int = 0,
        val currectAnswers: Int = 0,
        val wrongAnswers: Int = 0
    ) : ViewState()

    //info about the finished game
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