package com.shamlou.fallingwords.fakers

import com.shamlou.fallingwords.data.models.ResponseWord
import java.lang.Exception

object fakers {

    val fileName = "words.json"
    val validWordsList = listOf(
        ResponseWord("primary school","escuela primaria"),
        ResponseWord("teacher","profesor / profesora"),
        ResponseWord("pupil","alumno / alumna"),
        ResponseWord("holidays","vacaciones")
    )

    val validWordsJson = """
        [
          {
            "text_eng": "primary school",
            "text_spa": "escuela primaria"
          },
          {
            "text_eng": "teacher",
            "text_spa": "profesor / profesora"
          },
          {
            "text_eng": "pupil",
            "text_spa": "alumno / alumna"
          },
          {
            "text_eng": "holidays",
            "text_spa": "vacaciones "
          }
        ]

    """.trimIndent()

    val sampleErrorText = "error"
    val sampleError = Exception(sampleErrorText)
}