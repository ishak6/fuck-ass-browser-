package com.fuckass.browser

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fuckass.browser.engine.html.HtmlComment
import com.fuckass.browser.engine.html.HtmlElement
import com.fuckass.browser.engine.html.HtmlText
import com.fuckass.browser.engine.html.HtmlTokenizer
import com.fuckass.browser.engine.html.HtmlTreeBuilder
import com.fuckass.browser.engine.net.BrowserHttpClient
import com.fuckass.browser.engine.net.BrowserHttpRequest
import com.fuckass.browser.engine.render.CanvasRenderer
import com.fuckass.browser.engine.render.LayoutEngine
import com.fuckass.browser.engine.render.LayoutResult
import com.fuckass.browser.engine.render.LayoutViewport
import com.fuckass.browser.privacy.BrowserPrivacyPolicy
import com.fuckass.browser.search.BuiltInSearchProviders
import com.fuckass.browser.storage.EphemeralPrivateProfile
import com.fuckass.browser.storage.FileBrowserDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BrowserShell(this@MainActivity) } }
    }
}

private data class LoadedPage(
    val url: String,
    val title: String,
    val layout: LayoutResult,
    val statusCode: Int
)

@Composable
private fun BrowserShell(activity: ComponentActivity) {
    val client = remember { BrowserHttpClient(privacyPolicy = BrowserPrivacyPolicy()) }
    val renderer = remember { CanvasRenderer() }
    val privateProfile = remember { EphemeralPrivateProfile() }
    val persistentStore = remember(activity) { FileBrowserDataStore(activity.filesDir) }
    val generationCounter = remember { AtomicInteger(0) }
    val sessionLock = remember { Any() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var address by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("ddg") }
    var providerMenu by remember { mutableStateOf(false) }
    var privateMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<LoadedPage?>(null) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }

    SideEffect {
        if (privateMode) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    DisposableEffect(client, privateProfile, activity) {
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            generationCounter.incrementAndGet()
            navigationJob?.cancel()
            client.cancelActiveRequests()
            client.close()
            privateProfile.close()
        }
    }

    fun navigate(input: String) {
        val raw = input.trim()
        if (raw.isEmpty()) return
        val target = when {
            raw.startsWith("https://", true) -> raw
            raw.startsWith("http://", true) -> {
                error = "Cleartext HTTP is disabled. Use an HTTPS address."
                return
            }
            raw.contains(' ') || !raw.contains('.') ->
                BuiltInSearchProviders.find(selectedProvider)?.searchUri(raw)?.toString()
                    ?: BuiltInSearchProviders.find("ddg")!!.searchUri(raw).toString()
            else -> "https://$raw"
        }
        val requestGeneration = generationCounter.incrementAndGet()
        val privateModeAtStart = privateMode
        navigationJob?.cancel()
        client.cancelActiveRequests()
        address = target
        loading = true
        error = null
        navigationJob = scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val response = client.execute(
                        BrowserHttpRequest(
                            url = target,
                            resourceType = "document",
                            topLevelUrl = target,
                            useCache = !privateMode
                        )
                    )
                    if (response.statusCode !in 200..299) {
                        throw IllegalStateException("Server returned HTTP ${response.statusCode}")
                    }
                    val html = response.body.toString(Charsets.UTF_8)
                    val document = HtmlTreeBuilder().build(HtmlTokenizer().tokenize(html))
                    val title = document.getElementsByTagName("title").firstOrNull()?.let(::elementText)
                        ?.ifBlank { null } ?: target
                    val styles = document.getElementsByTagName("style").map(::elementText)
                    val viewportWidth = with(density) { 360.dp.toPx() }
                    val layout = LayoutEngine().layout(
                        document,
                        LayoutViewport(viewportWidth, with(density) { 800.dp.toPx() }),
                        styles
                    )
                    synchronized(sessionLock) {
                        if (generationCounter.get() == requestGeneration) {
                            if (privateModeAtStart) privateProfile.addHistory(target)
                            else runCatching { persistentStore.addHistory(target, System.currentTimeMillis()) }
                        }
                    }
                    LoadedPage(target, title, layout, response.statusCode)
                }
                if (requestGeneration == generationCounter.get()) page = loaded
            } catch (failure: Exception) {
                if (requestGeneration == generationCounter.get()) {
                    error = failure.message ?: "Page could not be loaded"
                }
            } finally {
                if (requestGeneration == generationCounter.get()) loading = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("Custom Browser", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search or enter HTTPS address") }
                )
                Button(onClick = { navigate(address) }, enabled = !loading) { Text("Go") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    OutlinedButton(onClick = { providerMenu = true }) {
                        Text(BuiltInSearchProviders.find(selectedProvider)?.displayName ?: "Search")
                    }
                    DropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                        BuiltInSearchProviders.all.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    selectedProvider = provider.id
                                    providerMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = {
                    synchronized(sessionLock) { generationCounter.incrementAndGet() }
                    navigationJob?.cancel()
                    client.cancelActiveRequests()
                    client.clearCache()
                    loading = false
                    error = null
                    privateMode = !privateMode
                    privateProfile.clear()
                    page = null
                    address = ""
                }) { Text(if (privateMode) "Private: ON" else "Private: OFF") }
                if (privateMode) {
                    OutlinedButton(onClick = { privateProfile.clear(); page = null; address = "" }) {
                        Text("Panic clear")
                    }
                }
            }
            if (privateMode) {
                Text(
                    "Private session data is memory-only and cleared when closed. Websites can still see your IP address.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (loading) {
                Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Loading with the first-party engine…")
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            page?.let { loaded ->
                Card(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        Text(loaded.title, style = MaterialTheme.typography.titleMedium)
                        Text("${loaded.statusCode} • ${loaded.url}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        val pageHeight = with(density) { loaded.layout.contentHeight.coerceAtLeast(500f).toDp() }
                        Box(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(pageHeight)) {
                                drawIntoCanvas { canvas -> renderer.draw(canvas.nativeCanvas, loaded.layout) }
                            }
                        }
                    }
                }
            }
            if (page == null && !loading) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("A first-party browser engine is being built.")
                    Text(
                        "This preview supports a limited HTML/CSS subset. Complex websites, modern JavaScript, DRM and many media sites are not supported yet.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun elementText(element: HtmlElement): String = buildString {
    fun appendChildren(node: com.fuckass.browser.engine.html.HtmlNode) {
        when (node) {
            is HtmlText -> append(node.data).append(' ')
            is HtmlComment -> Unit
            is HtmlElement -> node.children.forEach(::appendChildren)
        }
    }
    appendChildren(element)
}.replace(Regex("\\s+"), " ").trim()
