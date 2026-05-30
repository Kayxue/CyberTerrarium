package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import job.model.Job
import job.model.script.ScriptLanguage

@Composable
fun JobScriptEditorPage(
    job: Job,
    onBack: () -> Unit,
    onSave: (String, ScriptLanguage, String) -> Unit
) {
    var language by remember(job.id) { mutableStateOf(job.script?.language ?: ScriptLanguage.JAVA) }
    var scriptContent by remember(job.id) { mutableStateOf(job.script?.content ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Script Editor", style = MaterialTheme.typography.headlineSmall)
        Text("Job: ${job.title} (${job.id})", style = MaterialTheme.typography.bodyMedium)

        SelectDropdownField(
            label = "Language",
            value = language.name,
            options = ScriptLanguage.values().map { it.name },
            onSelect = { selected ->
                val picked = runCatching { ScriptLanguage.valueOf(selected) }.getOrNull() ?: return@SelectDropdownField
                language = picked
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = scriptContent,
            onValueChange = { scriptContent = it },
            label = { Text("Script") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 280.dp)
        )

        Text(
            text = "Runtime note: script runs locally by selected language adapter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(onClick = onBack) { Text("Back") }
            AppButton(onClick = { scriptContent = defaultTemplate(language) }) { Text("Use Template") }
            AppButton(onClick = { onSave(job.id, language, scriptContent) }) { Text("Save Script") }
        }
    }
}

private fun defaultTemplate(language: ScriptLanguage): String {
    return when (language) {
        ScriptLanguage.JAVA -> """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("hello from Java job");
                }
            }
        """.trimIndent()
        ScriptLanguage.PYTHON -> """
            print("hello from Python job")
        """.trimIndent()
        ScriptLanguage.C -> """
            #include <stdio.h>

            int main(void) {
                printf("hello from C job\n");
                return 0;
            }
        """.trimIndent()
        ScriptLanguage.CPP -> """
            #include <iostream>

            int main() {
                std::cout << "hello from C++ job" << std::endl;
                return 0;
            }
        """.trimIndent()
        ScriptLanguage.LUA -> """
            print("hello from Lua job")
        """.trimIndent()
        ScriptLanguage.SHELL -> """
            echo "hello from Shell job"
        """.trimIndent()
    }
}
