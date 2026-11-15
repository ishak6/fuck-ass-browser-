package com.fuckass.browser

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : Activity() {
    private lateinit var runtime: GeckoRuntime
    private lateinit var session: GeckoSession
    private lateinit var addressBar: EditText
    private lateinit var status: TextView
    private lateinit var geckoView: GeckoView
    private var blockedCount = 0
    private var protectionEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentBlocking = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .strictSocialTrackingProtection(true)
            .build()
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlocking)
            .build()
        runtime = GeckoRuntime.create(this, runtimeSettings)
        session = GeckoSession()
        session.settings.setUseTrackingProtection(true)
        session.open(runtime)
        session.setContentBlockingDelegate(object : ContentBlocking.Delegate {
            override fun onContentBlocked(
                session: GeckoSession,
                event: ContentBlocking.BlockEvent
            ) {
                runOnUiThread {
                    blockedCount += 1
                    updateStatus()
                }
            }
        })

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 18, 24))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 10, 10, 8)
        }
        addressBar = EditText(this).apply {
            hint = "Search DuckDuckGo or enter address"
            singleLine = true
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(38, 42, 52))
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(14, 8, 14, 8)
        }
        toolbar.addView(addressBar, LinearLayout.LayoutParams(0, 48, 1f))
        toolbar.addView(makeButton("Go") { navigate(addressBar.text.toString()) })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controls.addView(makeButton("Back") { if (session.canGoBack) session.goBack() })
        controls.addView(makeButton("Forward") { if (session.canGoForward) session.goForward() })
        controls.addView(makeButton("Reload") { session.reload() })
        controls.addView(makeButton("Stop") { session.stop() })
        controls.addView(makeButton("Protection: ON") {
            protectionEnabled = !protectionEnabled
            session.settings.setUseTrackingProtection(protectionEnabled)
            (it as Button).text = if (protectionEnabled) "Protection: ON" else "Protection: OFF"
            updateStatus()
            session.reload()
        })

        status = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 11f
            setPadding(12, 4, 12, 6)
        }
        updateStatus()
        geckoView = GeckoView(this).apply { setSession(session) }
        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                navigate(addressBar.text.toString())
                true
            } else false
        }

        root.addView(toolbar)
        root.addView(controls)
        root.addView(status)
        root.addView(geckoView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        session.loadUri("https://duckduckgo.com")
    }

    private fun makeButton(label: String, action: (android.view.View) -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action(it) }
        }

    private fun updateStatus() {
        if (::status.isInitialized) {
            status.text = "Tracker protection: ${if (protectionEnabled) "ON" else "OFF"} • Blocked: $blockedCount"
        }
    }

    private fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val uri = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains('.') && !value.contains(' ') -> "https://$value"
            else -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(value, "UTF-8")
        }
        addressBar.setText(uri)
        session.loadUri(uri)
    }

    override fun onDestroy() {
        if (::session.isInitialized) session.close()
        super.onDestroy()
    }
}
