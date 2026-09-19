package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.export.ResultExporter
import com.example.core.ipsource.BundledRanges
import com.example.core.ipsource.CidrParser
import com.example.core.ipsource.GermanyNetherlandsFilter
import com.example.core.ipsource.IpSampler
import com.example.core.ipsource.ParsedRange
import com.example.core.linkparse.ShareLinkParser
import com.example.core.model.FailureClass
import com.example.core.model.IpSourceType
import com.example.core.model.IspInfo
import com.example.core.model.ProbeResult
import com.example.core.model.ScanConfig
import com.example.core.model.ScanPreset
import com.example.core.model.ScanProgress
import com.example.core.model.ShareLinkConfig
import com.example.core.netinfo.IspDetector
import com.example.core.probe.NetUtils
import com.example.core.probe.ProbeEngine
import com.example.core.probe.ThroughputTester
import com.example.core.probe.TimeoutBudget
import com.example.core.storage.PreferencesManager
import com.example.core.storage.SavedHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private class DoubleAccumulatorCompat {
    private val bits = AtomicLong(0L)
    fun add(value: Double) {
        while (true) {
            val curBits = bits.get()
            val curVal = java.lang.Double.longBitsToDouble(curBits)
            val nextVal = curVal + value
            val nextBits = java.lang.Double.doubleToRawLongBits(nextVal)
            if (bits.compareAndSet(curBits, nextBits)) break
        }
    }
    fun get(): Double = java.lang.Double.longBitsToDouble(bits.get())
}

enum class SortOption {
    SCORE,
    LATENCY,
    LOSS,
    SPEED
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val ispDetector = IspDetector(application)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val _config = MutableStateFlow(ScanConfig(preset = prefs.lastPreset))
    val config: StateFlow<ScanConfig> = _config.asStateFlow()

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val _rawResults = MutableStateFlow<List<ProbeResult>>(emptyList())
    val rawResults: StateFlow<List<ProbeResult>> = _rawResults.asStateFlow()

    val ispInfo: StateFlow<IspInfo> = ispDetector.ispInfo

    private val _currentLanguage = MutableStateFlow(prefs.language)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _parsedShareLink = MutableStateFlow<ShareLinkConfig?>(null)
    val parsedShareLink: StateFlow<ShareLinkConfig?> = _parsedShareLink.asStateFlow()

    private val _vlessExportInput = MutableStateFlow("")
    val vlessExportInput: StateFlow<String> = _vlessExportInput.asStateFlow()

    private val _isSpeedTesting = MutableStateFlow(false)
    val isSpeedTesting: StateFlow<Boolean> = _isSpeedTesting.asStateFlow()

