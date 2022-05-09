package com.shamlou.fallingwords.data


import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.shamlou.fallingwords.fakers.fakers.sampleErrorText
import com.shamlou.fallingwords.fakers.fakers.validWordsJson
import com.shamlou.fallingwords.fakers.fakers.validWordsList
import com.shamlou.fallingwords.utility.CoroutineTestRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class ReadFileFromAssetsTest{

    @get:Rule
    var mainCoroutineRule = CoroutineTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    lateinit var context : Context
    private lateinit var reader : ReadFileFromAssets

    @Before
    fun setUp() {

        MockKAnnotations.init(this)
        reader = ReadFileFromAssets(context)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun readFile_shouldCallAssetOpen() = mainCoroutineRule.testDispatcher.runBlockingTest {

        //given
        every { context.assets.open(any()) } returns ByteArrayInputStream(validWordsJson.toByteArray())
        //when
        val fileName = "test.txt"
        reader.readFile(fileName)
        //then
        verify { context.assets.open(eq(fileName)) }
    }
    @Test
    @ExperimentalCoroutinesApi
    fun readFile_shouldReturnValidResult() = mainCoroutineRule.testDispatcher.runBlockingTest {

        //given
        every { context.assets.open(any()) } returns ByteArrayInputStream(validWordsJson.toByteArray())
        //when
        val fileName = "test.txt"
        val result = reader.readFile(fileName)
        //then
        Assert.assertEquals(result , validWordsJson)
    }
    @Test
    @ExperimentalCoroutinesApi
    fun readFile_shouldThrowValidException() = mainCoroutineRule.testDispatcher.runBlockingTest {

        //given
        every { context.assets.open(any()) } throws IOException(sampleErrorText)

        // i had to do this instead of assertThrow
        // since it does not support suspend call
        try {
            //when
            val fileName = "test.txt"
            reader.readFile(fileName)
        }catch (e : Exception){
            //then
            Assert.assertEquals(e.message, IOException(sampleErrorText).message)
        }
    }
}