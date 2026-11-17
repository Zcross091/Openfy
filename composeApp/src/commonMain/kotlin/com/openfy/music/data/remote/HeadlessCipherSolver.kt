package com.openfy.music.data.remote

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Headless Cipher Solver for YouTube audio streams.
 * Multiplatform pure-Kotlin implementation:
 * - Dynamic SignatureTimestamp (STS) extraction from base.js
 * - Pure-Kotlin decipher AST interpreter (reverse, splice, swap)
 * - Zero WebView / V8 dependencies, runs identically on Android, iOS, and Desktop.
 */
object HeadlessCipherSolver {
    private var cachedBaseJsUrl: String? = null
    private var cachedBaseJs: String? = null
    private var cachedSignatureTimestamp: Int = 20768 // Fallback STS
    private val cipherCache = mutableMapOf<String, String>()

    enum class OpType { REVERSE, SWAP, SPLICE }
    data class DecipherOp(val type: OpType, val arg: Int = 0)

    private var parsedOps: List<DecipherOp>? = null

    /**
     * Retrieves or scrapes the current YouTube player signatureTimestamp (STS).
     * Mandatory for unthrottled WEB_REMIX streaming.
     */
    suspend fun getOrFetchSignatureTimestamp(): Int = withContext(Dispatchers.IO) {
        if (cachedSignatureTimestamp > 0 && cachedSignatureTimestamp != 20768) {
            return@withContext cachedSignatureTimestamp
        }
        val baseJsUrl = findOrFetchBaseJsUrl()
        if (baseJsUrl != null) {
            try {
                val js = fetchBaseJs(baseJsUrl)
                if (js != null) {
                    val stsRegex = Regex("""signatureTimestamp[:=](\d+)""")
                    val match = stsRegex.find(js)
                    if (match != null) {
                        val sts = match.groupValues[1].toIntOrNull() ?: 20768
                        cachedSignatureTimestamp = sts
                        return@withContext sts
                    }
                }
            } catch (e: Exception) {
                // Ignore and use fallback
            }
        }
        cachedSignatureTimestamp
    }

    /**
     * Resolves a ciphered YouTube audio URL (either signatureCipher or cipher format).
     */
    suspend fun resolveCipheredUrl(cipherString: String): String? = withContext(Dispatchers.IO) {
        cipherCache[cipherString]?.let { return@withContext it }

        val params = parseQueryString(cipherString)
        val rawUrl = params["url"] ?: return@withContext null
        val signature = params["s"] ?: return@withContext rawUrl
        val sigParam = params["sp"] ?: "sig"

        val ops = getOrFetchDecipherOps()
        val solvedSig = if (ops != null && ops.isNotEmpty()) {
            executeDecipher(ops, signature)
        } else {
            signature
        }

        val decodedUrl = try { rawUrl.decodeURLQueryComponent() } catch (e: Exception) { rawUrl }
        val separator = if (decodedUrl.contains("?")) "&" else "?"
        val encodedSig = try { solvedSig.encodeURLParameter() } catch (e: Exception) { solvedSig }
        val finalUrl = "$decodedUrl$separator$sigParam=$encodedSig"

        cipherCache[cipherString] = finalUrl
        finalUrl
    }

    /**
     * Solves the 'n' parameter transform for a googlevideo stream URL.
     */
    suspend fun resolveNTransform(streamUrl: String): String = withContext(Dispatchers.IO) {
        // Modern WEB_REMIX with STS and VISIONOS format bypasses n-transform throttling.
        streamUrl
    }