    private val _history = MutableStateFlow<List<SavedHistoryEntry>>(prefs.getScanHistory())
    val history: StateFlow<List<SavedHistoryEntry>> = _history.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(prefs.getFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    // Filter states
    val maxLatencyFilter = MutableStateFlow(1000.0)
    val maxLossFilter = MutableStateFlow(100.0)
    val selectedColoFilter = MutableStateFlow<String?>(null)
    val sortBy = MutableStateFlow(SortOption.SCORE)

    private var scanJob: Job? = null
    private var scanStartTime = 0L

    init {
        // Apply preset
        applyPreset(prefs.lastPreset)
        // Refresh ISP info in background
        viewModelScope.launch {
            ispDetector.detectIsp()
        }
    }

    fun setLanguage(lang: String) {
        prefs.language = lang
        _currentLanguage.value = lang
    }

    fun applyPreset(preset: ScanPreset) {
        prefs.lastPreset = preset
        _config.value = _config.value.copy(
            preset = preset,
            workers = preset.workers,
            timeoutSec = preset.timeoutSec,
            sampleSize = preset.sampleSize
        )
    }

    fun updateConfig(updater: (ScanConfig) -> ScanConfig) {
        _config.value = updater(_config.value)
    }

    fun setShareLink(raw: String) {
        val parsed = ShareLinkParser.parse(raw)
        _parsedShareLink.value = parsed
        _config.value = _config.value.copy(shareLinkToTest = raw)
        if (raw.trim().startsWith("vless://", ignoreCase = true) || _vlessExportInput.value.isEmpty()) {
            _vlessExportInput.value = raw.trim()
        }
        if (parsed != null) {
            // Automatically add SNI and port if valid
            val newPorts = if (!_config.value.ports.contains(parsed.port)) {
                listOf(parsed.port) + _config.value.ports
            } else {
                _config.value.ports
            }
            val newSni = if (!_config.value.sniHosts.contains(parsed.sni) && parsed.sni.isNotEmpty()) {
                listOf(parsed.sni) + _config.value.sniHosts
            } else {
                _config.value.sniHosts
            }
            _config.value = _config.value.copy(ports = newPorts, sniHosts = newSni)
        }
    }

    fun setVlessExportInput(link: String) {
        _vlessExportInput.value = link
    }

    fun toggleFavorite(ip: String) {
        prefs.toggleFavorite(ip)
        _favorites.value = prefs.getFavorites()
    }

    fun togglePort(port: Int) {
        val current = _config.value.ports.toMutableList()
        if (current.contains(port)) {
            if (current.size > 1) current.remove(port)
        } else {
            current.add(port)
        }
        _config.value = _config.value.copy(ports = current)
    }

    fun toggleOnlyGermanyAndNetherlands() {
        _config.value = _config.value.copy(onlyGermanyAndNetherlands = !_config.value.onlyGermanyAndNetherlands)
    }

    /**
     * Initiates non-blocking scan with worker pool.
     */
    fun startScan() {
        if (_progress.value.isRunning) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val currentCfg = _config.value
            _progress.value = ScanProgress(isRunning = true)
            scanStartTime = System.currentTimeMillis()

            try {
                // 1. Gather Candidate Ranges
                val ranges = resolveRanges(currentCfg)
                if (ranges.isEmpty()) {
                    _progress.value = _progress.value.copy(isRunning = false)
                    return@launch
                }

                // 2. Generate Candidate IP List using stratified randomized sampling across all subnets
                val knownLiveV6 = prefs.getDiscoveredIpv6Prefixes()
                val sampledIps = IpSampler.sampleRandomized(
                    ranges = ranges,
                    targetCount = currentCfg.sampleSize,
                    knownLiveIpv6Prefixes = knownLiveV6
                )

                // Pair with ports and thoroughly shuffle for true randomized multi-worker probing
                val rawPairs = LinkedHashSet<Pair<String, Int>>()
                for (ip in sampledIps) {
                    for (p in currentCfg.ports) {
                        rawPairs.add(Pair(ip, p))
                    }
                }
                val targetPairs = ArrayList(rawPairs).apply { shuffle() }

                val totalCandidates = targetPairs.size
                _progress.value = _progress.value.copy(
                    totalCandidates = totalCandidates,
                    testedCount = 0,
                    healthyCount = 0
                )

                val timeoutBudget = TimeoutBudget.fromTotalSeconds(currentCfg.timeoutSec)
                val probeEngine = ProbeEngine(
                    timeoutBudget = timeoutBudget,
                    sniHosts = currentCfg.sniHosts,
                    onlyGermanyAndNetherlands = currentCfg.onlyGermanyAndNetherlands
                )

                // Cap worker concurrency to a safe maximum (32) to prevent OS thread starvation and UI freezes
                val effectiveWorkers = currentCfg.workers.coerceIn(8, 32)
                val scanDispatcher = Dispatchers.IO.limitedParallelism(effectiveWorkers)

                // Deduplicate healthy results by hostPortString
                val healthyResults = ConcurrentHashMap<String, ProbeResult>()
                val coloCounts = ConcurrentHashMap<String, Int>()
                val failureCounts = ConcurrentHashMap<FailureClass, Int>()
                val testedCounter = AtomicInteger(0)
                val healthyCounter = AtomicInteger(0)
                val healthyLatencySum = DoubleAccumulatorCompat()
                val warpDetectedCounter = AtomicInteger(0)

                // Single Queue Owner with Bounded Channel to prevent worker deadlock (§1.9)
                val candidateChannel = Channel<Pair<String, Int>>(capacity = 1000)

                // Dedicated progress & results publisher coroutine (smooth ~2.5 FPS updates prevent Compose UI freezes)
                val tickerJob = launch(Dispatchers.Default) {
                    var tickCount = 0
                    while (isActive) {
                        delay(400) // 400ms = 2.5 FPS: silky smooth without flooding the Compose UI thread
                        tickCount++
                        val tested = testedCounter.get()
                        val healthyCountVal = healthyCounter.get()
                        val elapsed = (System.currentTimeMillis() - scanStartTime) / 1000
                        val rate = if (elapsed > 0) tested.toDouble() / elapsed else 1.0
                        val remaining = (totalCandidates - tested).coerceAtLeast(0)
                        val eta = if (rate > 0) (remaining / rate).toLong() else 0L

                        val avgHealthyLatency = if (healthyCountVal > 0) {
                            healthyLatencySum.get() / healthyCountVal
                        } else 0.0

                        val proxyDetected = (healthyCountVal >= 3 && avgHealthyLatency < 5.0) ||
                                warpDetectedCounter.get() > 0

                        _progress.value = _progress.value.copy(
                            isRunning = true,
                            testedCount = tested,
                            healthyCount = healthyCountVal,
                            elapsedSeconds = elapsed,
                            etaSeconds = eta,
                            coloDistribution = coloCounts.toMap(),
                            failureStats = failureCounts.toMap(),
                            isProxyDetected = proxyDetected,
                            averageHealthyLatencyMs = avgHealthyLatency,
                            proxyWarningMessage = if (proxyDetected) "VPN / Proxy active (<5ms or WARP). Disconnect to scan accurately." else ""
                        )

                        // Rebuild & post results list every ~1.6s during scan to prevent UI recomposition churn
                        if (tickCount % 4 == 0) {
                            try {
                                val snapshot = healthyResults.values.toList().sortedBy { it.score }
                                _rawResults.value = snapshot
                            } catch (_: Exception) {}
                        }
                    }
                }

                // Launch Single Queue Producer
                val producerJob = launch {
                    try {
                        for (target in targetPairs) {
                            if (!isActive) break
                            candidateChannel.send(target)
                        }
                    } catch (_: Exception) {
                    } finally {
                        candidateChannel.close()
                    }
                }

                // Launch Worker Pool on dedicated limited dispatcher
                val workerJobs = List(effectiveWorkers) {
                    launch(scanDispatcher) {
                        for ((ip, port) in candidateChannel) {
                            if (!isActive) break
                            try {
                                val res = probeEngine.probeCandidate(ip, port)

                                val isNonDeNl = currentCfg.onlyGermanyAndNetherlands &&
                                        !GermanyNetherlandsFilter.isAllowedColo(res.colo)

                                if (res.isHealthy && !isNonDeNl) {
                                    val key = "${res.ip}:${res.port}"
                                    val isNew = (healthyResults.put(key, res) == null)
                                    if (isNew) {
                                        healthyCounter.incrementAndGet()
                                        healthyLatencySum.add(res.avgLatencyMs)
                                    }
                                    if (res.warp.equals("on", ignoreCase = true)) {
                                        warpDetectedCounter.incrementAndGet()
                                    }
                                    if (res.colo.isNotEmpty()) {
                                        coloCounts[res.colo] = (coloCounts[res.colo] ?: 0) + 1
                                    }
                                    // Cache discovered IPv6 prefix
                                    if (res.isIpv6) {
                                        val prefix = extractIpv6Prefix48(res.ip)
                                        if (prefix != null) {
                                            prefs.saveDiscoveredIpv6Prefix(prefix)
                                        }
                                    }
                                } else {
                                    val failClass = if (isNonDeNl) FailureClass.NON_DE_NL_DATACENTER else res.primaryFailure
                                    failureCounts[failClass] = (failureCounts[failClass] ?: 0) + 1
                                }
                            } catch (_: CancellationException) {
                                break
                            } catch (_: Exception) {
                            } finally {
                                testedCounter.incrementAndGet()
                            }
                        }
                    }
                }

                producerJob.join()
                workerJobs.forEach { it.join() }
                tickerJob.cancel()

                // Finalize results cleanly
                val finalHealthy = healthyResults.values.toList().sortedBy { it.score }
                val finalTested = testedCounter.get()
                val totalElapsed = (System.currentTimeMillis() - scanStartTime) / 1000
                val finalHealthyCount = healthyCounter.get()
                val finalAvgLatency = if (finalHealthyCount > 0) {
                    healthyLatencySum.get() / finalHealthyCount
                } else 0.0
                val finalProxyDetected = (finalHealthyCount >= 3 && finalAvgLatency < 5.0) ||
                        warpDetectedCounter.get() > 0

                _rawResults.value = finalHealthy
                _progress.value = _progress.value.copy(
                    isRunning = false,
                    testedCount = finalTested,
                    healthyCount = finalHealthy.size,
                    elapsedSeconds = totalElapsed,
                    etaSeconds = 0L,
                    coloDistribution = coloCounts.toMap(),
                    failureStats = failureCounts.toMap(),
                    isProxyDetected = finalProxyDetected,
                    averageHealthyLatencyMs = finalAvgLatency,
                    proxyWarningMessage = if (finalProxyDetected) "VPN / Proxy active (<5ms or WARP). Disconnect to scan accurately." else ""
                )

                // Save history
                if (finalHealthy.isNotEmpty()) {
                    try {
                        val best = finalHealthy.first()
                        val entry = SavedHistoryEntry(
                            timestamp = System.currentTimeMillis(),
                            asn = ispDetector.ispInfo.value.asn,
                            ispName = ispDetector.ispInfo.value.asOrganization,
                            port = currentCfg.ports.firstOrNull() ?: 443,
                            healthyCount = finalHealthy.size,
                            topIps = finalHealthy.take(5).map { it.hostPortString },
                            bestLatency = best.avgLatencyMs
                        )
                        prefs.saveScanHistory(entry)
                        _history.value = prefs.getScanHistory()
                    } catch (_: Exception) {}
                }
            } catch (e: CancellationException) {
                // User cancelled or stopped scan
            } catch (_: Throwable) {
            } finally {
                _progress.value = _progress.value.copy(
                    isRunning = false,
                    etaSeconds = 0L
                )
            }
        }
    }

