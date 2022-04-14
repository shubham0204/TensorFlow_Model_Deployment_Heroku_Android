package com.shubham0204.ml.webagemodel

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.shubham0204.ml.webagemodel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .build()
    private val firebaseFaceDetector = FaceDetection.getClient(faceDetectorOptions)
    private val ageDetectionModel = AgeDetectionModel()
    private lateinit var activityMainBinding : ActivityMainBinding
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate( layoutInflater )
        setContentView( activityMainBinding.root )

        progressDialog = ProgressDialog( this )
        progressDialog.setCancelable( false )
        activityMainBinding.selectImageButton.setOnClickListener {
            dispatchSelectPictureIntent()
        }

    }

    // Dispatch Intent to let the user select an image from the gallery
    private fun dispatchSelectPictureIntent() {
        val selectPictureIntent = Intent( Intent.ACTION_OPEN_DOCUMENT ).apply {
            type = "image/*"
            addCategory( Intent.CATEGORY_OPENABLE )
        }
        activityResultLauncher.launch( selectPictureIntent )
    }

    // Intercept the result of the select image intent here.
    private val activityResultLauncher = registerForActivityResult( ActivityResultContracts.StartActivityForResult() ) {
        if ( it.data == null ) {
            // If the user didn't select any image from the gallery
            return@registerForActivityResult
        }
        progressDialog.setMessage( "🤳 Detecting faces ..." )
        progressDialog.show()
        val inputStream = contentResolver.openInputStream( it.data?.data!! )
        val bitmap = BitmapFactory.decodeStream( inputStream )
        inputStream?.close()
        detectFaces( bitmap )
    }

    // Detect faces in the given image and crop them.
    // Pass the cropped faces to the AgeDetectionModel
    private fun detectFaces(image: Bitmap) {
        val inputImage = InputImage.fromBitmap(image, 0)
        firebaseFaceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if ( faces.size != 0 ) {
                    progressDialog.apply {
                        dismiss()
                        setMessage( "📍 Posting to server ...")
                        show()
                    }
                    // Crop the image using the boundingBox predicted by the face detector
                    val inputImage = cropToBBox( image , faces[ 0 ].boundingBox )
                    activityMainBinding.imageView.setImageBitmap( inputImage )
                    // Pass the cropped face to AgeDetectionModel
                    ageDetectionModel.pass( inputImage , predictionCallback )
                }
                else {
                    // Display a warning to the user
                    val dialog = AlertDialog.Builder( this ).apply {
                        title = "No Faces Found"
                        setMessage( "We could not find any faces in the image you just clicked. " +
                                "Try clicking another image or improve the lightning or the device rotation." )
                        setPositiveButton( "OK") { dialog, which ->
                            dialog.dismiss()
                            progressDialog.dismiss()
                        }
                        setCancelable( false )
                        create()
                    }
                    dialog.show()
                }
            }
    }

    // A callback to get the age detection results from the AgeDetectionModel
    // This callback is passed to the `AgeDetectionModel.pass` method in `detectFaces()`
    private val predictionCallback = object : AgeDetectionModel.PredictionCallback {
        override fun onError(error: String) {
            runOnUiThread {
                progressDialog.dismiss()
                val dialog = AlertDialog.Builder( this@MainActivity ).apply {
                    title = "An error occurred"
                    setMessage( error )
                    setPositiveButton( "OK") { dialog, which ->
                        dialog.dismiss()
                    }
                    setCancelable( false )
                    create()
                }
                dialog.show()
            }
        }

        override fun onResult(age: Int) {
            runOnUiThread {
                progressDialog.dismiss()
                val dialog = AlertDialog.Builder( this@MainActivity ).apply {
                    title = "Result"
                    setMessage( "The result is $age years" )
                    setPositiveButton( "OK") { dialog, which ->
                        dialog.dismiss()
                    }
                    setCancelable( false )
                    create()
                }
                dialog.show()
            }
        }
    }

    // Crop the given image using bbox
    private fun cropToBBox(image: Bitmap, bbox: Rect) : Bitmap {
        return Bitmap.createBitmap(
            image,
            bbox.left - 25,
            bbox.top - 25 ,
            bbox.width() + 50 ,
            bbox.height() + 50
        )
    }

}

