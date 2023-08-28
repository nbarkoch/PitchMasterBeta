package com.example.pitchmasterbeta.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.event.ProgressEvent
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.InvokeRequest
import com.amazonaws.services.lambda.model.InvokeResult
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.example.pitchmasterbeta.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class SpleeterService : Service() {

    private val apiCoroutineScope = CoroutineScope(Dispatchers.IO)
    private var serviceNotifier: ServiceNotifier? = null

    private lateinit var sCredProvider: CognitoCachingCredentialsProvider
    private lateinit var s3Client: AmazonS3Client
    private lateinit var lambdaClient: AWSLambda

    private var putObjectRequest: PutObjectRequest? = null
    private var uploadedInputStream: InputStream? = null


    private var vocalsUrl: URL? = null
    private var accompanimentUrl: URL? = null
    private var isActive: Boolean = false

    interface ServiceNotifier {
        fun notifyCompletion(vocalsUrl: URL, accompanimentUrl: URL)

        fun notifyFailed()

        fun notifyProgressChanged(progress: Int, message: String)

        fun notifyProgressChanged(progress: Int, message: String, duration: Double)
    }



    object KEYS {
        const val EXTRA_FILE_URI = "EXTRA_FILE_URI"
        const val EXTRA_OBJECT_KEY = "EXTRA_OBJECT_KEY"
    }



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        serviceNotifier = MainActivity.viewModelStore["workspace"] as ServiceNotifier?

        if (!isActive && serviceNotifier != null) {
            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(KEYS.EXTRA_FILE_URI, Uri::class.java)
            } else {
                intent.getParcelableExtra(KEYS.EXTRA_FILE_URI)
            }

            val objectKey = intent.getStringExtra(KEYS.EXTRA_OBJECT_KEY)

            if (fileUri != null && objectKey != null) {
                startBackgroundThread(fileUri, objectKey)
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


    private fun startUploadToS3(fileUri: Uri, objectKey: String) {
        try {
            StrictMode.setThreadPolicy(ThreadPolicy.Builder().permitAll().build())

            uploadedInputStream = contentResolver.openInputStream(fileUri)
            val objectMetadata = ObjectMetadata().apply {
                contentLength = uploadedInputStream?.available()?.toLong() ?: 0L
            }

            putObjectRequest = PutObjectRequest(
                AWSKeys.BUCKET_NAME,
                "input/$objectKey", uploadedInputStream, objectMetadata
            ).apply {
                cannedAcl = CannedAccessControlList.PublicRead
                setGeneralProgressListener { progressEvent ->
                    when (progressEvent.eventCode) {

                        ProgressEvent.COMPLETED_EVENT_CODE -> {
                            checkItself()
                            serviceNotifier?.notifyProgressChanged(60, "Separating..", 40 * 1000.0)
                            val lambdaExecutionResult = invokeLambdaFunction(objectKey)
                            serviceNotifier?.notifyProgressChanged(100, "Extracting results", 35 * 1000.0)
                            checkItself()
                            vocalsUrl?.takeIf { lambdaExecutionResult }
                                ?.let { vocalsUrl ->
                                    accompanimentUrl
                                        ?.let { accompanimentUrl ->
                                            serviceNotifier?.notifyCompletion(
                                                vocalsUrl,
                                                accompanimentUrl
                                            )
                                        }
                                } ?: serviceNotifier?.notifyFailed()
                            stopSelf()
                        }

                        ProgressEvent.FAILED_EVENT_CODE -> serviceNotifier?.notifyFailed()
                        else -> {}
                    }
                }
            }
            checkItself()
            serviceNotifier?.notifyProgressChanged(20, "Uploading The file", 5 * 1000.0)
            s3Client.putObject(putObjectRequest)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun checkItself() {
        if (!isActive) {
            s3Client.shutdown()
            lambdaClient.shutdown()
            serviceNotifier?.notifyFailed()
            throw Exception("Spleeter Interrupted")
        }
    }


    private fun invokeLambdaFunction(objectKey: String): Boolean {
        val payload = "{\"inputFile\": \"$objectKey\"}"
        val payloadBuffer = ByteBuffer.wrap(payload.toByteArray(StandardCharsets.UTF_8))
        val invokeRequest: InvokeRequest = InvokeRequest()
            .withFunctionName(AWSKeys.LAMBDA_NAME)
            .withPayload(payloadBuffer)
        try {
            val invokeResult: InvokeResult? = lambdaClient.invoke(invokeRequest)
            val statusCode: Int? = invokeResult?.statusCode
            vocalsUrl = s3Client.generatePresignedUrl(
                AWSKeys.BUCKET_NAME,
                "output/" + objectKey + "_vocals.mp3",
                null
            )
            accompanimentUrl = s3Client.generatePresignedUrl(
                AWSKeys.BUCKET_NAME,
                "output/" + objectKey + "_accompaniment.mp3",
                null
            )
            return statusCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundThread()
    }

    private fun startBackgroundThread(uri: Uri, objectKey: String) {
        isActive = true
        apiCoroutineScope.launch {
            initServices()
            startUploadToS3(uri, objectKey)
        }
    }

    private fun stopBackgroundThread() {
        apiCoroutineScope.cancel()
        isActive = false
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}