    /**
     * Instant non-destructive cancellation (§1.10).
     * Stops workers immediately while keeping all discovered results.
     */
    fun stopScan() {
        scanJob?.cancel()
        val currentResults = _rawResults.value
        _progress.value = _progress.value.copy(
            isRunning = false,
            etaSeconds = 0L,
            healthyCount = currentResults.size
        )
    }

    /**
     * Executes dual download and upload throughput speed test on top N candidates.
     */
    fun runSpeedTestTopN(topN: Int = 10) {
        if (_isSpeedTesting.value) return
        val current = _rawResults.value
        if (current.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isSpeedTesting.value = true
            val tester = ThroughputTester(timeoutSec = 8)
            val candidates = current.take(topN)
            val updated = current.toMutableList()

            for (cand in candidates) {
                val speedResult = tester.testDualSpeed(
                    ip = cand.ip,
                    port = cand.port,
                    payloadBytes = _config.value.speedTestPayloadKb * 1024
                )

                val idx = updated.indexOfFirst { it.ip == cand.ip && it.port == cand.port }
                if (idx != -1) {
                    updated[idx] = updated[idx].copy(
                        throughputMbps = speedResult.throughputMbps,
                        uploadSpeedMbps = speedResult.uploadSpeedMbps,
                        ttfbMs = speedResult.ttfbMs
                    )
                    _rawResults.value = updated.toList()
                }
                delay(200) // Small pause between speed tests
            }
            _isSpeedTesting.value = false
        }
    }

