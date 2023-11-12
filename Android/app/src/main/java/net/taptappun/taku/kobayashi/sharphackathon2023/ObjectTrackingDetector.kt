package net.taptappun.taku.kobayashi.sharphackathon2023

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectTrackingDetector : ImageDetector<DetectedObject>() {
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
        objectDetector.process(image)
            .addOnSuccessListener { objects ->
                renderDetectMarks(objects)
            }
            .addOnFailureListener { e ->
                Log.d(MainActivity.TAG, "${e.stackTrace}")
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