package net.taptappun.taku.kobayashi.sharphackathon2023

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.graphics.toRect
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import okio.Okio
import okio.Source
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID


class Util {
    companion object {
        fun saveImageToLocalStorage(context: Context, image: Bitmap) {
            val uuidFileName = "${UUID.randomUUID().toString()}.jpg"

            val collection = if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, uuidFileName)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                contentValues.put(MediaStore.Images.Media.IS_PENDING, true)
            }

            val contentResolver = context.contentResolver
            val contentUri = contentResolver.insert(collection, contentValues)

            //※2 ファイルを書き込む
            val openedFile = contentResolver.openFileDescriptor(contentUri!!, "w", null)
            val fileOutputStream = FileOutputStream(openedFile!!.fileDescriptor)

            contentValues.clear()
            if (Build.VERSION.SDK_INT >= 29) {
                contentResolver.update(contentUri, contentValues.apply {
                    put(MediaStore.Images.Media.IS_PENDING, false)
                }, null, null)
            } else {
                contentResolver.update(contentUri, contentValues, null, null)
            }

            val saveFile: File = File(context.getFilesDir(), uuidFileName)
            //val fileOutputStream = FileOutputStream(saveFile)
            image.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush();
            fileOutputStream.close();

            contentValues.clear()
            if (Build.VERSION.SDK_INT >= 29) {
                contentResolver.update(contentUri, contentValues.apply {
                    put(MediaStore.Images.Media.IS_PENDING, false)
                }, null, null)
            } else {
                contentResolver.update(contentUri, contentValues, null, null)
            }
        }

        fun uploadFile(vararg uploadFiles: File): String {
            val url = "https://miseai.site/api/upload"
            val media = "multipart/form-data".toMediaType()
            val boundary = System.currentTimeMillis().toString()
            try {
                val multipartBodyRequestBuilder = MultipartBody.Builder(boundary)
                multipartBodyRequestBuilder.setType(MultipartBody.FORM)
                for(uploadFile in uploadFiles) {
                    val fileName = uploadFile.name
                    multipartBodyRequestBuilder.addFormDataPart("file", fileName, uploadFile.asRequestBody(media))
                }
                val requestBody: RequestBody = multipartBodyRequestBuilder.build()
                val request: Request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                val responseBody: String = response.body?.string().orEmpty()
                return responseBody
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ""
        }

        fun uploadFile(context: Context, uri: Uri): String {
            val uploadInputStream = context.contentResolver.openInputStream(uri)
            if(uploadInputStream == null) {
                return ""
            }
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            val fileName: String = if(nameIndex != null) {
                cursor.getString(nameIndex)
            } else {
                "${UUID.randomUUID().toString()}.jpg"
            }
            val url = "https://miseai.site/api/upload"
            val media = "multipart/form-data".toMediaType()
            val boundary = System.currentTimeMillis().toString()
            val multipartBodyRequestBuilder = MultipartBody.Builder(boundary)
            multipartBodyRequestBuilder.setType(MultipartBody.FORM)
            val inputStreamRequestBody = object : RequestBody() {
                override fun contentType(): MediaType {
                    return media
                }

                override fun contentLength(): Long {
                    return uploadInputStream.available().toLong()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    val source = uploadInputStream.source()
                    sink.writeAll(source)
                }
            }
            multipartBodyRequestBuilder.addFormDataPart("fileName", fileName, inputStreamRequestBody)
            val requestBody: RequestBody = multipartBodyRequestBuilder.build()
            val request: Request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val responseBody: String = response.body?.string().orEmpty()
            return responseBody
        }

        // https://gist.github.com/kwmt/60964abd7eecbf0dc384c441abab0912
        fun rotateRect(source: RectF, degree: Float): Rect {
            val matrix = Matrix()
            matrix.setRotate(degree, source.centerX(), source.centerY())
            matrix.mapRect(source)
            return source.toRect()
        }

        fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }

        // アプリ起動中は画面が回転しないようにLockする設置
        fun screenOrientationToLock(activity: Activity) {
            if (Build.VERSION.SDK_INT < 18) {
                when (Util.getDisplayOrientation(activity)) {
                    Surface.ROTATION_0 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
        }

        private fun getDisplayOrientation(act: Activity): Int {
            val display = getDisplay(act)
            return if (display != null) {
                display.rotation
            } else {
                Surface.ROTATION_0
            }
        }

        private fun getDisplay(act: Activity): Display? {
            return if (Build.VERSION.SDK_INT < 30) {
                act.windowManager.defaultDisplay
            } else {
                act.display
            }
        }

        // ナビゲーションバーとステータスバーを隠したフルスクリーンモードにする処理
        fun requestFullScreenMode(activity: Activity) {
            if (Build.VERSION.SDK_INT < 30) {
                // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY で外部からスワイプしないとナビゲーションバーが出てこなくなる
                // View.SYSTEM_UI_FLAG_FULLSCREEN でステータスバーを非表示にする
                // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION でナビゲーションバーを非表示にする
                // View.SYSTEM_UI_FLAG_LAYOUT_STABLE でナビゲーションバーが非表示になった時にレイアウトが崩れないようにする。
                // View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN ステータスバーが非表示になった時にレイアウトが崩れないようにする。
                val decorView = activity.window.decorView
                val systemUiVisibilityFlags = if (Build.VERSION.SDK_INT < 19) {
                    (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            )
                } else {
                    (
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            )
                }

                decorView.systemUiVisibility = systemUiVisibilityFlags
            } else {
                activity.window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                activity.window.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}