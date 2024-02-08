package com.ziad.sayit.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.ziad.sayit.R
import com.ziad.sayit.databinding.ActivityAdjustImageBinding
import java.io.File

//Documentation
//https://github.com/CanHub/Android-Image-Cropper

class AdjustImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdjustImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdjustImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialisations
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val doneBtn = findViewById<Button>(R.id.done_btn)
        val cancelBtn = findViewById<Button>(R.id.cancel_btn)
        val rotateBtn = findViewById<ImageButton>(R.id.rotate_btn)


        //Set Appbar
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = "Crop & Rotate" //title

        //Appbar Navigation
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener { v: View? ->
            //What to do on back clicked
            finish()
        }


        //Receive image data
        val imageUri = intent.data


        //Set Crop Image Layout
        val cropImageView: CropImageView = findViewById(R.id.cropImageView)
        cropImageView.setImageCropOptions(CropImageOptions(
            guidelines = CropImageView.Guidelines.ON
            //here you can customize
            //..
        ))
        cropImageView.setImageUriAsync(imageUri)



        ///Buttons ClickListeners
        //Done
        doneBtn.setOnClickListener {
            // subscribe to async event using cropImageView.setOnCropImageCompleteListener(listener)
            cropImageView.setOnCropImageCompleteListener { _, result ->
                //get data
                val returnIntent = Intent()
                returnIntent.data = result.uriContent
                setResult(RESULT_OK, returnIntent)

                finish() //Go back
            }


            //The result will be invoked to listener set by setOnCropImageCompleteListener.
            //Save to a specific location
            cropImageView.croppedImageAsync(
                customOutputUri = croppedImageUriFile()
            )
        }

        //Cancel
        cancelBtn.setOnClickListener { finish() }

        //Rotate
        rotateBtn.setOnClickListener { cropImageView.rotateImage(90) }
    }


    private fun croppedImageUriFile(): Uri? {
        var fileName = externalCacheDir!!.absolutePath
        fileName += "/SayIt_tempCroppedImage.jpg"
        return Uri.fromFile(File(fileName))
    }
}