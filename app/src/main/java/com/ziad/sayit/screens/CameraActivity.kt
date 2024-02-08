package com.ziad.sayit.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.MotionEvent
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ziad.sayit.ImageHandler
import com.ziad.sayit.R
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class CameraActivity : AppCompatActivity() {
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var flashMode: Int = ImageCapture.FLASH_MODE_AUTO



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus_darkTheme)
            window.decorView.systemUiVisibility = 0
        }

        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        flashMode = sharedPref.getInt("flash_mode_preference_key", ImageCapture.FLASH_MODE_OFF)
        setUpFlashButton(flashMode)


        // /////////////// Open Camera /////////////////////
        startCamera()
        //

        // Setup the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }
        flash_button.setOnClickListener {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF ->{
                    flashMode = ImageCapture.FLASH_MODE_ON
                    flash_button.setImageResource(R.drawable.ic_flash_on)
                }
                ImageCapture.FLASH_MODE_ON ->{
                    flashMode = ImageCapture.FLASH_MODE_AUTO
                    flash_button.setImageResource(R.drawable.ic_flash_auto)
                }
                ImageCapture.FLASH_MODE_AUTO ->{
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    flash_button.setImageResource(R.drawable.ic_flash_off)
                }
            }

            setUpFlashButton(flashMode)

            with (sharedPref.edit()) {
                putInt("flash_mode_preference_key", flashMode)
                apply()
            }
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        setUpTapToFocus()
    }



    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        viewFinder.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewFinder.viewTreeObserver.removeOnGlobalLayoutListener(this)

                cameraProviderFuture.addListener({
                    // Used to bind the lifecycle of cameras to the lifecycle owner
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                    // Preview
                    preview = Preview.Builder()
                        .setTargetResolution(Size(viewFinder.width, viewFinder.height))
                        .build()

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetResolution(Size(viewFinder.width, viewFinder.height))
                        .setFlashMode(flashMode)
                        .build()

                    // Select back camera
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

                    try {
                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll()

                        // Bind use cases to camera
                        camera = cameraProvider.bindToLifecycle(
                            this@CameraActivity, cameraSelector, preview, imageCapture)
                        preview?.setSurfaceProvider(viewFinder.surfaceProvider)
                    } catch(exc: Exception) {
                        //Log.e(TAG, "Use case binding failed", exc)
                        Toast.makeText(this@CameraActivity,
                            "Use case binding failed",
                            Toast.LENGTH_SHORT).show()
                    }

                }, ContextCompat.getMainExecutor(this@CameraActivity))
            }
        })
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = ImageHandler.createImageFile(this)

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    //Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@CameraActivity,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val returnIntent = Intent()
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }
            })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }



    private fun setUpTapToFocus(){
        viewFinder.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    val factory = SurfaceOrientedMeteringPointFactory(
                        v.width.toFloat(),
                        v.height.toFloat()
                    )
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        // auto calling cancelFocusAndMetering in 5 seconds
                        .setAutoCancelDuration(5, TimeUnit.SECONDS)
                        .build()
                    camera?.cameraControl?.startFocusAndMetering(action)

                    v.performClick()
                    return@setOnTouchListener true
                }
            }

            v?.onTouchEvent(event) ?: true
        }
    }

    //----------------------------------------------------------------------------------------------
    private fun setUpFlashButton(flashMode: Int){
        when (flashMode) {
            ImageCapture.FLASH_MODE_OFF ->{
                imageCapture?.flashMode = flashMode
                flash_button.setImageResource(R.drawable.ic_flash_off)
            }
            ImageCapture.FLASH_MODE_ON ->{
                imageCapture?.flashMode = flashMode
                flash_button.setImageResource(R.drawable.ic_flash_on)
            }
            ImageCapture.FLASH_MODE_AUTO ->{
                imageCapture?.flashMode = flashMode
                flash_button.setImageResource(R.drawable.ic_flash_auto)
            }
        }
    }
}


