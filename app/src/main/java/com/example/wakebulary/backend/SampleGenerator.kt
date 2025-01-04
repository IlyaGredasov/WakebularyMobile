package com.example.wakebulary.backend

import android.content.Context
import java.util.Locale
import kotlin.math.ln
import kotlin.math.round
import kotlin.random.Random

class SampleGenerator(context: Context, mode: WordType) {
    private val dataBaseClient = DataBaseClient(context)
    private val globalList: MutableList<String> = dataBaseClient.loadList(mode.typeName)
    private val sessionStats: SessionStatistics =
        SessionStatistics(0, 0, System.currentTimeMillis() / 1000.0)

    private fun expoDistribution(): Int {
        return round(-ln(Random.nextDouble()) / Settings.alpha * globalList.size).toInt()
    }

    fun startLearningLoop(sampleSize: Int = Settings.sampleSize) {
        val sample: MutableList<WordTranslation> = mutableListOf()
        while (globalList.isNotEmpty()) {
            while (globalList.isNotEmpty() && sample.size < sampleSize) {
                var index = expoDistribution()
                while (index < 0 || index >= globalList.size) {
                    index = expoDistribution()
                }
                val word = globalList.removeAt(index)
                val translations = dataBaseClient.translateWord(word).toMutableList()
                sample.add(WordTranslation(word, translations))
            }
            while (sample.isNotEmpty()) {
                val questionWord = sample.random()
                println("${questionWord.word} ?")
                println("Remain: ${globalList.size}, remain in sample: ${sample.size}, Your answer:")
                val answer = readlnOrNull()?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.ROOT
                    ) else it.toString()
                } ?: ""
                if (answer == "!end") {
                    throw InterruptedException()
                }
                val (correct, attempts) = dataBaseClient.getStatistics(questionWord.word)
                if (answer in questionWord.translation) {
                    println("Yes! $answer")
                    questionWord.translation.remove(answer)
                    if (questionWord.translation.isEmpty()) {
                        sample.remove(questionWord)
                    }
                    sessionStats.correct++
                    sessionStats.attempts++
                    dataBaseClient.setStatistics(questionWord.word, correct + 1, attempts + 1)
                } else {
                    println("No: ${questionWord.translation}")
                    sessionStats.attempts++
                    dataBaseClient.setStatistics(questionWord.word, correct, attempts + 1)
                }
            }
        }
    }
}


