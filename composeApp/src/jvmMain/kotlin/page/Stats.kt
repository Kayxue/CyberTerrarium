package page

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Stats(){
    Column {
        Text("Stats page", modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}