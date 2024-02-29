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
                context,
                AWSKeys.COGNITO_POOL_ID,  // Identity Pool ID
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
        songName: String,
        objectKey: String,
        audioDuration: Double
    ): List<LyricsTimestampedSegment> {
        initLambda()
        var result = listOf<LyricsTimestampedSegment>()
        val payload = "{\"song\": \"$songName\", \"url\":\"$objectKey\"}"
        val payloadBuffer = ByteBuffer.wrap(payload.toByteArray(StandardCharsets.UTF_8))
        val invokeRequest = InvokeRequest()
            .withFunctionName(AWSKeys.LYRICS_LAMBDA_NAME)
            .withPayload(payloadBuffer)
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
                "LyricsProvider", " - bad response - something went wrong :(\n ${e.message}\n" +
                        "${e.localizedMessage}\n" +
                        "${e.cause}"
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
        words: List<LyricsWord>,
        segments: List<LyricsSegment>
    ): List<LyricsTimestampedSegment> {
        var wordIndex = 0
        val wordsLen = words.size
        return segments.mapNotNull { segment ->
            val segmentWords = segment.text.trim().split("\\s+|-|\\*".toRegex())
            val timestampedWords = segmentWords.mapNotNull { word ->
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
            }.toMutableList()

            // TODO: make a shift function and spread accordingly
            //  the segments words timeline according to segment original boundaries
            if (timestampedWords.isNotEmpty()) {
                if (timestampedWords.first().start < segment.start && timestampedWords.first().end > segment.start) {
                    timestampedWords[0] = LyricsWord(
                        word = timestampedWords.first().word,
                        start = segment.start,
                        end = timestampedWords.first().end
                    )
                }
                if (timestampedWords.last().end > segment.end && timestampedWords.last().start < segment.end) {
                    timestampedWords[timestampedWords.size - 1] = LyricsWord(
                        word = timestampedWords.last().word,
                        start = timestampedWords.last().start,
                        end = segment.end
                    )
                }
            }
            if (timestampedWords.isEmpty()) null else LyricsTimestampedSegment(timestampedWords)
        }
    }

    fun extractData(payloadString: String, audioDuration: Double): List<LyricsTimestampedSegment> {
        val responseJson = JSONObject(payloadString)
        val gson = Gson()
        val jsonBody = JSONObject(responseJson.getString("body"))
        val lyricsSegments: List<LyricsSegment> = gson.fromJson(
            jsonBody.getString("segments"),
            object : TypeToken<List<LyricsSegment>>() {}.type
        )
        val lyricsWords: List<LyricsWord> = gson.fromJson(
            jsonBody.getString("words"),
            object : TypeToken<List<LyricsWord>>() {}.type
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
            segments = lyricsSegments.filter { lyricsSegment -> lyricsSegment.end < audioDuration && lyricsSegment.text.isNotEmpty() }
        )
    }

    companion object {
        private var sCredProvider: CognitoCachingCredentialsProvider? = null
    }
}