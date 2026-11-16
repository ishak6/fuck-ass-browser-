package com.fuckass.browser

import android.os.Bundle
import android.app.AlertDialog
import android.content.Intent
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.key
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
import com.fuckass.browser.engine.net.CookieAccessPolicy
import com.fuckass.browser.engine.net.CookieMode
import com.fuckass.browser.engine.net.BrowserHttpRequest
import com.fuckass.browser.engine.render.CanvasRenderer
import com.fuckass.browser.engine.render.LayoutEngine
import com.fuckass.browser.engine.render.LayoutResult
import com.fuckass.browser.engine.render.LayoutViewport
import com.fuckass.browser.privacy.BrowserPrivacyPolicy
import com.fuckass.browser.privacy.SiteOrigin
import com.fuckass.browser.privacy.SitePermission
import com.fuckass.browser.privacy.SitePermissionDecision
import com.fuckass.browser.privacy.SitePermissionManager
import com.fuckass.browser.privacy.PrivateSiteRules
import com.fuckass.browser.search.BuiltInSearchProviders
import com.fuckass.browser.search.NativeSearchClient
import com.fuckass.browser.search.NativeSearchResult
import com.fuckass.browser.search.InternetArchiveVideoClient
import com.fuckass.browser.search.ArchiveVideoResult
import com.fuckass.browser.storage.EphemeralPrivateProfile
import com.fuckass.browser.storage.EncryptedSitePermissionStore
import com.fuckass.browser.storage.FileBrowserDataStore
import com.fuckass.browser.storage.FilePrivateSiteRulesStore
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
    val normalCookies = remember { CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER) }
    val privateCookies = remember { CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER) }
    val normalCookiePolicy = remember { CookieAccessPolicy() }
    val privateCookiePolicy = remember { CookieAccessPolicy() }
    val normalClient = remember { BrowserHttpClient(privacyPolicy = BrowserPrivacyPolicy(), cookieManager = normalCookies, cookieAccessPolicy = normalCookiePolicy) }
    val privateClient = remember { BrowserHttpClient(privacyPolicy = BrowserPrivacyPolicy(), cookieManager = privateCookies, cookieAccessPolicy = privateCookiePolicy) }
    val renderer = remember { CanvasRenderer() }
    val privateProfile = remember { EphemeralPrivateProfile() }
    val persistentStore = remember(activity) { FileBrowserDataStore(activity.filesDir) }
    val privateSiteStore = remember(activity) { FilePrivateSiteRulesStore(activity.filesDir) }
    val permissionStoreResult = remember(activity) { runCatching { EncryptedSitePermissionStore(activity.filesDir) } }
    val sitePermissionManager = remember(permissionStoreResult) {
        permissionStoreResult.getOrNull()?.let(::SitePermissionManager)
    }
    val generationCounter = remember { AtomicInteger(0) }
    val navigationHistory = remember { BrowserNavigationHistory() }
    val sessionLock = remember { Any() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var address by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("wikipedia") }
    var providerMenu by remember { mutableStateOf(false) }
    var bookmarkMenu by remember { mutableStateOf(false) }
    var bookmarkItems by remember { mutableStateOf(persistentStore.bookmarks()) }
    var historyMenu by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(persistentStore.history().asReversed().map { it.first }) }
    var privateMode by remember { mutableStateOf(false) }
    var permissionPanel by remember { mutableStateOf(false) }
    var permissionRevision by remember { mutableStateOf(0) }
    var dataControlsPanel by remember { mutableStateOf(false) }
    var dataControlsRevision by remember { mutableStateOf(0) }
    var cookieMode by remember { mutableStateOf(CookieMode.ALLOW_ALL) }
    var privateSiteHosts by remember { mutableStateOf(privateSiteStore.hosts()) }
    val privateSiteRules = remember(privateSiteHosts) { PrivateSiteRules(privateSiteHosts) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<LoadedPage?>(null) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    var historySnapshot by remember { mutableStateOf(navigationHistory.snapshot()) }
    var nativeResults by remember { mutableStateOf<List<NativeSearchResult>?>(null) }
    var nativeSearchQuery by remember { mutableStateOf("") }
    var videoResults by remember { mutableStateOf<List<ArchiveVideoResult>?>(null) }
    var videoSearchQuery by remember { mutableStateOf("") }

    SideEffect {
        if (privateMode) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    DisposableEffect(normalClient, privateClient, privateProfile, activity) {
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            generationCounter.incrementAndGet()
            navigationJob?.cancel()
            normalClient.cancelActiveRequests()
            privateClient.cancelActiveRequests()
            normalClient.close()
            privateClient.close()
            privateClient.clearCookies()
            privateProfile.close()
        }
    }

    fun confirmAndPlayVideo(url: String) {
        AlertDialog.Builder(activity)
            .setTitle("Play archive video?")
            .setMessage("Playback uses Android's platform player. Browser ad blocking, cookie rules and private-session network isolation do not apply to this media request. Continue only if you trust the source.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Play") { _, _ ->
                activity.startActivity(
                    Intent(activity, NativeVideoPlayerActivity::class.java)
                        .putExtra(NativeVideoPlayerActivity.EXTRA_URL, url)
                        .putExtra(NativeVideoPlayerActivity.EXTRA_PRIVATE, privateMode)
                )
            }
            .show()
    }

    fun runVideoSearch(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return
        val requestGeneration = generationCounter.incrementAndGet()
        navigationJob?.cancel()
        normalClient.cancelActiveRequests()
        privateClient.cancelActiveRequests()
        val requestClient = if (privateMode) privateClient else normalClient
        address = cleanQuery
        videoSearchQuery = cleanQuery
        videoResults = null
        nativeResults = null
        page = null
        loading = true
        error = null
        navigationJob = scope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    InternetArchiveVideoClient().search(requestClient, cleanQuery)
                }
                if (generationCounter.get() == requestGeneration) {
                    videoResults = results
                    if (results.isEmpty()) error = "No public archive videos found for this query."
                }
            } catch (failure: Exception) {
                if (generationCounter.get() == requestGeneration) {
                    error = failure.message ?: "Video search failed"
                }
            } finally {
                if (generationCounter.get() == requestGeneration) loading = false
            }
        }
    }

    fun resolveAndPlayArchiveVideo(identifier: String) {
        val requestGeneration = generationCounter.incrementAndGet()
        navigationJob?.cancel()
        val requestClient = if (privateMode) privateClient else normalClient
        loading = true
        error = null
        navigationJob = scope.launch {
            try {
                val mediaUrl = withContext(Dispatchers.IO) {
                    InternetArchiveVideoClient().resolvePlayableMp4(requestClient, identifier)
                }
                if (generationCounter.get() == requestGeneration) confirmAndPlayVideo(mediaUrl)
            } catch (failure: Exception) {
                if (generationCounter.get() == requestGeneration) {
                    error = failure.message ?: "No playable video file found"
                }
            } finally {
                if (generationCounter.get() == requestGeneration) loading = false
            }
        }
    }

    fun runNativeSearch(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return
        val requestGeneration = generationCounter.incrementAndGet()
        navigationJob?.cancel()
        normalClient.cancelActiveRequests()
        privateClient.cancelActiveRequests()
        val requestClient = if (privateMode) privateClient else normalClient
        address = cleanQuery
        nativeSearchQuery = cleanQuery
        nativeResults = null
        page = null
        loading = true
        error = null
        navigationJob = scope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    NativeSearchClient().search(requestClient, cleanQuery)
                }
                if (generationCounter.get() == requestGeneration) {
                    nativeResults = results
                    if (results.isEmpty()) error = "No Wikipedia results found. Try a different query."
                }
            } catch (failure: Exception) {
                if (generationCounter.get() == requestGeneration) {
                    error = failure.message ?: "Native search failed"
                }
            } finally {
                if (generationCounter.get() == requestGeneration) loading = false
            }
        }
    }

    fun navigate(input: String, recordHistory: Boolean = true, onHistorySuccess: (() -> Unit)? = null) {
        val raw = input.trim()
        if (raw.isEmpty()) return
        if (!raw.startsWith("https://", true) && !raw.startsWith("http://", true) &&
            (raw.contains(' ') || !raw.contains('.')) && selectedProvider == "wikipedia"
        ) {
            runNativeSearch(raw)
            return
        }
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
        nativeResults = null
        videoResults = null
        if (isDirectVideoUrl(target)) {
            confirmAndPlayVideo(target)
            return
        }
        val autoPrivate = !privateMode && requiresPrivateSession(false, target, privateSiteRules)
        if (autoPrivate && !privateMode) {
            synchronized(sessionLock) { generationCounter.incrementAndGet() }
            navigationJob?.cancel()
            normalClient.cancelActiveRequests()
            normalClient.clearCache()
            privateClient.cancelActiveRequests()
            privateClient.clearCache()
            privateClient.clearCookies()
            privateProfile.clear()
            privateMode = true
            historyItems = emptyList()
            navigationHistory.reset()
            historySnapshot = navigationHistory.snapshot()
            page = null
        }
        val requestGeneration = generationCounter.incrementAndGet()
        val privateModeAtStart = requiresPrivateSession(privateMode, target, privateSiteRules)
        val requestClient = if (privateModeAtStart) privateClient else normalClient
        navigationJob?.cancel()
        normalClient.cancelActiveRequests()
        privateClient.cancelActiveRequests()
        address = target
        loading = true
        error = null
        navigationJob = scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val response = requestClient.execute(
                        BrowserHttpRequest(
                            url = target,
                            resourceType = "document",
                            topLevelUrl = target,
                            useCache = !privateModeAtStart
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
                if (requestGeneration == generationCounter.get()) {
                    if (onHistorySuccess != null) onHistorySuccess()
                    else if (recordHistory) navigationHistory.record(target)
                    historySnapshot = navigationHistory.snapshot()
                    historyItems = if (privateModeAtStart) privateProfile.historyEntries()
                        else persistentStore.history().map { it.first }
                    page = loaded
                }
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
            if (selectedProvider == "wikipedia") {
                Text("Native search sends your query to Wikipedia. Other providers open their result pages.", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
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
                OutlinedButton(onClick = { runVideoSearch(address) }, enabled = address.isNotBlank() && !loading) {
                    Text("Videos")
                }
                OutlinedButton(onClick = {
                    synchronized(sessionLock) { generationCounter.incrementAndGet() }
                    navigationJob?.cancel()
                    normalClient.cancelActiveRequests()
                    privateClient.cancelActiveRequests()
                    normalClient.clearCache()
                    privateClient.clearCache()
                    privateClient.clearCookies()
                    loading = false
                    error = null
                    val enteringPrivate = !privateMode
                    if (enteringPrivate) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    privateMode = enteringPrivate
                    historyItems = if (enteringPrivate) emptyList() else persistentStore.history().map { it.first }
                    privateProfile.clear()
                    navigationHistory.reset()
                    historySnapshot = navigationHistory.snapshot()
                    page = null
                    address = ""
                }) { Text(if (privateMode) "Private: ON" else "Private: OFF") }
                OutlinedButton(onClick = { permissionPanel = !permissionPanel }) {
                    Text(if (permissionPanel) "Hide permissions" else "Site permissions")
                }
                Box {
                    OutlinedButton(onClick = { historyMenu = true }) { Text("History") }
                    DropdownMenu(expanded = historyMenu, onDismissRequest = { historyMenu = false }) {
                        if (historyItems.isEmpty()) {
                            DropdownMenuItem(text = { Text("No browsing history") }, onClick = { historyMenu = false })
                        } else {
                            historyItems.asReversed().forEach { url ->
                                DropdownMenuItem(
                                    text = { Text(url, maxLines = 1) },
                                    onClick = {
                                        historyMenu = false
                                        navigate(url)
                                    }
                                )
                            }
                        }
                    }
                }
                Box {
                    OutlinedButton(onClick = { bookmarkMenu = true }) { Text("Bookmarks") }
                    DropdownMenu(expanded = bookmarkMenu, onDismissRequest = { bookmarkMenu = false }) {
                        if (bookmarkItems.isEmpty()) {
                            DropdownMenuItem(text = { Text("No saved bookmarks") }, onClick = { bookmarkMenu = false })
                        } else {
                            bookmarkItems.forEach { (url, title) ->
                                DropdownMenuItem(
                                    text = { Text(title.ifBlank { url }, maxLines = 1) },
                                    onClick = {
                                        bookmarkMenu = false
                                        navigate(url)
                                    }
                                )
                            }
                        }
                    }
                }
                if (privateMode) {
                    OutlinedButton(onClick = {
                        synchronized(sessionLock) { generationCounter.incrementAndGet() }
                        navigationJob?.cancel()
                        normalClient.cancelActiveRequests()
                        privateClient.cancelActiveRequests()
                        normalClient.clearCache()
                        privateClient.clearCache()
                        privateClient.clearCookies()
                        privateProfile.clear()
                        historyItems = emptyList()
                        navigationHistory.reset()
                        historySnapshot = navigationHistory.snapshot()
                        loading = false
                        error = null
                        page = null
                        address = ""
                    }) {
                        Text("Panic clear")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        navigationHistory.previousUrl()?.let { target ->
                            navigate(target, recordHistory = false) { navigationHistory.goBack() }
                        }
                    },
                    enabled = historySnapshot.canGoBack && !loading
                ) { Text("Back") }
                OutlinedButton(
                    onClick = {
                        navigationHistory.nextUrl()?.let { target ->
                            navigate(target, recordHistory = false) { navigationHistory.goForward() }
                        }
                    },
                    enabled = historySnapshot.canGoForward && !loading
                ) { Text("Forward") }
                OutlinedButton(
                    onClick = { if (address.isNotBlank()) navigate(address, recordHistory = false) },
                    enabled = address.isNotBlank() && !loading
                ) { Text("Refresh") }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedButton(onClick = { dataControlsPanel = !dataControlsPanel }) {
                    Text(if (dataControlsPanel) "Hide data controls" else "Privacy & data")
                }
            }
            if (videoResults != null) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
                        Text("Video results", style = MaterialTheme.typography.titleLarge)
                        Text("Internet Archive public video catalogue for: $videoSearchQuery", style = MaterialTheme.typography.bodySmall)
                        Text("Only publicly listed MP4 files are offered. Playback uses Android's platform decoder.", style = MaterialTheme.typography.bodySmall)
                        videoResults.orEmpty().forEachIndexed { index, result ->
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Text(result.title, style = MaterialTheme.typography.titleMedium)
                                if (result.description.isNotBlank()) Text(result.description, style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { resolveAndPlayArchiveVideo(result.identifier) }, enabled = !loading) {
                                        Text("Play")
                                    }
                                    OutlinedButton(onClick = { navigate("https://archive.org/details/${result.identifier}") }) {
                                        Text("Details")
                                    }
                                }
                            }
                            if (index != videoResults.orEmpty().lastIndex) {
                                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
            if (nativeResults != null) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {
                        Text("Search results", style = MaterialTheme.typography.titleLarge)
                        Text("Wikipedia native results for: $nativeSearchQuery", style = MaterialTheme.typography.bodySmall)
                        Text("Powered by Wikimedia's public search API. Other providers open their result pages.", style = MaterialTheme.typography.bodySmall)
                        nativeResults.orEmpty().forEachIndexed { index, result ->
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Text(result.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                if (result.snippet.isNotBlank()) Text(result.snippet, style = MaterialTheme.typography.bodyMedium)
                                Text(result.url, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { navigate(result.url) }) { Text("Open result") }
                            }
                            if (index != nativeResults.orEmpty().lastIndex) {
                                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
            if (permissionPanel) {
                val permissionUrl = page?.url ?: address
                val validPermissionOrigin = SiteOrigin.fromUrl(permissionUrl) != null
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("Site permission controls", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (validPermissionOrigin) "Origin: ${SiteOrigin.fromUrl(permissionUrl)}"
                            else "Open an HTTPS site to edit its permissions.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        permissionStoreResult.exceptionOrNull()?.let {
                            Text("Encrypted permission storage unavailable: ${it.message ?: "initialization failed"}", color = MaterialTheme.colorScheme.error)
                        }
                        if (sitePermissionManager != null && validPermissionOrigin) {
                            key(permissionRevision) {
                                SitePermission.entries.forEach { permission ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        Text(permission.name.replace('_', ' '), style = MaterialTheme.typography.labelLarge)
                                        Text("Current: ${sitePermissionManager.decision(permissionUrl, permission)}", style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            SitePermissionDecision.entries.forEach { decision ->
                                                OutlinedButton(
                                                    onClick = {
                                                        runCatching { sitePermissionManager.setDecision(permissionUrl, permission, decision) }
                                                            .onSuccess { permissionRevision++ }
                                                            .onFailure { failure -> error = failure.message ?: "Could not save site permission" }
                                                    },
                                                    enabled = !privateMode
                                                ) { Text(when (decision) {
                                                        SitePermissionDecision.ASK -> "Ask"
                                                        SitePermissionDecision.ALLOW -> "Allow"
                                                        SitePermissionDecision.BLOCK -> "Block"
                                                    }) }
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                "Camera, microphone and location also require Android runtime permission. Site ALLOW does not grant Android access.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        if (privateMode) Text("Site permission changes are disabled in private mode.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (dataControlsPanel) {
                val currentSiteUrl = page?.url ?: address
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("Privacy & data controls", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "The controls below manage the normal session. Private-session cookies and cache are cleared when private mode exits.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Cookie policy: ${cookieMode.name.replace('_', ' ')}", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CookieMode.entries.forEach { mode ->
                                OutlinedButton(onClick = {
                                    cookieMode = mode
                                    normalCookiePolicy.setMode(mode)
                                    privateCookiePolicy.setMode(mode)
                                }) {
                                    Text(when (mode) {
                                        CookieMode.ALLOW_ALL -> "All"
                                        CookieMode.SAME_ORIGIN_ONLY -> "Same origin"
                                        CookieMode.BLOCK_ALL -> "Block"
                                    })
                                }
                            }
                        }
                        Text(
                            "Same origin is stricter than same-site: scheme, host and port must all match. This is not a public-suffix-list implementation.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            if (currentSiteUrl.startsWith("https://", true)) "Current page: $currentSiteUrl"
                            else "Open an HTTPS page to clear that site's cookies.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val removed = normalClient.clearCookiesFor(currentSiteUrl)
                                    dataControlsRevision++
                                    error = "Removed $removed cookie(s) for this site"
                                },
                                enabled = !privateMode && currentSiteUrl.startsWith("https://", true)
                            ) { Text("Clear site cookies") }
                            OutlinedButton(
                                onClick = {
                                    normalClient.clearCookies()
                                    dataControlsRevision++
                                    error = "All normal-session cookies cleared"
                                },
                                enabled = !privateMode
                            ) { Text("Clear all cookies") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    normalClient.clearCache()
                                    dataControlsRevision++
                                    error = "Normal-session cache cleared"
                                },
                                enabled = !privateMode
                            ) { Text("Clear cache") }
                            OutlinedButton(
                                onClick = {
                                    persistentStore.clearHistory()
                                    historyItems = emptyList()
                                    dataControlsRevision++
                                    error = "Browsing history cleared"
                                },
                                enabled = !privateMode
                            ) { Text("Clear history") }
                        }
                        Text(
                            "Cookie-backed requests bypass the URL-only HTTP cache to avoid cross-session data leakage. Clear cache empties any in-memory entries; it does not enable persistent caching.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Private-session data is cleared when private mode exits or Panic clear is used.", style = MaterialTheme.typography.bodySmall)
                        if (dataControlsRevision > 0) Text("Data controls updated.", style = MaterialTheme.typography.bodySmall)
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
                        val loadedHost = runCatching { URI(loaded.url).host }.getOrNull()
                        val alwaysPrivate = privateSiteRules.matches(loaded.url)
                        val matchedPrivateRule = loadedHost?.let { host ->
                            privateSiteHosts.firstOrNull { host == it || host.endsWith(".$it") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(loaded.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    if (loadedHost != null) {
                                        runCatching {
                                            if (alwaysPrivate && matchedPrivateRule != null) {
                                                privateSiteStore.remove(matchedPrivateRule)
                                            } else privateSiteStore.add(loadedHost)
                                        }.onSuccess { updatedHosts ->
                                            privateSiteHosts = updatedHosts
                                            if (!alwaysPrivate && !privateMode) {
                                                privateMode = true
                                                privateProfile.clear()
                                                historyItems = emptyList()
                                                navigationHistory.reset()
                                                historySnapshot = navigationHistory.snapshot()
                                                page = null
                                                address = ""
                                                navigate(loaded.url)
                                            }
                                        }.onFailure { failure ->
                                            error = failure.message ?: "Could not update private-site rules"
                                        }
                                    }
                                },
                                enabled = loadedHost != null
                            ) { Text(if (alwaysPrivate) "Always private ✓" else "Always private") }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        persistentStore.saveBookmark(loaded.url, loaded.title)
                                        bookmarkItems = persistentStore.bookmarks()
                                    }.onFailure { failure ->
                                        error = failure.message ?: "Could not save bookmark"
                                    }
                                },
                                enabled = !privateMode
                            ) { Text("Bookmark") }
                        }
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
            if (page == null && nativeResults == null && videoResults == null && !loading) {
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

private fun isDirectVideoUrl(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null) return false
    return uri.path.orEmpty().substringAfterLast('.', "").lowercase() in setOf("mp4", "m4v", "3gp", "webm")
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


/** Platform-native video playback for an explicitly supplied HTTPS media URL. */
class NativeVideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val uri = runCatching { URI(rawUrl) }.getOrNull()
        if (uri == null || !uri.scheme.equals("https", true) || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || !isDirectVideoUrl(rawUrl)
        ) {
            finish()
            return
        }
        if (intent.getBooleanExtra(EXTRA_PRIVATE, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        val video = android.widget.VideoView(this)
        val controls = android.widget.MediaController(this)
        controls.setAnchorView(video)
        video.setMediaController(controls)
        setContentView(video)
        video.setOnErrorListener { _, what, extra ->
            android.widget.Toast.makeText(this, "Video playback failed ($what/$extra). Format or server may not be supported.", android.widget.Toast.LENGTH_LONG).show()
            finish()
            true
        }
        video.setVideoURI(android.net.Uri.parse(rawUrl))
        video.setOnPreparedListener { player -> player.start() }
    }

    companion object {
        const val EXTRA_URL = "com.fuckass.browser.extra.VIDEO_URL"
        const val EXTRA_PRIVATE = "com.fuckass.browser.extra.VIDEO_PRIVATE"
    }
}
