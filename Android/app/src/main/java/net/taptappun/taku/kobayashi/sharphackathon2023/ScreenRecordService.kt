package net.taptappun.taku.kobayashi.sharphackathon2023

import android.R.attr.bitmap
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager


// 参考: https://takusan23.github.io/Bibouroku/2020/04/06/MediaProjection/
class ScreenRecordService : Service() {
    // 画面録画で使う
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private var handleBinder = ScreenRecordBinder()
    private lateinit var overlayView: View
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        val layoutInflater = LayoutInflater.from(this)

        // レイアウトファイルからInfalteするViewを作成
        overlayView = layoutInflater.inflate(R.layout.overlay_view, null)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    inner class ScreenRecordBinder : Binder() {
        fun getService(): ScreenRecordService = this@ScreenRecordService
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(MainActivity.TAG, "onBind")
        return handleBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        Log.d(MainActivity.TAG, "onUnbind")
        // allowUnbind
        return true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 通知を出す。
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val channelID = "rec_notify"
            // 通知チャンネルが存在しないときは登録する
            if (notificationManager.getNotificationChannel(channelID) == null) {
                val channel = NotificationChannel(
                    channelID,
                    "録画サービス起動中通知",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
            Notification.Builder(applicationContext, channelID)
        } else {
            Notification.Builder(applicationContext)
        }
        // 通知作成

        val notification = notificationBuilder
            .setContentText("録画中です。")
            .setContentTitle("画面録画")
            .build()

        startForeground(1, notification)
        if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
            showWindowOverlay()
        }
        startRec(intent)
        return START_NOT_STICKY
    }

    private fun showWindowOverlay() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            // WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        val layoutParams = WindowManager.LayoutParams(
            windowType, // Overlay レイヤに表示
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // フォーカスを奪わない
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE // 画面の操作を無効化(タップを受け付けない)
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // これがないとedittextをおしてもキーボードが反応しないようだ
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // 画面外への拡張を許可
            // viewを透明にする
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, layoutParams)
    }

    // 録画開始
    private fun startRec(intent: Intent) {
        val data: Intent? = intent.getParcelableExtra("data")
        val code = intent.getIntExtra("code", Activity.RESULT_OK)
        // 画面の大きさ
        val height = intent.getIntExtra("height", 1000)
        val width = intent.getIntExtra("width", 1000)
        val dpi = intent.getIntExtra("dpi", 1000)
        if (data != null) {
            projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            // Service上でMediaProjectionを行う場合AndroidMannifest.xmlで以下の項目をいれないとエラーが発生しちゃう
            // android:foregroundServiceType="mediaProjection"
            // 参考: https://stackoverflow.com/questions/61276730/media-projections-require-a-foreground-service-of-type-serviceinfo-foreground-se

            // codeはActivity.RESULT_OKとかが入る。
            projection = projectionManager.getMediaProjection(code, data)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader.setOnImageAvailableListener(imageReaderListener, null)

            // DISPLAYMANAGERの仮想ディスプレイ表示条件
            // VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR	コンテンツをミラーリング表示する
            // VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY	独自のコンテンツを表示。ミラーリングしない
            // VIRTUAL_DISPLAY_FLAG_PRESENTATION	プレゼンテーションモード
            // VIRTUAL_DISPLAY_FLAG_PUBLIC	HDMIやWirelessディスプレイ
            // VIRTUAL_DISPLAY_FLAG_SECURE	暗号化対策が施されたセキュアなディスプレイ
            // https://techbooster.org/android/application/17026/
            virtualDisplay = projection.createVirtualDisplay(
                "recode",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                object : VirtualDisplay.Callback() {
                    override fun onPaused() {
                    }

                    override fun onResumed() {
                    }

                    override fun onStopped() {
                    }
                },
                null
            )
        }
    }

    // 録画止める
    private fun stopRec() {
        imageReader.close()
        virtualDisplay.release()
        projection.stop()
    }

    private var isSaved = false;

    private val imageReaderListener = ImageReader.OnImageAvailableListener { reader: ImageReader ->
        val image = reader.acquireLatestImage()
        if(image != null) {
            for (imagePlane in image.planes) {
                val pixelStride: Int = imagePlane.getPixelStride()
                val rowStride: Int = imagePlane.getRowStride()
                val rowPadding: Int = rowStride - pixelStride * image.width
                val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(imagePlane.buffer)
                if(!isSaved) {
                    Util.saveImageToLocalStorage(applicationContext, bitmap)
                    isSaved = true;
                }
//            Log.d(ScreenScanCommonActivity.TAG, "rowStride:${imagePlane.rowStride} pixelStride:${imagePlane.pixelStride}")
            }
            image.close()
        }
        /*
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val mBuffer: ByteArray = yuvToBuffer(
            yPlane.getBuffer(),
            uPlane.getBuffer(),
            vPlane.getBuffer(),
            yPlane.getPixelStride(),
            yPlane.getRowStride(),
            uPlane.getPixelStride(),
            uPlane.getRowStride(),
            vPlane.getPixelStride(),
            vPlane.getRowStride(),
            image.width,
            image.height
        )
        mQueue.add(MyData(mBuffer, image.timestamp, false))
        */
    }


    // Service終了と同時に録画終了
    override fun onDestroy() {
        super.onDestroy()
//        Log.d(ScreenScanCommonActivity.TAG, "onDestroy")
        if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
            windowManager.removeView(overlayView)
        }
        stopRec()
    }

    companion object {
        private const val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
    }
}