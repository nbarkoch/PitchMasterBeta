package com.example.pitchmasterbeta.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.event.ProgressEvent
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.InvokeRequest
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.example.pitchmasterbeta.MainActivity.Companion.viewModelProvider
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
import com.example.pitchmasterbeta.notifications.SpleeterProgressNotification
import com.example.pitchmasterbeta.ui.workspace.WorkspaceViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SpleeterService : Service() {

    private val apiCoroutineScope = CoroutineScope(Dispatchers.IO)
    private var serviceNotifier: ServiceNotifier? = null

    private lateinit var sCredProvider: CognitoCachingCredentialsProvider
    private lateinit var s3Client: AmazonS3Client
    private lateinit var lambdaClient: AWSLambda

    private var putObjectRequest: PutObjectRequest? = null
    private var uploadedInputStream: InputStream? = null
    private var isActive: Boolean = false

    private var spleeterNotification: SpleeterProgressNotification? = null

    interface ServiceNotifier {
        fun notifyCompletion(
            vocalsFile: File,
            accompanimentFile: File,
            lyrics: List<LyricsTimestampedSegment>,
            shouldSaveData: Boolean = false
        )

        fun notifyFailed()

        fun notifyProgressChanged(progress: Int, message: String)

        fun notifyProgressChanged(progress: Int, message: String, duration: Double)
    }


    object KEYS {
        const val EXTRA_FILE_URI = "EXTRA_FILE_URI"
        const val EXTRA_OBJECT_KEY = "EXTRA_OBJECT_KEY"
        const val EXTRA_FILE_NAME = "EXTRA_FILE_NAME"
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        serviceNotifier = viewModelProvider[WorkspaceViewModel::class.java]
        if (!isActive) {
            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(KEYS.EXTRA_FILE_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(KEYS.EXTRA_FILE_URI)
            }

            val objectKey = intent.getStringExtra(KEYS.EXTRA_OBJECT_KEY)
            val songName = intent.getStringExtra(KEYS.EXTRA_FILE_NAME)

            if (fileUri != null && objectKey != null && songName != null) {
                spleeterNotification = SpleeterProgressNotification(this)
                startForeground(
                    SpleeterProgressNotification.NOTIFICATION_ID,
                    spleeterNotification?.buildNotification()
                )
                startBackgroundThread(fileUri, objectKey, songName)
            }
        }
        return START_STICKY
    }

    private fun initServices() {
        val clientConfiguration = ClientConfiguration().apply {
            connectionTimeout = 1000 * 60 * 4
            socketTimeout = 1000 * 60 * 4
        }

        sCredProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            AWSKeys.COGNITO_POOL_ID,
            AWSKeys.MY_REGIONS,
            clientConfiguration
        )

        s3Client = AmazonS3Client(sCredProvider, clientConfiguration).apply {
            setRegion(AWSKeys.MY_REGION)
        }

        lambdaClient = AWSLambdaClient(sCredProvider, clientConfiguration).apply {
            setRegion(AWSKeys.MY_REGION)
        }
    }


    private suspend fun startUploadToS3(fileUri: Uri, objectKey: String): Boolean {
        return suspendCoroutine { continuation ->
            try {
                StrictMode.setThreadPolicy(ThreadPolicy.Builder().permitAll().build())

                uploadedInputStream = contentResolver.openInputStream(fileUri)
                val objectMetadata = ObjectMetadata().apply {
                    contentLength = uploadedInputStream?.available()?.toLong() ?: 0L
                }
                var totalTransferred = 0L
                val progressChunk = 20
                putObjectRequest = PutObjectRequest(
                    AWSKeys.BUCKET_NAME,
                    "input/$objectKey", uploadedInputStream, objectMetadata
                ).apply {
                    cannedAcl = CannedAccessControlList.PublicRead
                    setGeneralProgressListener { progressEvent ->
                        when (progressEvent.eventCode) {
                            ProgressEvent.COMPLETED_EVENT_CODE -> {
                                continuation.resume(true)
                            }
                            ProgressEvent.FAILED_EVENT_CODE -> {
                                serviceNotifier?.notifyFailed()
                                continuation.resume(false)
                            }
                            else -> {
                                totalTransferred += progressEvent.bytesTransferred
                                val progress = totalTransferred.toDouble() / objectMetadata.contentLength
                                Log.d("Upload Progress", "${(100 * progress).toInt()}%")
                                notifyProgressChanged(
                                    (progressChunk * progress).toInt(),
                                    "Uploading The file",
                                    1.0
                                )
                            }
                        }
                    }
                }
                s3Client.putObject(putObjectRequest)
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
                continuation.resume(false) // Resume with false if an exception occurs
            }
        }
    }


    private fun checkItself() {
        if (!isActive) {
            serviceNotifier?.notifyFailed()
            endSelf()
        }
    }

    private fun endSelf() {
        s3Client.shutdown()
        lambdaClient.shutdown()
        stopSelf()
    }

    data class ResultLambda(
        var lyrics: List<LyricsTimestampedSegment>,
        var vocalsUrl: URL,
        var accompanimentUrl: URL
    )

    private suspend fun invokeLambdaFunction(songName: String, objectKey: String): ResultLambda? {
        return suspendCoroutine { continuation ->
            val payload = "{\"file_name\": \"$songName\", \"object_key\":\"$objectKey\"}"
            val payloadBuffer = ByteBuffer.wrap(payload.toByteArray(StandardCharsets.UTF_8))
            val invokeRequest = InvokeRequest()
                .withFunctionName(AWSKeys.KARAOKE_FUNCTION)
                .withPayload(payloadBuffer)

            try {
                lambdaClient.run {
                    val invokeResult = this.invoke(invokeRequest)
                    val statusCode = invokeResult.statusCode
                    checkItself()
                    if (statusCode == 200) {
                        val responseJson = JSONObject(String(invokeResult.payload.array()))
                        Log.i("lambdaFunction", "200 - response: \n $responseJson")
                        val gson = Gson()
                        val jsonBody = JSONObject(responseJson.getString("body"))
                        val lyrics = gson.fromJson<List<LyricsTimestampedSegment>>(
                            jsonBody.getString("timestamped_segments"),
                            object : TypeToken<List<LyricsTimestampedSegment>>() {}.type
                        )
                        val urls = jsonBody.getJSONObject("urls")
                        val vocalsUrl = URL(urls.getString("vocals"))
                        val accompanimentUrl = URL(urls.getString("accompaniment"))
                        continuation.resume(ResultLambda(lyrics, vocalsUrl, accompanimentUrl))
                    } else {
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("lambdaFunction", "Exception occurred: ${e.message}")
                continuation.resume(null)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundThread()
    }

    private fun startBackgroundThread(uri: Uri, objectKey: String, songName: String) {
        isActive = true
        apiCoroutineScope.launch(Dispatchers.IO) {
            initServices()
            val s3UploadResult = startUploadToS3(uri, objectKey)
            if (!s3UploadResult) {
                serviceNotifier?.notifyFailed()
                endSelf()
                return@launch
            }
            checkItself()
            notifyProgressChanged(60, "Separating..", 45.0)
            val lambdaResult = invokeLambdaFunction(songName, objectKey)
            if (lambdaResult == null) {
                serviceNotifier?.notifyFailed()
                endSelf()
                return@launch
            }
            checkItself()
            val fileResults = downloadFiles(lambdaResult.vocalsUrl, lambdaResult.accompanimentUrl)
            if (fileResults == null) {
                serviceNotifier?.notifyFailed()
                endSelf()
                return@launch
            }
            serviceNotifier?.notifyCompletion(
                fileResults.vocalsFile, fileResults.accompanimentFile, lambdaResult.lyrics,
                true
            )
            endSelf()
        }
    }

    private fun stopBackgroundThread() {
        apiCoroutineScope.cancel()
        isActive = false
        spleeterNotification?.hideNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun notifyProgressChanged(progress: Int, message: String, duration: Double) {
        spleeterNotification?.updateProgress(progress, message, duration * 1000.0)
        serviceNotifier?.notifyProgressChanged(progress, message, duration)
    }

    data class DownloadFilesResult(val vocalsFile: File, val accompanimentFile: File)

    private suspend fun downloadFiles(vocalsUrl: URL, accompanimentUrl: URL): DownloadFilesResult? {
        return suspendCoroutine { continuation ->
            apiCoroutineScope.launch(Dispatchers.IO) {
                try {
                    val progressChunk = 40
                    var progress1 = 0f
                    var progress2 = 0f
                    val (downloadedVocalFile, downloadedAccompanimentFile) = kotlinx.coroutines.awaitAll(
                        async {
                            downloadAudioFile(vocalsUrl) { progress ->
                                progress1 = progress
                                notifyProgressChanged(
                                    (progressChunk * (progress1 + progress2) / 2f).toInt() + 60,
                                    "Extracting results",
                                    1.0
                                )
                                // Log the progress
                                Log.d("Download Progress", "file1: ${progress*100}%")
                            }
                        },
                        async {
                            downloadAudioFile(accompanimentUrl) { progress ->
                                progress2 = progress
                                notifyProgressChanged(
                                    (progressChunk * (progress1 + progress2) / 2f).toInt() + 60,
                                    "Extracting results",
                                    1.0
                                )
                                // Log the progress
                                Log.d("Download Progress", "file2: ${progress*100}%")
                            }
                        }
                    )
                    if (downloadedVocalFile == null || downloadedAccompanimentFile == null || !(downloadedVocalFile.exists() && downloadedAccompanimentFile.exists())) {
                        //"Downloaded files not found"
                        continuation.resume(null)
                    } else {
                        continuation.resume(
                            DownloadFilesResult(
                                downloadedVocalFile,
                                downloadedAccompanimentFile
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    //"Exception occurred during download: ${e.message}")
                    continuation.resume(null)
                }
            }
        }
    }


    private fun downloadAudioFile(url: URL, onProgressChanged: (progress: Float) -> Unit): File? {
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // Create a temporary file to store the downloaded content
                val audioFile =
                    File.createTempFile(url.path.replace("/", "").replace(".", "_"), null)
                val outputStream = FileOutputStream(audioFile)

                // Download the content
                val inputStream = connection.inputStream
                val fileSize = connection.contentLength
                var totalBytesRead = 0
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    onProgressChanged(totalBytesRead.toFloat() / fileSize)
                }

                // Close the streams
                inputStream.close()
                outputStream.close()

                return audioFile
            } else {
                // Handle the case when the connection fails
            }

        } catch (e: IOException) {
            e.printStackTrace()
            // Handle any errors that occur during download or extraction
        }
        return null
    }

}