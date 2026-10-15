package ch.wastedpotential.padel.phone

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.wastedpotential.padel.link.DataLayerChannel
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.storage.FileTextStore
import ch.wastedpotential.padel.sync.SampleBoards
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: BoardViewModel by viewModels {
        viewModelFactory {
            initializer {
                // Debug builds: a sample board (adb: --es sample_board match|summary) runs without the watch and saved names
                val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                val sample = intent.getStringExtra(EXTRA_SAMPLE_BOARD)?.takeIf { debuggable }
                when (sample) {
                    null -> BoardViewModel({ FileTextStore.open(File(filesDir, "names.json")) }, DataLayerChannel(applicationContext))
                    else -> BoardViewModel({ MemoryTextStore() }, null, SampleBoards.make(sample)?.encode())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Courtside display: never sleep, no system bars
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { BoardApp(vm) }
    }

    companion object {
        const val EXTRA_SAMPLE_BOARD = "sample_board"
    }
}
