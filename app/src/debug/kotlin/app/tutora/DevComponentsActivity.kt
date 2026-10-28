package app.tutora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.tutora.designsystem.ComponentGallery

/**
 * The developer screen with every component (AC-002-15). It exists only in debug builds:
 * `adb shell am start -n app.tutora.app/app.tutora.DevComponentsActivity`.
 */
class DevComponentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ComponentGallery() }
    }
}
