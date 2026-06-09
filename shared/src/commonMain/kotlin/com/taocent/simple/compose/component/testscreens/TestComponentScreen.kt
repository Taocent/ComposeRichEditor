package com.taocent.simple.compose.component.testscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.DetailScreen

@Composable
fun TestComponentScreen(onBack: () -> Unit, showTopBar: Boolean = true) {
    var counter by remember { mutableStateOf(0) }

    if (showTopBar) {
        DetailScreen(title = "示例组件", onBack = onBack) { padding ->
            ComponentTestContent(counter = counter, onIncrement = { counter++ }, modifier = Modifier.padding(padding))
        }
    } else {
        ComponentTestContent(counter = counter, onIncrement = { counter++ }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ComponentTestContent(
    counter: Int,
    onIncrement: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "示例组件测试界面",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "当前计数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$counter",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onIncrement) {
                Text("计数 +1")
            }
        }
    }
}
