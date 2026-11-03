package com.haroon.haiertvmonitor

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Locale
import kotlin.math.max

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var ramValue: TextView
    private lateinit var ramDetail: TextView
    private lateinit var ramBar: ProgressBar
    private lateinit var cpuValue: TextView
    private lateinit var cpuDetail: TextView
    private lateinit var cpuBar: ProgressBar
    private lateinit var storageValue: TextView
    private lateinit var storageDetail: TextView
    private lateinit var storageBar: ProgressBar
    private lateinit var updated: TextView
    private var previousCpu: Pair<Long, Long>? = null

    private val refreshTask = object : Runnable {
        override fun run() {
            refreshStats()
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.rgb(16, 21, 29))
        buildDashboard()
        refreshStats()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshTask)
        handler.post(refreshTask)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        super.onPause()
    }

    private fun buildDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(34), dp(24), dp(34), dp(20))
            setBackgroundColor(Color.rgb(16, 21, 29))
        }
        val title = text("HAIER TV MONITOR", 25f, Color.WHITE, true)
        root.addView(title)
        root.addView(text("Live device performance · refreshes every 3 seconds", 13f, 0xFF9EABB9.toInt(), false).withTop(5))

        val cards = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }
        val ram = createCard("RAM MEMORY", "Waiting for data…")
        ramValue = ram.first; ramDetail = ram.second; ramBar = ram.third
        val cpu = createCard("CPU USAGE", "Sampling CPU…")
        cpuValue = cpu.first; cpuDetail = cpu.second; cpuBar = cpu.third
        val storage = createCard("INTERNAL STORAGE", "Reading storage…")
        storageValue = storage.first; storageDetail = storage.second; storageBar = storage.third
        cards.addView(cardContainer(ram, 0), weightedParams())
        cards.addView(cardContainer(cpu, 1), weightedParams())
        cards.addView(cardContainer(storage, 2), weightedParams())
        root.addView(cards, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(24) })
        updated = text("Updating…", 12f, 0xFF8290A0.toInt(), false)
        updated.gravity = Gravity.CENTER_VERTICAL
        root.addView(updated)
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }

    private data class CardViews(val value: TextView, val detail: TextView, val bar: ProgressBar)

    private fun createCard(label: String, initial: String): CardViews {
        val value = text(initial, 24f, Color.WHITE, true)
        val detail = text("", 14f, 0xFFB7C2CE.toInt(), false)
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(0xFF62D6A5.toInt())
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0xFF35404D.toInt())
        }
        return CardViews(value, detail, bar)
    }

    private fun cardContainer(c: CardViews, index: Int): View {
        val labels = listOf("RAM MEMORY", "CPU USAGE", "INTERNAL STORAGE")
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(18))
            setBackgroundColor(0xFF202A36.toInt())
            isFocusable = true
            isFocusableInTouchMode = true
            addView(text(labels[index], 13f, 0xFF62D6A5.toInt(), true))
            addView(c.value.withTop(24))
            addView(c.bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(9)).apply { topMargin = dp(18) })
            addView(c.detail.withTop(16))
        }
    }

    private fun weightedParams() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginEnd = dp(12)
    }

    private fun refreshStats() {
        if (!::ramValue.isInitialized) return
        try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(info)
            val total = info.totalMem.coerceAtLeast(1L)
            val available = info.availMem.coerceIn(0L, total)
            val used = total - available
            val percent = ((used * 100.0) / total).toInt().coerceIn(0, 100)
            ramValue.text = "${mb(used)} / ${mb(total)} MB"
            ramDetail.text = "${mb(available)} MB available  ·  $percent% used"
            ramBar.progress = percent
        } catch (_: Exception) {
            ramValue.text = "Unavailable"
            ramDetail.text = "Android did not expose memory statistics"
        }

        val cpu = readCpuPercent()
        if (cpu == null) {
            cpuValue.text = "Unavailable"
            cpuDetail.text = "CPU counters are restricted on this firmware"
            cpuBar.progress = 0
        } else {
            cpuValue.text = "$cpu%"
            cpuDetail.text = "Overall CPU utilization · sampled"
            cpuBar.progress = cpu
        }

        try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val total = stat.totalBytes.coerceAtLeast(1L)
            val available = stat.availableBytes.coerceIn(0L, total)
            val used = total - available
            val percent = ((used * 100.0) / total).toInt().coerceIn(0, 100)
            storageValue.text = "${gb(used)} / ${gb(total)} GB"
            storageDetail.text = "${gb(available)} GB available  ·  $percent% used"
            storageBar.progress = percent
        } catch (_: Exception) {
            storageValue.text = "Unavailable"
            storageDetail.text = "Storage statistics unavailable"
        }
        updated.text = "Updated ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())}  ·  Refresh interval 3 seconds"
    }

    private fun readCpuPercent(): Int? {
        return try {
            BufferedReader(FileReader(File("/proc/stat"))).use { reader ->
                val line = reader.readLine() ?: return null
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 5 || parts[0] != "cpu") return null
                val values = parts.drop(1).map { it.toLongOrNull() ?: 0L }
                val idle = values[3] + (values.getOrNull(4) ?: 0L)
                val total = values.sum()
                val previous = previousCpu
                previousCpu = total to idle
                if (previous == null) return null
                val totalDelta = total - previous.first
                val idleDelta = idle - previous.second
                if (totalDelta <= 0L) null else ((100.0 * (totalDelta - idleDelta).coerceAtLeast(0L) / totalDelta).toInt()).coerceIn(0, 100)
            }
        } catch (_: Exception) { null }
    }

    private fun mb(bytes: Long) = (bytes / (1024.0 * 1024.0)).let { String.format(Locale.US, "%.0f", it) }
    private fun gb(bytes: Long) = String.format(Locale.US, "%.2f", bytes / (1024.0 * 1024.0 * 1024.0))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun View.withTop(margin: Int): View = apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(margin) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }

    companion object { private const val REFRESH_MS = 3000L }
}
