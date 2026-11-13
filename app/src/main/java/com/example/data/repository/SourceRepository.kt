package com.example.data.repository

import com.example.core.security.SecureSourceStorage
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.NextVuDatabase
import com.example.data.remote.ApiResult
import com.example.data.remote.XtreamApiService
import com.example.data.remote.XtreamConnectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SourceRepository(
    private val secureStorage: SecureSourceStorage,
    private val apiService: XtreamApiService,
    private val database: NextVuDatabase
) {
    private val _sources = MutableStateFlow<List<XtreamSourceProfile>>(emptyList())
    val sources: StateFlow<List<XtreamSourceProfile>> = _sources.asStateFlow()

    private val _activeSource = MutableStateFlow<XtreamSourceProfile?>(null)
    val activeSource: StateFlow<XtreamSourceProfile?> = _activeSource.asStateFlow()

    init {
        refreshFromStorage()
    }

    fun refreshFromStorage() {
        val loaded = secureStorage.loadSources()
        _sources.value = loaded
        _activeSource.value = loaded.firstOrNull { it.isActive }
    }

    fun normalizeServerUrl(serverUrl: String): String {
        return apiService.normalizeServerUrl(serverUrl) ?: serverUrl.trim().trimEnd('/')
    }

    fun isDuplicateSource(serverUrl: String, username: String): Boolean {
        val normalizedUrl = normalizeServerUrl(serverUrl).lowercase()
        val normalizedUser = username.trim()
        return _sources.value.any { existing ->
            normalizeServerUrl(existing.serverUrl).lowercase() == normalizedUrl &&
                existing.username.trim() == normalizedUser
        }
    }

    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): ApiResult<XtreamConnectionResult> {
        return apiService.testAndAuthenticate(serverUrl, username, password)
    }

    fun addImportedSource(
        sourceName: String,
        serverUrl: String,
        username: String,
        password: String,
        expiryDate: String,
        connectionResult: XtreamConnectionResult
    ): XtreamSourceProfile {
        val normalizedUrl = normalizeServerUrl(serverUrl)
        val current = _sources.value.toMutableList()
        val profile = XtreamSourceProfile(
            id = UUID.randomUUID().toString(),
            sourceName = sourceName.trim().ifBlank { "Xtream Source" },
            serverUrl = normalizedUrl,
            username = username.trim(),
            password = password,
            isActive = false,
            lastVerifiedEpochMs = System.currentTimeMillis(),
            accountStatus = connectionResult.status,
            expDate = connectionResult.expDate?.takeIf { it.isNotBlank() } ?: expiryDate.takeIf { it.isNotBlank() },
            activeConnections = connectionResult.activeConnections,
            maxConnections = connectionResult.maxConnections
        )
        current.add(profile)
        secureStorage.saveSources(current)
        _sources.value = current
        _activeSource.value = current.firstOrNull { it.isActive }
        return profile
    }

    fun addOrUpdateSource(
        existingId: String?,
        sourceName: String,
        serverUrl: String,
        username: String,
        password: String,
        connectionResult: XtreamConnectionResult,
        makeActive: Boolean
    ): XtreamSourceProfile {
        val normalizedUrl = normalizeServerUrl(serverUrl)
        val current = _sources.value.toMutableList()
        val id = existingId ?: UUID.randomUUID().toString()
        val shouldActivate = makeActive || current.isEmpty() || current.none { it.isActive }

        val updatedList = if (shouldActivate) {
            current.map { it.copy(isActive = false) }.toMutableList()
        } else {
            current
        }

        val profile = XtreamSourceProfile(
            id = id,
            sourceName = sourceName.trim().ifBlank { "Xtream Source" },
            serverUrl = normalizedUrl,
            username = username.trim(),
            password = password,
            isActive = shouldActivate || (existingId != null && current.firstOrNull { it.id == existingId }?.isActive == true),
            lastVerifiedEpochMs = System.currentTimeMillis(),
            accountStatus = connectionResult.status,
            expDate = connectionResult.expDate,
            activeConnections = connectionResult.activeConnections,
            maxConnections = connectionResult.maxConnections
        )

        val idx = updatedList.indexOfFirst { it.id == id }
        if (idx >= 0) {
            updatedList[idx] = profile
        } else {
            updatedList.add(profile)
        }

        secureStorage.saveSources(updatedList)
        _sources.value = updatedList
        _activeSource.value = updatedList.firstOrNull { it.isActive }
        return profile
    }

    fun setActiveSource(sourceId: String) {
        val updated = _sources.value.map { it.copy(isActive = it.id == sourceId) }
        secureStorage.saveSources(updated)
        _sources.value = updated
        _activeSource.value = updated.firstOrNull { it.isActive }
    }

    fun renameSource(sourceId: String, newName: String) {
        if (newName.isBlank()) return
        val updated = _sources.value.map {
            if (it.id == sourceId) it.copy(sourceName = newName.trim()) else it
        }
        secureStorage.saveSources(updated)
        _sources.value = updated
        _activeSource.value = updated.firstOrNull { it.isActive } ?: updated.firstOrNull()
    }

    suspend fun deleteSource(sourceId: String) {
        val remaining = _sources.value.filterNot { it.id == sourceId }.toMutableList()
        if (remaining.isNotEmpty() && remaining.none { it.isActive }) {
            remaining[0] = remaining[0].copy(isActive = true)
        }
        secureStorage.saveSources(remaining)
        _sources.value = remaining
        _activeSource.value = remaining.firstOrNull { it.isActive } ?: remaining.firstOrNull()

        // Clean isolated Room records for deleted source
        database.playbackProgressDao().clearProgressForSource(sourceId)
        database.watchHistoryDao().clearHistoryForSource(sourceId)
        database.watchlistDao().clearWatchlistForSource(sourceId)
        database.cachedCatalogDao().clearCacheForSource(sourceId)
    }

    suspend fun resetAllSources() {
        secureStorage.clearAllSources()
        _sources.value = emptyList()
        _activeSource.value = null
        database.playbackProgressDao().clearAllProgress()
        database.watchHistoryDao().clearAllHistory()
        database.watchlistDao().clearAllWatchlist()
        database.cachedCatalogDao().clearAllCache()
    }
}
