package net.taptappun.taku.kobayashi.sharphackathon2023

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.taptappun.taku.kobayashi.sharphackathon2023.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val requestPermissionNamesStash = mutableSetOf<String>()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkCanDrawOverlay {
            val permissions = mutableListOf<String>()
            checkAndRequestPermissions(permissions.toTypedArray()) {
                val mediaProjectionManager = getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjectionStartActivityForResult.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
        }

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
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