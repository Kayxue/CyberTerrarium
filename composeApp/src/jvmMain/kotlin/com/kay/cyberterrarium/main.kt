package com.kay.cyberterrarium

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import db.MigrationRunner
import java.awt.Dimension

fun main() {
    MigrationRunner.migrate()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Cyber Terrarium",
            state = WindowState(
                width = 1520.dp,
                height = 820.dp
            )
        ) {
            SideEffect {
                window.minimumSize = Dimension(1480, 760)
            }
            App()
        }
    }
}
