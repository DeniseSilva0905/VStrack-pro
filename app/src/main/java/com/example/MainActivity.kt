package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.navigation.VSTracksAppScaffold
import com.example.ui.theme.VSTracksProTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake for live stage operation (WakeLock)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            VSTracksProTheme {
                VSTracksAppScaffold(viewModel = viewModel)
            }
        }
    }

    /**
     * Hardware Pedal Footswitch and Keyboard Shortcut Interceptor
     * Handles iRig BlueTurn, Donner Wireless, AirTurn, CUVAVE, and physical keyboards
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                viewModel.onHardwareKeyEvent("ESPAÇO", "Disparo / Pausa Playback")
                viewModel.requestPlayPause()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_DOWN -> {
                viewModel.onHardwareKeyEvent("SETA DIREITA / PG DN", "Avançar Próxima Música")
                viewModel.nextSong()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_PAGE_UP -> {
                viewModel.onHardwareKeyEvent("SETA ESQUERDA / PG UP", "Retornar Música Anterior")
                viewModel.previousSong()
                return true
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                viewModel.onHardwareKeyEvent("ESC", "PÂNICO ACIONADO (Fade 700ms)")
                viewModel.triggerPanic()
                return true
            }

        }
        return super.onKeyDown(keyCode, event)
    }
}
