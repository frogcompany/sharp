package net.taptappun.taku.kobayashi.sharphackathon2023

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import net.taptappun.taku.kobayashi.sharphackathon2023.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
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
    }
}