package com.example.superApp;


import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import android.content.Context

import android.graphics.*
import android.media.Image
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.ImageProxy
import java.io.OutputStream
import java.util.*
import kotlin.random.Random
import android.widget.ImageView

import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min



class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private var colorEffects: List<(Bitmap) -> Bitmap> = listOf(
        ::toNegative,
        ::toGrayscale,
        ::toNatural,
        ::applySwirlEffect
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()


        setContentView(R.layout.activity_main)
    
        val bg = findViewById<ImageView>(R.id.backgroundImage)
        bg.animate().alpha(1f).setDuration(300).start()

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)

                takePhoto()

            } catch (e: Exception) {
                Toast.makeText(this, "Camera binding failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        
        if(imageCapture == null)
        {
            Toast.makeText(this@MainActivity, "Este Null", Toast.LENGTH_SHORT).show()
        }

        imageCapture?.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            
        override fun onCaptureSuccess(image: ImageProxy) {
            val bitmap = imageProxyToBitmap(image)
            image.close()

            val filteredBitmap = colorEffects.random()(bitmap)

            val rotatedBitmap = rotateBitmap(filteredBitmap, Random.nextDouble(0.0, 360.0).toFloat())

            saveBitmapToGallery(rotatedBitmap, this@MainActivity)
            finish()
        }

        override fun onError(exception: ImageCaptureException) {
            Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun toNatural(src: Bitmap): Bitmap {
        return src
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val bmpGrayscale = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmpGrayscale
    }

    private fun toNegative(src: Bitmap): Bitmap {
        val bmpNegative = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpNegative)
        val paint = Paint()
        val colorMatrix = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmpNegative
    }

    private fun applySwirlEffect(src: Bitmap): Bitmap {
        val swirlStrength = 1.0f
        val width = src.width
        val height = src.height
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val distance = hypot(dx, dy)
                val angle = atan2(dy, dx)

                val swirl = angle + swirlStrength * ((radius - distance) / radius).coerceAtLeast(0f)

                val srcX = (centerX + distance * cos(swirl)).toInt()
                val srcY = (centerY + distance * sin(swirl)).toInt()

                val dstIndex = y * width + x
                if (srcX in 0 until width && srcY in 0 until height) {
                    val srcIndex = srcY * width + srcX
                    dstPixels[dstIndex] = srcPixels[srcIndex]
                } else {
                    dstPixels[dstIndex] = Color.TRANSPARENT
                }
            }
        }

        return Bitmap.createBitmap(dstPixels, width, height, src.config)
    }



    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, context: Context) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/superApp")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        }
    }

}
