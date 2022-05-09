package com.shamlou.fallingwords.ui.fallingWords

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.shamlou.fallingwords.data.models.ResponseWord
import com.shamlou.fallingwords.fakers.fakers.sampleError
import com.shamlou.fallingwords.fakers.fakers.validWordsList
import com.shamlou.fallingwords.repo.WordsRepository
import com.shamlou.fallingwords.utility.CoroutineTestRule
import com.shamlou.fallingwords.utility.Resource
import com.shamlou.fallingwords.utility.getLastEmitted
import com.shamlou.fallingwords.utility.getListEmitted
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@ExperimentalCoroutinesApi
class FallingWordsViewModelTest {

    @get:Rule
    var mainCoroutineRule = CoroutineTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    lateinit var repo: WordsRepository

    lateinit var viewModel: FallingWordsViewModel

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        MockKAnnotations.init(this)

    }

    @Test
    fun shouldGetWords_WhenStarted()= mainCoroutineRule.testDispatcher.runBlockingTest {


        viewModel = FallingWordsViewModel(repo)

        coVerify {
            repo.getWords()
        }
    }
    @Test
    fun shouldGetUpdateState_whenGetAllWordsReturnsSuccessfull()= mainCoroutineRule.testDispatcher.runBlockingTest {



        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        viewModel = FallingWordsViewModel(repo)

        coVerify {
            repo.getWords()
        }

        val emittedList = getListEmitted(viewModel.allWords)
        Assert.assertEquals(emittedList.first().status, Resource.Status.SUCCESS)
        Assert.assertEquals(emittedList.first().data, validWordsList)
    }
    @Test
    fun shouldGetUpdateState_whenGetAllWordsReturnsFailure()= mainCoroutineRule.testDispatcher.runBlockingTest {



        coEvery {
            repo.getWords()
        }returns flow { emit(Resource.error<List<ResponseWord>>(sampleError)) }

        viewModel = FallingWordsViewModel(repo)

        coVerify {
            repo.getWords()
        }

        val emittedList = getListEmitted(viewModel.allWords)
        Assert.assertEquals(emittedList.first().status, Resource.Status.ERROR)
        Assert.assertEquals(emittedList.first().error, sampleError)
    }

    @Test
    fun setTimeLeft_shouldUpdateStateOfTime() = mainCoroutineRule.testDispatcher.runBlockingTest {

        viewModel = FallingWordsViewModel(repo)
        viewModel.setTimeLeft(1_123_123)
        val emittedList = getListEmitted(viewModel.timeLeft)
        Assert.assertEquals(emittedList.first(), 1_123_123)
    }

    @Test
    fun timerFinished_shouldTakeResult_andUpdateTheState() = mainCoroutineRule.testDispatcher.runBlockingTest {

        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        stateGaming()

        (viewModel.viewState.value as ViewState.Gaming).let {
            val currectAnswers = it.currectAnswers
            val wrongAnswers = it.wrongAnswers
            val index = it.currentIndex

            viewModel.timerFinished()

            val emittedList = getLastEmitted(viewModel.viewState)
            Assert.assertEquals(emittedList is ViewState.Result, true)
            Assert.assertEquals((emittedList as ViewState.Result).currectAnswers, currectAnswers)
            Assert.assertEquals(emittedList.wrongAnswers, wrongAnswers)
            Assert.assertEquals(emittedList.allQuestions, index+1)
        }

    }

    @Test
    fun animationFinished_shouldIncreaseIndexAndSendNextWordAsEvent() = mainCoroutineRule.testDispatcher.runBlockingTest {

        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        stateGaming()

        (viewModel.viewState.value as ViewState.Gaming).let {

            val words = it.shuffledWords
            val index = it.currentIndex

            viewModel.animationFinished()

            val emittedViewState = getLastEmitted(viewModel.viewState)
            Assert.assertEquals(emittedViewState is ViewState.Gaming, true)
            Assert.assertEquals((emittedViewState as ViewState.Gaming).currentIndex, index+1)

            val emitedEvent = getLastEmitted(viewModel.startAnimEvent)?.getContentIfNotHandled()
            Assert.assertEquals(emitedEvent?.first, words[index+1])
        }

    }

    @Test
    fun startGameClicked_shouldUpdateStateAndStartTimerAndStartAnim() = mainCoroutineRule.testDispatcher.runBlockingTest {

        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        stateSetUp()
        (viewModel.viewState.value as ViewState.SetUpGame).let {


            viewModel.startGameClicked()
            val emittedViewState = getLastEmitted(viewModel.viewState)
            Assert.assertEquals(emittedViewState is ViewState.Gaming, true)

            val emittedTimerEvent = getLastEmitted(viewModel.startTimerEvent)?.getContentIfNotHandled()
            Assert.assertEquals(emittedTimerEvent, GameTime.FOUTTY.time)
        }
    }

    @Test
    fun timeSelected_shouldUpdateTheState() = mainCoroutineRule.testDispatcher.runBlockingTest {

        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        viewModel = FallingWordsViewModel(repo)
        viewModel.timeSelected(GameTime.FOUTTY)

        val emittedViewState = getLastEmitted(viewModel.viewState)
        Assert.assertEquals((emittedViewState as ViewState.SetUpGame).gameTime, GameTime.FOUTTY)
    }

    @Test
    fun speedSelected_shouldUpdateTheState() = mainCoroutineRule.testDispatcher.runBlockingTest {

        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        viewModel = FallingWordsViewModel(repo)
        viewModel.speedSelected(GameSpeed.SLOW)

        val emittedViewState = getLastEmitted(viewModel.viewState)
        Assert.assertEquals((emittedViewState as ViewState.SetUpGame).gameSpeed, GameSpeed.SLOW)
    }

    @Test
    fun correctClicked_shouldIncreaseIndexAndAddAnswer() = mainCoroutineRule.testDispatcher.runBlockingTest {

        coEvery {
            repo.getWords()
        } returns flow { emit(Resource.success(validWordsList)) }

        stateGaming()

        (viewModel.viewState.value as ViewState.Gaming).let {

            val words = it.shuffledWords
            val index = it.currentIndex
            val currectAnswers = it.currectAnswers
            val wrongAnswers = it.wrongAnswers
            val isWrong = it.shuffledWords[index].isCorrect.not()

            viewModel.correctClicked()

            val emittedViewState = getLastEmitted(viewModel.viewState)
            Assert.assertEquals(emittedViewState is ViewState.Gaming, true)
            Assert.assertEquals((emittedViewState as ViewState.Gaming).currentIndex, index+1)
            if(isWrong){

                Assert.assertEquals((emittedViewState as ViewState.Gaming).wrongAnswers, wrongAnswers+1)
            }else{
                Assert.assertEquals((emittedViewState as ViewState.Gaming).currectAnswers, currectAnswers+1)
            }

            val emitedEvent = getLastEmitted(viewModel.startAnimEvent)?.getContentIfNotHandled()
            Assert.assertEquals(emitedEvent?.first, words[index+1])

        }

    }

    private fun stateGaming(){
        stateSetUp()
        viewModel.startGameClicked()

        viewModel.correctClicked()
        viewModel.correctClicked()
    }

    private fun stateSetUp(){
        viewModel = FallingWordsViewModel(repo)
        viewModel.speedSelected(GameSpeed.SLOW)
        viewModel.timeSelected(GameTime.FOUTTY)
    }

}