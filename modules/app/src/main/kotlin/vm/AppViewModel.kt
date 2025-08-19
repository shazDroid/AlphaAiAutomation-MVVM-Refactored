package ui.vm

import adb.AdbExecutor
import adb.UiDumpParser
import agent.ActionPlan
import agent.AgentRunner
import agent.IntentParser
import agent.LocatorResolver
import agent.Snapshot
import agent.SnapshotStore
import appium.DriverFactory
import generator.LlmScriptGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import model.UiElement
import java.io.File
import java.time.LocalTime

data class TimelineItem(val time: String, val text: String, val kind: String)

data class UiState(
    val devices: List<String> = emptyList(),
    val selectedDevice: String = "",
    val packageName: String = "",
    val appActivity: String = "",
    val nlTask: String = "",
    val plan: ActionPlan? = null,
    val agentTimeline: List<TimelineItem> = emptyList(),
    val agentLogs: List<String> = emptyList(),
    val status: String = "",
    val isAgentRunning: Boolean = false,
    val outputDir: String = ""
)

class AppViewModel(
    private val parser: IntentParser = IntentParser(),
    private val locatorResolver: LocatorResolver = LocatorResolver()
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun refreshDevices() {
        scope.launch {
            val list = AdbExecutor.listDevices()
            _state.value = _state.value.copy(devices = list)
        }
    }

    fun selectDevice(d: String) {
        _state.value = _state.value.copy(selectedDevice = d)
    }

    fun setPackageName(p: String) {
        _state.value = _state.value.copy(packageName = p)
    }

    fun setActivity(a: String) {
        _state.value = _state.value.copy(appActivity = a)
    }

    fun setTask(t: String) {
        _state.value = _state.value.copy(nlTask = t)
    }

    fun setOutputDir(p: String) {
        _state.value = _state.value.copy(outputDir = p)
    }

    fun parseTask() {
        scope.launch {
            val t = _state.value.nlTask
            if (t.isBlank()) return@launch
            val parsed = parser.parse(t)
            val plan = parsed.copy(steps = parsed.steps.mapIndexed { i, s -> s.copy(index = i + 1) })
            _state.value = _state.value.copy(plan = plan, status = "Task parsed")
        }
    }

    fun runAgent() {
        scope.launch {
            val s = _state.value
            if (s.selectedDevice.isBlank()) return@launch
            if (s.packageName.isBlank() || s.appActivity.isBlank()) return@launch
            val driver = DriverFactory.create(s.selectedDevice, s.packageName, s.appActivity)
            val runner = AgentRunner(driver, LocatorResolver(), SnapshotStore(driver, File(System.getProperty("java.io.tmpdir"))))
            _state.value = _state.value.copy(isAgentRunning = true, status = "Running")
            val plan = s.plan ?: return@launch
            runner.run(
                plan,
                onStep = { idx, step ->
                    val tl = _state.value.agentTimeline + TimelineItem(LocalTime.now().toString().substring(0, 5), "Step $idx: ${'$'}{step.description}", "OK")
                    _state.value = _state.value.copy(agentTimeline = tl)
                },
                onLog = { msg ->
                    _state.value = _state.value.copy(agentLogs = _state.value.agentLogs + msg)
                },
                onStatus = { st ->
                    _state.value = _state.value.copy(status = st)
                }
            )
            driver.quit()
            _state.value = _state.value.copy(isAgentRunning = false, status = "Done")
        }
    }

    fun generateScripts() {
        scope.launch {
            val s = _state.value
            val plan = s.plan ?: return@launch
            val output = s.outputDir
            if (output.isBlank()) {
                _state.value = _state.value.copy(status = "Specify outputDir")
                return@launch
            }
            val dump = if (s.isAgentRunning) null else {
                try {
                    UiDumpParser.getUiDumpXml(s.selectedDevice)
                } catch (e: Exception) {
                    null
                }
            }
            val elements: List<UiElement> = if (dump != null) UiDumpParser.parseUiDump(dump) else emptyList()
            val gen = LlmScriptGenerator(ui.OllamaClient, File(output))
            gen.generate(plan, emptyList<Snapshot>())
            _state.value = _state.value.copy(status = "Scripts generated")
        }
    }
}