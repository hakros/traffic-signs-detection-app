package com.hakros.trafficsigndetector

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImgSize {
    const val WIDTH = 30
    const val HEIGHT = 30
    const val PIXEL_COUNT = WIDTH * HEIGHT
}

class MainActivity : ComponentActivity() {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var model: Interpreter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        model = getModel()

        requestCameraPermission()

        val takePhotoBtn = findViewById<Button>(R.id.image_capture_button)

        takePhotoBtn.setOnClickListener {
            takePhoto()
        }
    }

    private fun requestCameraPermission() {
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    1
                )
            } else {
                openCamera()
            }
        }.launch(android.Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        println("openCamera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            /**
             * Allow image capturing using the camera
             */
            imageCapture = ImageCapture.Builder().build()

            try {
                // Remove all bound cameras
                cameraProvider.unbindAll()

                bindCameraPreview()
            } catch (e: Exception) {
                println(e.printStackTrace())
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview() {
        val camera = cameraProvider.bindToLifecycle(
            this, cameraSelector, preview, imageCapture
        )
    }

    private fun takePhoto() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageCapturedCallback() {
                override fun onError(exception: ImageCaptureException) {
                    println(exception.printStackTrace())
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()

                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        ImgSize.WIDTH,
                        ImgSize.HEIGHT,
                        true
                    )

                    model?.let {
                        val res = runInference(
                            model= it,
                            inputBuffer = bitmapToByteBuffer(scaledBitmap)
                        )

                        println(res)

                        it.close()
                    }

                    image.close()
                }
            }
        )
    }

    private fun getModel(): Interpreter? {
        val modelFile: File = getRawFile(R.raw.traffic_sign_ai, "traffic_sign_ai.tflite")

        if (!modelFile.exists()) {
            println("Model File Does Not Exist: ${modelFile.path}")
            return null
        }

        try {
            val interpreter = Interpreter(modelFile)

            return interpreter
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun getRawFile(resourceId: Int, outputFileName: String): File {
        val inputStream: InputStream = resources.openRawResource(resourceId)
        val outputFile = File(filesDir, outputFileName)

        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        return outputFile
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * ImgSize.WIDTH * ImgSize.HEIGHT * 3)
        buffer.rewind()

        for (y in 0 until 30) {
            for (x in 0 until 30) {
                val pixel = bitmap.getPixel(x, y)
                buffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
                buffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)
                buffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        }

        return buffer
    }

    private fun runInference(model: Interpreter, inputBuffer: ByteBuffer): String {
        val outputSize = ImgSize.WIDTH * ImgSize.HEIGHT * 3
        val outputBuffer = Array(1) {
            FloatArray(43)
        }

        model.run(
            inputBuffer,
            outputBuffer
        )

        Log.d("TFLiteModel", "Output probabilities: ${outputBuffer.contentDeepToString()}")

        val categoryIndex = outputBuffer[0].indices.maxByOrNull { outputBuffer[0][it] } ?: -1

        val categories = arrayOf(
            "Category 0",
            "Category 1",
            "Category 2",
            "Category 3",
            "Category 4",
            "Category 5",
            "Category 6",
            "Category 7",
            "Category 8",
            "Category 9",
            "Category 10",
            "Category 11",
            "Category 12",
            "Category 13",
            "Category 14",
            "Category 15",
            "Category 16",
            "Category 17",
            "Category 18",
            "Category 19",
            "Category 20",
            "Category 21",
            "Category 22",
            "Category 23",
            "Category 24",
            "Category 25",
            "Category 26",
            "Category 27",
            "Category 28",
            "Category 29",
            "Category 30",
            "Category 31",
            "Category 32",
            "Category 33",
            "Category 34",
            "Category 35",
            "Category 36",
            "Category 37",
            "Category 38",
            "Category 39",
            "Category 40",
            "Category 41",
            "Category 42"
        )

        println(categoryIndex)

        return categories[categoryIndex]
    }
}