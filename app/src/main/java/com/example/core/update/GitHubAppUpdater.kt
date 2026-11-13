package com.example.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
    val size: Long = 0L
)

@Serializable
data class GitHubLatestReleaseDto(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("published_at")
    val publishedAt: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

@Serializable
data class GitHubCommitAuthorDto(
    val date: String = ""
)

@Serializable
data class GitHubCommitDetailDto(
    val message: String = "",
    val author: GitHubCommitAuthorDto = GitHubCommitAuthorDto()
)

@Serializable
data class GitHubCommitItemDto(
    val sha: String = "",
    @SerialName("html_url")
    val htmlUrl: String = "",
    val commit: GitHubCommitDetailDto = GitHubCommitDetailDto()
)

@Serializable
data class GitHubWorkflowRunDto(
    val id: Long = 0L,
    val name: String = "",
    val status: String = "",
    val conclusion: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("head_sha")
    val headSha: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class GitHubWorkflowRunsResponse(
    @SerialName("workflow_runs")
    val workflowRuns: List<GitHubWorkflowRunDto> = emptyList()
)

data class GitHubUpdateInfo(
    val repoSlug: String,
    val installedVersionName: String,
    val installedVersionCode: Int,
    val latestVersionName: String,
    val latestVersionCode: Int?,
    val isUpdateAvailable: Boolean,
    val sourceLabel: String,
    val summaryText: String,
    val releaseOrBuildUrl: String,
    val directApkAssetUrl: String?,
    val latestCommitShaShort: String?,
    val publishedOrSyncedAt: String?
)

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data class Checking(val repoSlug: String) : AppUpdateUiState
    data class UpToDate(val info: GitHubUpdateInfo) : AppUpdateUiState
    data class UpdateAvailable(val info: GitHubUpdateInfo) : AppUpdateUiState
    data class Error(val message: String) : AppUpdateUiState
}

object GitHubAppUpdater {

    const val DEFAULT_GITHUB_REPO = "APPBuilder28/App-Testing"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val slugRegex = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    private val versionCodeRegex = Regex("""versionCode\s*=\s*(\d+)""")
    private val versionNameRegex = Regex("""versionName\s*=\s*["']([^"']+)["']""")

    /**
     * Normalizes user input such as:
     * - "owner/repo"
     * - "https://github.com/owner/repo"
     * - "https://github.com/owner/repo.git/"
     * into "owner/repo", or returns null if invalid.
     */
    fun normalizeRepoSlug(rawInput: String): String? {
        var cleaned = rawInput.trim()
        if (cleaned.isBlank()) return null

        cleaned = cleaned
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("github.com/")
            .removePrefix("www.github.com/")
            .trim('/')
            .removeSuffix(".git")

        val parts = cleaned.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val slug = "${parts[0]}/${parts[1]}"
        return slug.takeIf { slugRegex.matches(it) }
    }

    /**
     * Compares version strings like "v1.3", "1.2.1", "1.2" numerically.
     * Returns > 0 if [remote] > [local], 0 if equal, < 0 if older.
     */
    fun compareVersionNames(remote: String, local: String): Int {
        val rParts = tokenizeVersion(remote)
        val lParts = tokenizeVersion(local)
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val rv = rParts.getOrElse(i) { 0 }
            val lv = lParts.getOrElse(i) { 0 }
            if (rv != lv) return rv.compareTo(lv)
        }
        return 0
    }

    private fun tokenizeVersion(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        return Regex("""\d+""")
            .findAll(cleaned)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
    }

    fun parseGradleVersion(buildGradleText: String): Pair<Int?, String?> {
        val code = versionCodeRegex.find(buildGradleText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val name = versionNameRegex.find(buildGradleText)?.groupValues?.getOrNull(1)?.trim()
        return code to name
    }

    suspend fun checkForUpdate(
        repoInput: String,
        installedVersionName: String,
        installedVersionCode: Int
    ): AppUpdateUiState = withContext(Dispatchers.IO) {
        val slug = normalizeRepoSlug(repoInput)
            ?: return@withContext AppUpdateUiState.Error(
                "Enter a valid GitHub repository (e.g. owner/nextvu-player or full GitHub URL)."
            )

        try {
            val releaseOutcome = fetchLatestRelease(slug)
            if (releaseOutcome is FetchOutcome.HttpError && releaseOutcome.code == 403) {
                return@withContext AppUpdateUiState.Error(
                    "GitHub API rate limit reached. Please try again in a few minutes."
                )
            }

            val release = (releaseOutcome as? FetchOutcome.Success)?.value
            val latestCommit = fetchLatestCommit(slug)
            val gradleMeta = fetchSyncedGradleVersion(slug, latestCommit?.sha)
            val latestWorkflowRun = fetchLatestApkBuildRun(slug)

            if (release == null && gradleMeta == null && latestCommit == null) {
                val errDetail = when {
                    releaseOutcome is FetchOutcome.HttpError && releaseOutcome.code == 404 ->
                        "Repository \"$slug\" was not found or is private."
                    else ->
                        "Could not read release or version info from GitHub for \"$slug\"."
                }
                return@withContext AppUpdateUiState.Error(errDetail)
            }

            val releaseTag = release?.tagName?.trim().orEmpty()
            val releaseApkUrl = release?.assets
                ?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank() }
                ?.browserDownloadUrl

            val syncedVersionCode = gradleMeta?.first
            val syncedVersionName = gradleMeta?.second?.takeIf { it.isNotBlank() }

            val effectiveVersionName = when {
                releaseTag.isNotBlank() && syncedVersionName != null -> {
                    if (compareVersionNames(syncedVersionName, releaseTag) > 0) syncedVersionName else releaseTag
                }
                releaseTag.isNotBlank() -> releaseTag
                syncedVersionName != null -> syncedVersionName
                else -> installedVersionName
            }

            val releaseNewer = releaseTag.isNotBlank() && compareVersionNames(releaseTag, installedVersionName) > 0
            val syncedCodeNewer = syncedVersionCode != null && syncedVersionCode > installedVersionCode
            val syncedNameNewer = syncedVersionName != null && compareVersionNames(syncedVersionName, installedVersionName) > 0
            val updateAvailable = releaseNewer || syncedCodeNewer || syncedNameNewer

            val branchAheadOfRelease = isBranchAheadOfRelease(
                releaseTag = releaseTag,
                syncedVersionName = syncedVersionName,
                syncedVersionCode = syncedVersionCode,
                installedVersionName = installedVersionName,
                installedVersionCode = installedVersionCode
            )

            val effectiveDirectApkUrl = if (branchAheadOfRelease) null else releaseApkUrl

            val sourceLabel = when {
                !branchAheadOfRelease && (releaseNewer || release != null) -> "GitHub Release"
                gradleMeta != null -> "GitHub Synced Branch"
                else -> "GitHub Repository"
            }

            val firstLineCommitMsg = latestCommit?.commit?.message
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            val shortSha = latestCommit?.sha?.take(7)?.takeIf { it.isNotBlank() }

            val summary = buildString {
                if (!branchAheadOfRelease && release != null && release.body.isNotBlank()) {
                    append(release.body.trim().take(280))
                } else if (firstLineCommitMsg.isNotBlank()) {
                    append("Latest commit")
                    if (shortSha != null) append(" ($shortSha)")
                    append(": $firstLineCommitMsg")
                } else if (updateAvailable) {
                    append("A newer version synced on GitHub is ready.")
                } else {
                    append("Installed app matches the latest synced version on GitHub.")
                }
            }

            val targetUrl = when {
                !branchAheadOfRelease && !release?.htmlUrl.isNullOrBlank() -> release!!.htmlUrl
                latestWorkflowRun?.htmlUrl?.isNotBlank() == true -> latestWorkflowRun.htmlUrl
                latestCommit?.htmlUrl?.isNotBlank() == true -> latestCommit.htmlUrl
                release?.htmlUrl?.isNotBlank() == true -> release.htmlUrl
                else -> "https://github.com/$slug/releases"
            }

            val timestamp = (if (!branchAheadOfRelease) release?.publishedAt?.takeIf { it.isNotBlank() } else null)
                ?: latestWorkflowRun?.updatedAt?.takeIf { it.isNotBlank() }
                ?: latestCommit?.commit?.author?.date?.takeIf { it.isNotBlank() }

            val info = GitHubUpdateInfo(
                repoSlug = slug,
                installedVersionName = installedVersionName,
                installedVersionCode = installedVersionCode,
                latestVersionName = effectiveVersionName,
                latestVersionCode = syncedVersionCode,
                isUpdateAvailable = updateAvailable,
                sourceLabel = sourceLabel,
                summaryText = summary,
                releaseOrBuildUrl = targetUrl,
                directApkAssetUrl = effectiveDirectApkUrl,
                latestCommitShaShort = shortSha,
                publishedOrSyncedAt = timestamp?.take(10)
            )

            if (updateAvailable) {
                AppUpdateUiState.UpdateAvailable(info)
            } else {
                AppUpdateUiState.UpToDate(info)
            }
        } catch (t: Throwable) {
            AppUpdateUiState.Error(t.localizedMessage ?: "Failed to check GitHub for updates.")
        }
    }

    internal fun isBranchAheadOfRelease(
        releaseTag: String,
        syncedVersionName: String?,
        syncedVersionCode: Int?,
        installedVersionName: String,
        installedVersionCode: Int
    ): Boolean {
        if (releaseTag.isBlank()) return true
        val branchNameAheadOfRelease = !syncedVersionName.isNullOrBlank() &&
            compareVersionNames(syncedVersionName, releaseTag) > 0
        val branchCodeAheadWhileSameTag = syncedVersionCode != null &&
            syncedVersionCode > installedVersionCode &&
            compareVersionNames(releaseTag, installedVersionName) <= 0
        return branchNameAheadOfRelease || branchCodeAheadWhileSameTag
    }

    private sealed interface FetchOutcome<out T> {
        data class Success<T>(val value: T) : FetchOutcome<T>
        data class HttpError(val code: Int) : FetchOutcome<Nothing>
        data object Failed : FetchOutcome<Nothing>
    }

    private fun fetchLatestRelease(slug: String): FetchOutcome<GitHubLatestReleaseDto> {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$slug/releases/latest")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NextVuPlayer-Android")
            .build()
        return executeJson(req)
    }

    private fun fetchLatestCommit(slug: String): GitHubCommitItemDto? {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$slug/commits?per_page=1")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NextVuPlayer-Android")
            .build()
        return when (val res = executeJson<List<GitHubCommitItemDto>>(req)) {
            is FetchOutcome.Success -> res.value.firstOrNull()
            else -> null
        }
    }

    private fun fetchLatestApkBuildRun(slug: String): GitHubWorkflowRunDto? {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$slug/actions/workflows/build-apk.yml/runs?per_page=1")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NextVuPlayer-Android")
            .build()
        return when (val res = executeJson<GitHubWorkflowRunsResponse>(req)) {
            is FetchOutcome.Success -> res.value.workflowRuns.firstOrNull()
            else -> null
        }
    }

    private fun fetchSyncedGradleVersion(slug: String, commitSha: String?): Pair<Int?, String?>? {
        val refs = buildList {
            if (!commitSha.isNullOrBlank()) add(commitSha)
            add("HEAD")
            add("main")
            add("master")
        }.distinct()
        for (ref in refs) {
            val req = Request.Builder()
                .url("https://raw.githubusercontent.com/$slug/$ref/app/build.gradle.kts")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("User-Agent", "NextVuPlayer-Android")
                .build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val parsed = parseGradleVersion(body)
                        if (parsed.first != null || parsed.second != null) {
                            return parsed
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private inline fun <reified T> executeJson(request: Request): FetchOutcome<T> {
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return FetchOutcome.HttpError(resp.code)
                }
                val body = resp.body?.string() ?: return FetchOutcome.Failed
                FetchOutcome.Success(json.decodeFromString<T>(body))
            }
        } catch (_: Throwable) {
            FetchOutcome.Failed
        }
    }
}