    private suspend fun resolveRanges(config: ScanConfig): List<ParsedRange> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ParsedRange>()

        when (config.sourceType) {
            IpSourceType.CLOUDFLARE_DE_NL -> {
                // Official Germany (Frankfurt, Berlin, Munich, Dusseldorf, Hamburg, Stuttgart) and Netherlands (Amsterdam)
                if (config.includeIpv4) {
                    list.addAll(BundledRanges.CLOUDFLARE_DE_NL_IPV4.mapNotNull { CidrParser.parseLine(it) })
                }
                if (config.includeIpv6) {
                    list.addAll(BundledRanges.CLOUDFLARE_DE_NL_IPV6.mapNotNull { CidrParser.parseLine(it) })
                }
            }

            IpSourceType.CLOUDFLARE_OFFICIAL,
            IpSourceType.BUNDLED_FALLBACK -> {
                // Instant load from verified bundled ranges to start scanning immediately (<10ms)
                if (config.includeIpv4) {
                    list.addAll(BundledRanges.CLOUDFLARE_IPV4.mapNotNull { CidrParser.parseLine(it) })
                }
                if (config.includeIpv6) {
                    list.addAll(BundledRanges.CLOUDFLARE_IPV6.mapNotNull { CidrParser.parseLine(it) })
                }
            }

            IpSourceType.CUSTOM_URL -> {
                val url = config.customUrl.trim()
                if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                    try {
                        val req = Request.Builder().url(url).build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body?.string() ?: ""
                                list.addAll(CidrParser.parseAll(body))
                            }
                        }
                    } catch (_: Exception) {
                        // Fallback to bundled
                        list.addAll(BundledRanges.CLOUDFLARE_IPV4.mapNotNull { CidrParser.parseLine(it) })
                    }
                } else {
                    list.addAll(BundledRanges.CLOUDFLARE_IPV4.mapNotNull { CidrParser.parseLine(it) })
                }
            }

            IpSourceType.MANUAL -> {
                list.addAll(CidrParser.parseAll(config.manualInput))
            }

            IpSourceType.FASTLY -> {
                list.addAll(BundledRanges.FASTLY_IPV4.mapNotNull { CidrParser.parseLine(it) })
            }

            IpSourceType.GCORE -> {
                list.addAll(BundledRanges.GCORE_IPV4.mapNotNull { CidrParser.parseLine(it) })
            }
        }

        list
    }

    private fun extractIpv6Prefix48(ipv6: String): String? {
        return try {
            val parts = ipv6.split(":")
            if (parts.size >= 3) {
                "${parts[0]}:${parts[1]}:${parts[2]}::/48"
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