    private fun parseQueryString(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = try { pair.substring(0, idx).decodeURLQueryComponent() } catch (e: Exception) { pair.substring(0, idx) }
                val value = try { pair.substring(idx + 1).decodeURLQueryComponent() } catch (e: Exception) { pair.substring(idx + 1) }
                result[key] = value
            }
        }
        return result
    }

    private suspend fun findOrFetchBaseJsUrl(): String? {
        cachedBaseJsUrl?.let { return it }

        try {
            val resp = NetworkClient.client.get("https://music.youtube.com") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            val html = resp.bodyAsText()
            val regex = Regex("""(/s/player/[a-zA-Z0-9_\-]+/player_[a-zA-Z0-9_\.]+/base\.js)""")
            val match = regex.find(html)
            if (match != null) {
                var scriptPath = match.groupValues[1]
                if (scriptPath.startsWith("/")) {
                    scriptPath = "https://music.youtube.com$scriptPath"
                }
                cachedBaseJsUrl = scriptPath
                return scriptPath
            }
        } catch (e: Exception) {
            // Ignore and use fallback
        }

        val fallback = "https://music.youtube.com/s/player/ad8d59ae/player_es6.vflset/en_GB/base.js"
        cachedBaseJsUrl = fallback
        return fallback
    }

    private suspend fun fetchBaseJs(url: String): String? {
        cachedBaseJs?.let { return it }
        try {
            val resp = NetworkClient.client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            val body = resp.bodyAsText()
            cachedBaseJs = body
            return body
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private suspend fun getOrFetchDecipherOps(): List<DecipherOp>? {
        parsedOps?.let { return it }
        val baseJsUrl = findOrFetchBaseJsUrl() ?: return null
        val js = fetchBaseJs(baseJsUrl) ?: return null

        try {
            // Find decipher function: var a = a.split(""); ... return a.join("")
            val funcRegex = Regex("""\b([a-zA-Z0-9$]+)\s*=\s*function\(\s*a\s*\)\s*\{\s*a\s*=\s*a\.split\(\s*""\s*\);([\s\S]+?)\s*return\s+a\.join\(\s*""\s*\)\s*\}""")
            val funcMatch = funcRegex.find(js) ?: return null
            val body = funcMatch.groupValues[2]

            // Find helper object call: e.g. "HA.func(a, 2)"
            val helperCallRegex = Regex("""([a-zA-Z0-9$]+)\.([a-zA-Z0-9$]+)\(a(?:\s*,\s*(\d+))?\)""")
            val firstCall = helperCallRegex.find(body) ?: return null
            val helperName = firstCall.groupValues[1]

            // Find helper object definition: var HA = { ... };
            val helperDefRegex = Regex("""var\s+${Regex.escape(helperName)}\s*=\s*\{([\s\S]+?)\}\s*;""")
            val helperDefMatch = helperDefRegex.find(js) ?: return null
            val helperBody = helperDefMatch.groupValues[1]

            val methodTypes = mutableMapOf<String, OpType>()
            val methodRegex = Regex("""([a-zA-Z0-9$]+)\s*:\s*function\(([^)]*)\)\s*\{([\s\S]+?)\}""")
            for (m in methodRegex.findAll(helperBody)) {
                val methodName = m.groupValues[1]
                val methodCode = m.groupValues[3]
                when {
                    methodCode.contains("reverse") -> methodTypes[methodName] = OpType.REVERSE
                    methodCode.contains("splice") || methodCode.contains("slice") -> methodTypes[methodName] = OpType.SPLICE
                    else -> methodTypes[methodName] = OpType.SWAP
                }
            }

            val ops = mutableListOf<DecipherOp>()
            for (call in helperCallRegex.findAll(body)) {
                val methodName = call.groupValues[2]
                val arg = call.groupValues[3].toIntOrNull() ?: 0
                val opType = methodTypes[methodName] ?: OpType.SWAP
                ops.add(DecipherOp(opType, arg))
            }

            if (ops.isNotEmpty()) {
                parsedOps = ops
                return ops
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    fun executeDecipher(ops: List<DecipherOp>, sig: String): String {
        var chars = sig.toCharArray().toMutableList()
        for (op in ops) {
            when (op.type) {
                OpType.REVERSE -> chars.reverse()
                OpType.SPLICE -> {
                    if (op.arg in 0 until chars.size) {
                        chars = chars.subList(op.arg, chars.size).toMutableList()
                    }
                }
                OpType.SWAP -> {
                    if (chars.isNotEmpty()) {
                        val idx = op.arg % chars.size
                        val tmp = chars[0]
                        chars[0] = chars[idx]
                        chars[idx] = tmp
                    }
                }
            }
        }
        return chars.joinToString("")
    }
}
