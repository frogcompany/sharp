package net.taptappun.taku.kobayashi.sharphackathon2023

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.taptappun.taku.kobayashi.sharphackathon2023.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val requestPermissionNamesStash = mutableSetOf<String>()

    private lateinit var binding: ActivityMainBinding

    private var bindService: ScreenRecordService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName) {
            unbindScreenRecordService()
        }

        override fun onServiceConnected(p0: ComponentName, binder: IBinder) {
            val binder = binder as ScreenRecordService.ScreenRecordBinder
            bindService = binder.getService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaProjectionManager = getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        binding.startServiceButton.setOnClickListener {
            mediaProjectionStartActivityForResult.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
        binding.bindServiceButton.setOnClickListener {
            val intent = Intent(this, ScreenRecordService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        binding.unbindServiceButton.setOnClickListener {
            unbindScreenRecordService()
        }
        binding.stopServiceButton.setOnClickListener {
            val intent = Intent(this, ScreenRecordService::class.java)
            stopService(intent)
        }

        checkCanDrawOverlay {
            val permissions = mutableListOf<String>()
            checkAndRequestPermissions(permissions.toTypedArray()) {
            }
        }

        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute {
                    val responseBody = Util.uploadFile(applicationContext, uri)
                    val listType = object : TypeToken<List<UploadImageResult>>() {}.type
                    val json = Gson().fromJson<List<UploadImageResult>>(responseBody, listType)
                    Log.d(MainActivity.TAG, json.toString())
                    Log.d(MainActivity.TAG, responseBody)
                }
            }
        }
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        // Example of a call to a native method
        //binding.sampleText.text = stringFromJNI()
    }

    private fun unbindScreenRecordService() {
        if(bindService != null) {
            unbindService(serviceConnection)
            bindService = null
        }
    }

    private fun checkCanDrawOverlay(gratedCallback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                gratedCallback.invoke()
            } else {
                Toast.makeText(
                    this,
                    "画面上のオーバーレイする設定を有効にしてください",
                    Toast.LENGTH_SHORT
                ).show()
                // 許可されていない
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${this.packageName}")
                )

                val settingsStartActivityForResult = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(this)) {
                            gratedCallback.invoke()
                        }
                    } else {
                        gratedCallback.invoke()
                    }
                }
                // 設定画面に移行
                settingsStartActivityForResult.launch(intent)
            }
        }
    }

    // 参考:
    // https://buildersbox.corp-sansan.com/entry/2020/05/27/110000
    // https://qiita.com/yass97/items/62cccfad5190cc4d4fa6
    // onResume寄りまで定義しないとこんな感じのエラーがでてしまう
    // LifecycleOwner is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.
    private val mediaProjectionStartActivityForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenRecordService::class.java)
            intent.putExtra("code", result.resultCode) // 必要なのは結果。startActivityForResultのrequestCodeではない。
            intent.putExtra("data", result.data)
            // 画面の大きさも一緒に入れる
            val metrics = resources.displayMetrics
            intent.putExtra("height", metrics.heightPixels)
            intent.putExtra("width", metrics.widthPixels)
            intent.putExtra("dpi", metrics.densityDpi)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun checkAndRequestPermissions(
        permissionNames: Array<String>,
        permissionAllGrantedCallback: (permissionNames: Array<String>) -> Unit
    ) {
        requestPermissionNamesStash.clear()
        if (this.permissionsGranted(permissionNames)) {
            permissionAllGrantedCallback(permissionNames)
        } else {
            requestPermissionNamesStash.addAll(permissionNames)
            // permission許可要求
            ActivityCompat.requestPermissions(
                this,
                permissionNames,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun permissionsGranted(permissionNames: Array<String>): Boolean {
        return permissionNames.all { ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * A native method that is implemented by the 'sharphackathon2023' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'sharphackathon2023' library on application startup.
        init {
            System.loadLibrary("sharphackathon2023")
        }
        public const val TAG = "SharpHackathon"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}