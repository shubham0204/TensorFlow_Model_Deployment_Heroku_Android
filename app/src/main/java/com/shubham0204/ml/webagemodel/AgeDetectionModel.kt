package com.shubham0204.ml.webagemodel

import android.graphics.Bitmap
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt

// ML Model to detect the age of a person, hosted on Heroku
class AgeDetectionModel {

    private val herokuModelPredictURL = "https://age-detection-tf-app.herokuapp.com/predict"
    private val mediaType = "application/json".toMediaType()
    private val okHttpClient = OkHttpClient()

    interface PredictionCallback {
        fun onResult( age : Int )
        fun onError( error : String )
    }

    fun pass( inputImage : Bitmap , predictionCallback: PredictionCallback ) {
        // Make the POST request
        val requestBody = RequestBody.create( mediaType , makeRequestBody( inputImage ) )
        val request = Request.Builder()
            .url( herokuModelPredictURL )
            .post( requestBody )
            .build()
        // Execute the request asynchronously
        okHttpClient.newCall( request ).enqueue( object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                predictionCallback.onError( e.message!! )
            }

            override fun onResponse(call: Call, response: Response) {
                val result = JSONObject( response.body!!.string() )
                val age = result.getJSONArray( "prob" ).get( 0 ) as Double
                predictionCallback.onResult( age.roundToInt() )
            }

        })
    }

    private fun makeRequestBody( image : Bitmap ) : String {
        val imageEncoding = bitmapToBase64( image )
        // Return the request body's JSON
        return "{ \"image\" : \"${imageEncoding}\" }"
    }

    // Transform Bitmap into base64 String
    private fun bitmapToBase64( image : Bitmap ) : String {
        val imageByteArray = ByteArrayOutputStream().run {
            image.compress( Bitmap.CompressFormat.PNG , 100 , this )
            toByteArray()
        }
        return Base64.encodeToString( imageByteArray , Base64.NO_WRAP )
    }
}