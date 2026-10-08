package io.github.fate_grand_automata

import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Black-box UI verification. Interacts with visible UI using accessibility nodes;
 * no direct calls to BattleConfigListViewModel or importer.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardImportGuiTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val packageName = instrumentation.targetContext.packageName

    private fun waitText(text: String, timeout: Long = 25_000): Boolean =
        device.wait(Until.hasObject(By.textContains(text)), timeout)

    private fun clickText(text: String) {
        val found = device.wait(Until.findObject(By.textContains(text)), 25_000)
        assertTrue("Missing visible UI text: $text", found != null)
        found.click()
        device.waitForIdle()
    }

    private fun clipboard(text: String) {
        val manager = instrumentation.targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("FGA UI test", text))
    }

    private fun launch() {
        val intent = instrumentation.targetContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: error("Missing FGA launcher")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.targetContext.startActivity(intent)
        assertTrue("FGA never became foreground", device.wait(Until.hasObject(By.pkg(packageName)), 40_000))
        device.waitForIdle()
    }

    private fun ensureHome() {
        if (waitText("Battle Configs", 5_000)) return
        // The first launch presents a welcome/language/folder/battery/tutorial wizard.
        // UI automation deliberately uses the wizard rather than injecting app preferences.
        repeat(9) {
            if (waitText("Battle Configs", 1_000)) return
            if (waitText("Choose Folder", 800)) {
                clickText("Choose")
                val downloads = device.wait(Until.findObject(By.text("Download")), 15_000)
                    ?: device.wait(Until.findObject(By.text("Downloads")), 8_000)
                assertTrue("Android directory picker missing Download(s)", downloads != null)
                downloads.click()
                val use = device.wait(Until.findObject(By.textContains("USE THIS FOLDER")), 8_000)
                    ?: device.wait(Until.findObject(By.textContains("Use this folder")), 8_000)
                assertTrue("Android picker missing Use this folder", use != null)
                use.click()
                device.wait(Until.hasObject(By.textContains("Allow")), 8_000)
                device.findObject(By.textContains("Allow"))?.click()
                device.waitForIdle()
            }
            // Arrow icon has the content description in FGA onboarding.
            val arrow = device.findObject(By.descContains("Localized description"))
            if (arrow != null) arrow.click()
            else device.findObject(By.clazz("android.widget.Button"))?.click()
            device.waitForIdle()
        }
        assertTrue("Unable to finish onboarding. Inspect screenshot/UI XML.", waitText("Battle Configs", 7_000))
    }

    private fun openConfigs() {
        clickText("Battle Configs")
        assertTrue("Paste import not visible", waitText("PASTE IMPORT", 15_000))
    }

    private fun importText(text: String) {
        clipboard(text)
        clickText("PASTE IMPORT")
    }

    private fun relaunch() {
        device.pressHome()
        device.executeShellCommand("am force-stop $packageName")
        launch()
        ensureHome()
        openConfigs()
    }

    @Test
    fun clipboardImportViaVisibleUiAndPersistence() {
        try {
            launch()
            ensureHome()
            openConfigs()

            importText("""FGA1:{"autoskill_name":"GUI-Persist-Test","autoskill_cmd":"a4,#,b4,#,c4","autoskill_notes":"persist-marker"}""")
            assertTrue("Valid configuration not visible", waitText("GUI-Persist-Test", 12_000))

            importText("this is not an FGA command!!!")
            assertTrue("Invalid clipboard unexpectedly created a config", waitText("GUI-Persist-Test", 3_000))
            assertFalse("Invalid input created a config with fallback name",
                device.hasObject(By.textContains("Pasted skills")))

            importText("""FGA1:{"autoskill_name":"GUI-Second-Test","autoskill_cmd":"a4"}""")
            assertTrue("Second config missing", waitText("GUI-Second-Test", 12_000))

            relaunch()
            assertTrue("First config lost on process restart", waitText("GUI-Persist-Test", 12_000))
            assertTrue("Second config lost on process restart", waitText("GUI-Second-Test", 12_000))

            clickText("GUI-Persist-Test")
            assertTrue("Notes did not survive full config import", waitText("persist-marker", 12_000))
        } finally {
            val dir = File("/sdcard/Download/fga-gui-test")
            dir.mkdirs()
            device.takeScreenshot(File(dir, "final.png"))
            device.dumpWindowHierarchy(File(dir, "final.xml"))
        }
    }
}
