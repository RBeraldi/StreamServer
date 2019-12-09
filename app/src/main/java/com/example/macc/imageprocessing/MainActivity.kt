package com.example.macc.imageprocessing

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION
import android.hardware.camera2.CameraManager
import android.net.Uri

import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getBitmap
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley


import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset


class MainActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {



    lateinit var mOpenCvCameraView :CameraBridgeViewBase
    lateinit var cameraManager :CameraManager
    lateinit var queue : RequestQueue

    var stored = false
    var filename = "frame"

    var id = 0


    lateinit var file :File

    val TAG = "CAMERA"
    val TAG2 = "JSON"

    val MY_CAMERA_REQUEST_CODE = 1

    val port = "5000"
    val ip = "192.168.1.21"
//    val ip = "172.20.10.4"
    val url = "http://"+ip+":"+port //Change according to the server...


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        setContentView(R.layout.activity_main)


        queue  =  Volley.newRequestQueue(this)

        cameraManager = getSystemService (CAMERA_SERVICE) as CameraManager


        mOpenCvCameraView = findViewById(R.id.fd_activity_surface_view) as CameraBridgeViewBase
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView()
        mOpenCvCameraView.enableFpsMeter()


        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),MY_CAMERA_REQUEST_CODE)
        else
            mOpenCvCameraView.setCameraPermissionGranted()

        if (OpenCVLoader.initDebug()){
  //         Toast.makeText(this,"openCV Loaded. Version is: "+OpenCVLoader.OPENCV_VERSION,Toast.LENGTH_SHORT).show()
        }


    }

    override fun onCameraViewStarted(width: Int, height: Int) {
//        Toast.makeText(this,"Camera started",Toast.LENGTH_SHORT).show()
        Log.i(TAG,"Camera Started...")

    }

    override fun onCameraViewStopped() {
  //      Toast.makeText(this,"Camera stopped",Toast.LENGTH_SHORT).show()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
    //    Toast.makeText(this,"Camera Frame ",Toast.LENGTH_SHORT).show()
        //Log.i(TAG,"Frame received.. ")
        val mRgba = inputFrame?.rgba() as Mat
        val cols = mRgba.cols()
        val rows = mRgba.rows()

        var bitmap : Bitmap = Bitmap.createBitmap(cols,rows,Bitmap.Config.ARGB_8888)
        try { Utils.matToBitmap(mRgba,bitmap) } catch (e: Exception ) {Log.i(TAG,e.toString())}

        var matrix =  Matrix()
        matrix.setRotate(90f)

        // rotating image since sensor cameras are rotate of 90 wrt smartphone orientation (may depend on the model)
        val output = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true);
        postImage(output)



        val  center = Point(cols.toDouble()/2, rows.toDouble()/2)
        val rotMatrix = Imgproc.getRotationMatrix2D(center, -90.0, 1.0) //1.0 means 100 % scale

        Imgproc.warpAffine(mRgba, mRgba, rotMatrix, mRgba.size());

        return mRgba
    }



    fun postImage(x: Bitmap) {


//        Log.i(TAG,"Profile: "+x.config.toString())


        var p = HashMap<String, String>()


        p["x"] = x.width.toString()
        p["y"] = x.height.toString()
        p["density"]= x.density.toString()


        var msg = ""

        var y = ByteArrayOutputStream()
        x.compress(Bitmap.CompressFormat.JPEG, 100, y)


       //val stringTosend = y.toByteArray().toString(Charsets.UTF_8)
        val stringTosend =  java.util.Base64.getEncoder().
                            encodeToString(y.toByteArray()).
                            toByteArray(Charsets.UTF_8)

        //val stringTosend = y.toByteArray().toString(Charsets.UTF_8)


        filename=filename+".txt"

        if (stored==false)
            openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(stringTosend)
            stored=true
        }


        p["image"]= stringTosend.toString(Charsets.UTF_8)



        val body = JSONObject(p.toMap())

        //Riconvertire il json in bitmap
//        val output2 = body ["image"] as ByteArrayOutputStream


          //  body.keys().forEach { s-> Log.i(TAG2,s.toString())}

        //Log.i(TAG2,body.getString("image").length.toString()+" "+body.getString("chars").length)

        //Log.i(TAG,output.size().toString()+" "+output2.size().toString())




        val stringRequest = JsonObjectRequest(
            Request.Method.POST, url, body,
            Response.Listener<JSONObject> { response ->
                Log.i(TAG,"Server is OK $response")
            },
            Response.ErrorListener { error: VolleyError? ->
                Log.i(TAG,"Server is NOK: $error")
            })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)

    }

}
