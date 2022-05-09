package com.shamlou.fallingwords.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shamlou.fallingwords.data.ReadFileFromAssets
import com.shamlou.fallingwords.data.models.ResponseWord
import com.shamlou.fallingwords.fakers.fakers
import com.shamlou.fallingwords.fakers.fakers.fileName
import com.shamlou.fallingwords.fakers.fakers.sampleError
import com.shamlou.fallingwords.utility.*
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WordsRepositoryTest(){

    @get:Rule
    var mainCoroutineRule = CoroutineTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    lateinit var repo: WordsRepository

    @MockK(relaxed = true)
    lateinit var fileReader: ReadFileFromAssets
    @MockK(relaxed = true)
    lateinit var gson: Gson

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repo = WordsRepository(
            fileReader,
            gson,
            mainCoroutineRule.testDispatcherProvider
        )
    }

    @Test
    fun getWords_shouldReturnLoadingAtFirst() = mainCoroutineRule.testDispatcher.runBlockingTest {
        coEvery {
            fileReader.readFile(
                "words.json"
            )
        } returns fakers.validWordsJson

        val emittedList = getListEmitted(repo.getWords())
        Assert.assertEquals(emittedList.first().status, Resource.Status.LOADING)
    }

    @Test
    fun getWords_shouldCallApiAndReturnSuccess_whenApiReturnsSuccessfully() = mainCoroutineRule.testDispatcher.runBlockingTest {
            coEvery {
                fileReader.readFile(
                    fileName
                )
            } returns fakers.validWordsJson

            val type = TypeToken.getParameterized(MutableList::class.java, ResponseWord::class.java).type
            every { gson.fromJson<List<ResponseWord>?>(fakers.validWordsJson, type) } returns fakers.validWordsList

            val emittedList = getListEmitted(repo.getWords())
            coVerify {
                fileReader.readFile(fileName)
            }
            Assert.assertEquals(emittedList.first().status, Resource.Status.LOADING)
            Assert.assertEquals(emittedList.last().status, Resource.Status.SUCCESS)
            Assert.assertEquals(emittedList.size, 2)
            Assert.assertEquals(emittedList.last().data, fakers.validWordsList)
        }


    @Test
    fun login_shouldCallApiAndReturnError_whenApiReturnsError() =
        mainCoroutineRule.testDispatcher.runBlockingTest {
            coEvery {
                fileReader.readFile(
                    fileName
                )
            } throws fakers.sampleError

            val emittedList = getListEmitted(repo.getWords())
            coVerify {
                fileReader.readFile(fileName)
            }
            Assert.assertEquals(emittedList.first().status, Resource.Status.LOADING)
            Assert.assertEquals(emittedList.last().status, Resource.Status.ERROR)
            Assert.assertEquals(emittedList.size, 2)
            Assert.assertEquals(emittedList.last().error, sampleError)
        }
}