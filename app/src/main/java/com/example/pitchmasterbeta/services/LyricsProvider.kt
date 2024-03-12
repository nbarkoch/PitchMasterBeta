package com.example.pitchmasterbeta.services

import android.content.Context
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.InvokeRequest
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
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

    fun extractData(payloadString: String, audioDuration: Double): List<LyricsTimestampedSegment> {
        val responseJson = JSONObject(payloadString)
        val gson = Gson()
        val jsonBody = JSONObject(responseJson.getString("body"))
        val timestampedSegments: MutableList<LyricsTimestampedSegment> = gson.fromJson(
            jsonBody.getString("timestamped_segments"),
            object : TypeToken<List<LyricsTimestampedSegment>>() {}.type
        )
        if (timestampedSegments.isEmpty()) {
            Log.e("LyricsProvider", " - bad response - lyrics are empty :(")
        } else {
            Log.i("LyricsProvider", " - response: \n $timestampedSegments")
        }
        for (i in timestampedSegments.indices) {
            timestampedSegments[i] =
                timestampedSegments[i].copy(text = timestampedSegments[i].text.filter { w -> w.start < audioDuration && w.end < audioDuration })
        }
        return timestampedSegments
    }

    companion object {
        private var sCredProvider: CognitoCachingCredentialsProvider? = null
    }
}