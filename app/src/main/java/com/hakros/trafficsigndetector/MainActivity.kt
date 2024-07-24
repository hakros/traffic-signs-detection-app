package com.hakros.trafficsigndetector

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hakros.trafficsigndetector.ui.theme.TrafficSignDetectorTheme
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestCameraPermission()

        setContent {
            TrafficSignDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            val cameraRequestCode = 1

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequestCode)
        }
    }

    private fun dispatchTakePictureIntent() {
        val cameraRequestCode = 1
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        takePictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(takePictureIntent, cameraRequestCode)
        }
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

    private fun runInference(model: Interpreter, inputBuffer: ByteBuffer): String {
        val outputSize = 1024
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize)

        outputBuffer.order(ByteOrder.nativeOrder())

        model.run(
            inputBuffer,
            outputBuffer
        )

        val result = FloatArray(outputSize)

        outputBuffer.asFloatBuffer().get(result)

        return result.joinToString(", ")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrafficSignDetectorTheme {
        Greeting("Android")
    }
}