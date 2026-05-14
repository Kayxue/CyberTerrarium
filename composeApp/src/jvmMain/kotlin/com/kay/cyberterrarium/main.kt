package com.kay.cyberterrarium

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import db.MigrationRunner

fun main() {
    MigrationRunner.migrate()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Cyber Terrarium",
        ) {
            App()
        }
    }
}