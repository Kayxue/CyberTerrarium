package com.kay.cyberterrarium

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kay.cyberterrarium.jobmanagement.JobManagement

@Composable
@Preview
fun App() {
    MaterialTheme {
        JobManagement()
    }
}
