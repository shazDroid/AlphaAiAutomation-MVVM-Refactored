package adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Runs ADB commands on a given device. Commands are executed asynchronously to
 * avoid blocking the main thread. Each command may optionally specify the
 * device ID; if absent, the default device will be used.
 */
object AdbExecutor {

    /**
     * Execute an ADB command. If a [deviceId] is provided the command will be
     * prefixed with `adb -s <deviceId>`.
     */
    suspend fun runCommand(command: String, deviceId: String? = null): String {
        val finalCommand = if (deviceId != null) {
            "adb -s $deviceId $command"
        } else {
            "adb $command"
        }
        return withContext(Dispatchers.IO) {
            val process = ProcessBuilder(*finalCommand.split(" ").toTypedArray())
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        }
    }

    /**
     * Launch an application on the specified [deviceId] using its package
     * [packageName] and [activity]. Returns the output of the launch command.
     */
    suspend fun launchApp(deviceId: String, packageName: String, activity: String): String {
        return runCommand("shell am start -n $packageName/$activity", deviceId)
    }

    /**
     * List all connected devices using `adb devices` and return the serials.
     */
    suspend fun listDevices(): List<String> {
        val output = runCommand("devices", null)
        return output.lines()
            .drop(1)
            .filter { it.trim().isNotEmpty() }
            .map { it.split("\t").first() }
    }

    /**
     * Capture a screenshot of the current device screen and return the file
     * contents as a byte array.
     */
    suspend fun screencap(deviceId: String): ByteArray {
        val process = ProcessBuilder("adb", "-s", deviceId, "exec-out", "screencap", "-p")
            .redirectErrorStream(true)
            .start()
        val bytes = process.inputStream.readBytes()
        process.waitFor()
        return bytes
    }

    /**
     * Check if a package is installed on a device.
     */
    suspend fun isPackageInstalled(deviceId: String, packageName: String): Boolean {
        val output = runCommand("shell pm list packages $packageName", deviceId)
        return output.contains(packageName)
    }
}