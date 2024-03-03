package com.example.pitchmasterbeta.services

import android.content.Context
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.InvokeRequest
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
import com.example.pitchmasterbeta.model.LyricsWord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.math.pow

class LyricsProvider(context: Context?) {
    private var lambdaClient: AWSLambda? = null
    private var clientConfiguration: ClientConfiguration? = null
    private var context: Context? = null

    init {
        if (context != null) {
            this.context = context
        } else throw IllegalArgumentException("Context must be NoNull!")
    }

    private fun initLambda() {
        if (clientConfiguration == null) {
            clientConfiguration = ClientConfiguration().apply {
                connectionTimeout = 1000 * 60 * 4
                socketTimeout = 1000 * 60 * 4
            }
        }
        if (sCredProvider == null) {
            sCredProvider = CognitoCachingCredentialsProvider(
                context, AWSKeys.COGNITO_POOL_ID,  // Identity Pool ID
                AWSKeys.MY_REGIONS,  // Region
                clientConfiguration
            )
        }
        if (lambdaClient == null) {
            lambdaClient = AWSLambdaClient(sCredProvider, clientConfiguration).apply {
                setRegion(AWSKeys.MY_REGION)
            }
        }
    }

    fun invokeLyricsLambdaFunction(
        songName: String, objectKey: String, audioDuration: Double
    ): List<LyricsTimestampedSegment> {
        initLambda()
        var result = listOf<LyricsTimestampedSegment>()
        val payload = "{\"song\": \"$songName\", \"url\":\"$objectKey\"}"
        val payloadBuffer = ByteBuffer.wrap(payload.toByteArray(StandardCharsets.UTF_8))
        val invokeRequest =
            InvokeRequest().withFunctionName(AWSKeys.LYRICS_LAMBDA_NAME).withPayload(payloadBuffer)
        try {
            lambdaClient?.run {
                val invokeResult = this.invoke(invokeRequest)
                val statusCode = invokeResult.statusCode
                if (statusCode == 200) {
                    result = extractData(String(invokeResult.payload.array()), audioDuration)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                "LyricsProvider",
                " - bad response - something went wrong :(\n ${e.message}\n" + "${e.localizedMessage}\n" + "${e.cause}"
            )
        }
        return result
    }

    private fun String.cleanString(): String {
        return this.replace(Regex("[,.!?']"), "").lowercase()
    }


    private fun handleDuplicates(lyricsWords: List<LyricsWord>): List<LyricsWord> {
        return (lyricsWords.groupBy { it.start }).map { segment ->
            val words = segment.value.groupBy { it.word }.map { it.value.first() }
            val segmentDuration = segment.value.last().end - segment.value.first().start
            val wordDuration = segmentDuration / words.size.toDouble()
            (words.indices).map { index ->
                val wordStart = wordDuration * index + segment.value.first().start
                val wordEnd = wordStart + wordDuration
                LyricsWord(words[index].word, start = wordStart, end = wordEnd)
            }
        }.flatten()
    }

    /**
     * temporary function, should be in server
     * **/
    private fun generateSegments(
        words: List<LyricsWord>, segments: List<LyricsSegment>
    ): List<LyricsTimestampedSegment> {
        var wordIndex = 0
        val wordsLen = words.size
        val timestampedSegments = segments.mapNotNull { segment ->
            val segmentWords = segment.text.trim().split("\\s+|-|\\*".toRegex())
            var timestampedWords = segmentWords.mapNotNull { word ->
                val cleanedWord = word.cleanString().trim()
                var j = wordIndex
                var matchingWord = words.getOrNull(wordIndex)?.word?.cleanString()?.trim()
                while (cleanedWord != matchingWord && j < wordsLen - 1) {
                    j++
                    matchingWord = words.getOrNull(j)?.word?.cleanString()?.trim()
                }
                val lyricsWord = words[wordIndex]
                if (wordIndex < wordsLen) {
                    wordIndex++
                }
                if (cleanedWord == matchingWord) {
                    LyricsWord(word = word, start = lyricsWord.start, end = lyricsWord.end)
                } else {
                    null
                }
            }
            timestampedWords = shiftSegment(timestampedWords.toMutableList(), segment)
            if (timestampedWords.isEmpty()) null else LyricsTimestampedSegment(timestampedWords)
        }.toMutableList()
        for (i in 1 until timestampedSegments.size) {
            val previousSegment = timestampedSegments[i - 1]
            val currentSegment = timestampedSegments[i]

            val previousStart = previousSegment.text.first().start
            val previousEnd = previousSegment.text.last().end
            val currentStart = currentSegment.text.first().start
            val currentEnd = currentSegment.text.last().end

            if (previousEnd >= currentStart) {
                if (previousEnd >= currentEnd) {
                    val currentDuration = (currentEnd - currentStart)
                    val previousDuration = (previousEnd - previousStart)
                    val durationFactor =
                        (previousDuration.pow(2)) / (previousDuration + currentDuration)

                    timestampedSegments[i - 1] = timestampedSegments[i - 1].copy(
                        text = redefineBoundaries(
                            timestampedSegments[i - 1].text.toMutableList(),
                            previousStart,
                            previousStart + durationFactor
                        )
                    )
                    timestampedSegments[i] = timestampedSegments[i].copy(
                        text = redefineBoundaries(
                            timestampedSegments[i].text.toMutableList(),
                            previousStart + durationFactor,
                            previousEnd
                        )
                    )
                } else {
                    timestampedSegments[i] = timestampedSegments[i].copy(
                        text = redefineBoundaries(
                            timestampedSegments[i].text.toMutableList(),
                            previousEnd,
                            currentEnd
                        )
                    )
                }
            }
        }
        return timestampedSegments
    }

    private fun shiftSegment(
        words: MutableList<LyricsWord>, segment: LyricsSegment
    ): List<LyricsWord> {
        if (words.first().end >= segment.end && words.last().start <= segment.start) {
            // Segment completely within words range, no need to shift
            return words
        }
        if (words.first().start < segment.start && words.first().end > segment.start) {
            words[0] = words.first().copy(start = segment.start)
        }
        if (words.last().end > segment.end && words.last().start < segment.end) {
            words[words.size - 1] = words.last().copy(end = segment.end)
        }
        if (words.first().start < segment.start && words.first().end < segment.start) {
            // shift forward:
            val start = segment.start
            // but wait, should the duration of the segment be shortened?
            val end =
                if (words.last().end < segment.end && start < words.last().end) words.last().end else segment.end
            redefineBoundaries(words, start, end)
        }
        return words
    }

    private fun redefineBoundaries(
        words: MutableList<LyricsWord>,
        start: Double,
        end: Double
    ): List<LyricsWord> {
        val duration = end - start
        val originalDuration = words.last().end - words.first().start

        // Calculate new durations for each word after shift
        val wordsDurationsAfterShift = words.map { word ->
            (word.end - word.start) * (duration / originalDuration)
        }

        // Calculate gaps between words after shift
        val wordsGapsAfterShift = words.zipWithNext { word1, word2 ->
            (word2.start - word1.end) * (duration / originalDuration)
        }.toMutableList()
        wordsGapsAfterShift.add(0, 0.0)


        words[0] = words[0].copy(start = start, end = start + wordsDurationsAfterShift[0])
        for (i in 1 until words.size) {
            val wordStart = words[i - 1].end + wordsGapsAfterShift[i]
            words[i] =
                words[i].copy(start = wordStart, end = wordStart + wordsDurationsAfterShift[i])
        }
        return words
    }

    fun extractData(payloadString: String, audioDuration: Double): List<LyricsTimestampedSegment> {
        val responseJson = JSONObject(payloadString)
        val gson = Gson()
        val jsonBody = JSONObject(responseJson.getString("body"))
        val lyricsSegments: List<LyricsSegment> = gson.fromJson(
            jsonBody.getString("segments"), object : TypeToken<List<LyricsSegment>>() {}.type
        )
        val lyricsWords: List<LyricsWord> = gson.fromJson(
            jsonBody.getString("words"), object : TypeToken<List<LyricsWord>>() {}.type
        )
        if (lyricsSegments.isEmpty()) {
            Log.e("LyricsProvider", " - bad response - lyrics are empty :(")
        } else {
            Log.i("LyricsProvider", " - response: \n $lyricsSegments")
        }
        return generateSegments(
            // remove duplications
            words = handleDuplicates(lyricsWords),
            // filter segments with no relevant cases
            segments = lyricsSegments.filter { lyricsSegment -> lyricsSegment.end < audioDuration && lyricsSegment.text.isNotEmpty() })
    }

    companion object {
        private var sCredProvider: CognitoCachingCredentialsProvider? = null
    }
}