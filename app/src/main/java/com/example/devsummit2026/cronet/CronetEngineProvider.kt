package com.example.devsummit2026.cronet

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import org.chromium.net.CronetEngine
import org.chromium.net.ExperimentalCronetEngine
import org.json.JSONObject
import java.io.File

/**
 * Singleton provider for [CronetEngine] configured with:
 * - QUIC enabled
 * - HTTP/2 enabled
 * - Experimental JSON options enabling QUIC connection migration:
 *   - migrate_sessions_on_network_change_v2: Enables session migration when default network switches (e.g. Wi-Fi -> Cellular)
 *   - allow_port_migration: Allows QUIC sessions to migrate across different ports/IPs without dropping connection
 */
object CronetEngineProvider {

    @Volatile
    private var cronetEngine: CronetEngine? = null

    /**
     * Experimental options JSON for Cronet QUIC Connection Migration.
     */
    fun createQuicExperimentalOptionsJson(): String {
        return JSONObject().apply {
            put(
                "QUIC",
                JSONObject().apply {
                    put("migrate_sessions_on_network_change_v2", true)
                    put("allow_port_migration", true)
                }
            )
        }.toString()
    }

    fun getEngine(context: Context): CronetEngine {
        return cronetEngine ?: synchronized(this) {
            cronetEngine ?: buildCronetEngine(context.applicationContext).also {
                cronetEngine = it
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildCronetEngine(context: Context): CronetEngine {
        try {
            CronetProviderInstaller.installProvider(context)
            Log.d("CronetEngineProvider", "Google Play Services Cronet provider installed successfully.")
        } catch (e: Exception) {
            Log.w("CronetEngineProvider", "Could not install Play Services Cronet provider: ${e.message}")
        }

        val cacheDir = File(context.cacheDir, "cronet_cache").apply { mkdirs() }

        val builder = ExperimentalCronetEngine.Builder(context)
            .enableHttp2(true)
            .enableQuic(true)
            .setStoragePath(cacheDir.absolutePath)
            .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, 20 * 1024 * 1024L) // 20 MB
            .setExperimentalOptions(createQuicExperimentalOptionsJson())

        // Register QUIC hints for prominent HTTP/3 endpoints
        builder.addQuicHint("quic.aiortc.org", 443, 443)
        builder.addQuicHint("cloudflare-quic.com", 443, 443)
        builder.addQuicHint("www.google.com", 443, 443)

        return builder.build()
    }
}
