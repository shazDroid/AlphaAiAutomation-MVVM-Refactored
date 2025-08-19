package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.component.AlphaButton
import ui.component.AlphaInputText
import ui.component.AlphaInputTextMultiline
import ui.theme.BLUE
import ui.vm.AppViewModel

@Composable
fun AppUI() {
    val vm = remember { AppViewModel() }
    val state by vm.state.collectAsState()
    Row(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FF))
            .padding(12.dp)
    ) {
        Column(
            Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Text("Devices", color = BLUE)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlphaButton(text = "Refresh") { vm.refreshDevices() }
                Spacer(Modifier.width(8.dp))
                Text(state.selectedDevice, color = BLUE)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.height(160.dp)) {
                items(state.devices) { d ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { vm.selectDevice(d) }) { Text(d) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("App", color = BLUE)
            Spacer(Modifier.height(6.dp))
            AlphaInputText(value = state.packageName, onValueChange = { vm.setPackageName(it) }, hint = "Package name")
            Spacer(Modifier.height(6.dp))
            AlphaInputText(value = state.appActivity, onValueChange = { vm.setActivity(it) }, hint = "Main activity")
            Spacer(Modifier.height(12.dp))
            Text("Task", color = BLUE)
            Spacer(Modifier.height(6.dp))
            AlphaInputTextMultiline(value = state.nlTask, onValueChange = { vm.setTask(it) }, hint = "Describe the task…")
            Spacer(Modifier.height(8.dp))
            Row {
                AlphaButton(text = "Parse Task") { vm.parseTask() }
                Spacer(Modifier.width(8.dp))
                AlphaButton(text = "Run Agent") { vm.runAgent() }
            }
            Spacer(Modifier.height(8.dp))
            AlphaInputText(value = state.outputDir, onValueChange = { vm.setOutputDir(it) }, hint = "Output folder")
            Spacer(Modifier.height(6.dp))
            AlphaButton(text = "Generate Scripts") { vm.generateScripts() }
        }
        Column(
            Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Text("Status: " + state.status, color = BLUE)
            Spacer(Modifier.height(8.dp))
            Text("Timeline", color = BLUE)
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(0.5f)) {
                items(state.agentTimeline) { t -> Text(t.time + "  " + t.text) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Logs", color = BLUE)
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(0.5f)) {
                items(state.agentLogs) { l -> Text(l) }
            }
        }
    }
}