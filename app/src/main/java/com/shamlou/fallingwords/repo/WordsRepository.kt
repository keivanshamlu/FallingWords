package com.shamlou.fallingwords.repo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shamlou.fallingwords.data.ReadFileFromAssets
import com.shamlou.fallingwords.data.models.ResponseWord
import com.shamlou.fallingwords.utility.DefaultDispatcherProvider
import com.shamlou.fallingwords.utility.DispatcherProvider
import com.shamlou.fallingwords.utility.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class WordsRepository(
    private val fileReader: ReadFileFromAssets,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {

    // calls fileReader to get all words,
    // also handles loading statae, success and error
    fun getWords(): Flow<Resource<List<ResponseWord>>> = flow {

        emit(Resource.loading())
        fileReader.readFile("words.json").also {
            emit(
                Resource.success(getList(it) ?: listOf())
            )
        }

    }.flowOn(dispatchers.io())
        .catch {

            emit(Resource.error(it, null))
        }

    /**
     * gets a json string and returns list of kotlin objects
     */
    private fun getList(jsonArray: String?): List<ResponseWord>? {
        TypeToken.getParameterized(MutableList::class.java, ResponseWord::class.java).type.also {
            return gson.fromJson(jsonArray, it)
        }
    }
}