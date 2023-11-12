package net.taptappun.taku.kobayashi.sharphackathon2023

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.Executors

class ObjectTrackingDetector(context: Context) : ImageDetector<DetectedObject>(context) {
    private var isSave = false

    public override fun detect(image: InputImage) {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()  // Optional
            .build()

        // [END set_detector_options]

        // [START get_detector]
        val objectDetector = ObjectDetection.getClient(options)
        // Or, to use the default option:
        // val detector = FaceDetection.getClient();
        // [END get_detector]

        // [START run_detector]

        Log.d(MainActivity.TAG, "bitmap: ${image.bitmapInternal}")
        objectDetector.process(image)
            .addOnSuccessListener { objects ->
                for(detect in objects) {
                    Log.d(MainActivity.TAG, "trackid: ${detect.trackingId} bound: ${detect.boundingBox}")
                }
                if(objects.size > 0 && !isSave) {
                    Log.d(MainActivity.TAG, "999999999999999999999999999999999999999999999999999999")
                    val saveUri = Util.saveImageToLocalStorage(context, image.bitmapInternal!!)
                    Log.d(MainActivity.TAG, "saved")
                    val executorService = Executors.newSingleThreadExecutor()
                    executorService.execute {
                        Log.d(MainActivity.TAG, "upload")
                        val responseBody = Util.uploadFile(context, saveUri)
                        val listType = object : TypeToken<List<UploadImageResult>>() {}.type
                        val json = Gson().fromJson<List<UploadImageResult>>(responseBody, listType)
                        Log.d(MainActivity.TAG, json.toString())
                        Log.d(MainActivity.TAG, responseBody)
                    }
                    isSave = true
                }
            }
            .addOnFailureListener { e ->
                Log.d(MainActivity.TAG, "error")
            }
        // [END run_detector]
    }

    override fun renderDetectMarks(
        detects: MutableList<DetectedObject>
    ) {
        for(detect in detects) {
            Log.d(MainActivity.TAG, "trackid: ${detect.trackingId} bound: ${detect.boundingBox}")
        }
    }
}