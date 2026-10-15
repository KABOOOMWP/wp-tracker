package ch.wastedpotential.padel.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.wastedpotential.padel.link.DataLayerChannel
import ch.wastedpotential.padel.storage.FileTextStore
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: WatchViewModel by viewModels {
        viewModelFactory {
            initializer {
                WatchViewModel(
                    openMatch = { FileTextStore.open(File(filesDir, "match.json")) },
                    openNames = { FileTextStore.open(File(filesDir, "names.json")) },
                    phone = DataLayerChannel(applicationContext),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val haptics = Haptics.of(this)
        setContent { WatchApp(vm, haptics) }
    }
}
