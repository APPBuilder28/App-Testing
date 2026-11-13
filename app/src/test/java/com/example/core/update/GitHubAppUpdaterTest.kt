package com.example.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAppUpdaterTest {

    @Test
    fun `normalizeRepoSlug parses owner repo and full github urls`() {
        assertEquals("alice/nextvu-player", GitHubAppUpdater.normalizeRepoSlug("alice/nextvu-player"))
        assertEquals("alice/nextvu-player", GitHubAppUpdater.normalizeRepoSlug("https://github.com/alice/nextvu-player"))
        assertEquals("alice/nextvu-player", GitHubAppUpdater.normalizeRepoSlug("https://github.com/alice/nextvu-player.git/"))
        assertEquals("alice/nextvu-player", GitHubAppUpdater.normalizeRepoSlug("http://www.github.com/alice/nextvu-player/releases"))
        assertEquals(
            GitHubAppUpdater.DEFAULT_GITHUB_REPO,
            GitHubAppUpdater.normalizeRepoSlug("https://github.com/APPBuilder28/App-Testing")
        )
        assertNull(GitHubAppUpdater.normalizeRepoSlug("only-owner"))
        assertNull(GitHubAppUpdater.normalizeRepoSlug("   "))
    }

    @Test
    fun `compareVersionNames handles v prefixes and multi-segment versions`() {
        assertTrue(GitHubAppUpdater.compareVersionNames("v1.3", "1.2") > 0)
        assertTrue(GitHubAppUpdater.compareVersionNames("1.2.1", "1.2") > 0)
        assertTrue(GitHubAppUpdater.compareVersionNames("2.0.0", "1.9.9") > 0)
        assertEquals(0, GitHubAppUpdater.compareVersionNames("v1.2.0", "1.2"))
        assertTrue(GitHubAppUpdater.compareVersionNames("1.1", "1.2") < 0)
    }

    @Test
    fun `parseGradleVersion extracts versionCode and versionName from build gradle kts`() {
        val sample = """
            defaultConfig {
              applicationId = "com.aistudio.nextvuplayer.k7m9vx"
              minSdk = 24
              targetSdk = 36
              versionCode = 4
              versionName = "1.3"
            }
        """.trimIndent()

        val (code, name) = GitHubAppUpdater.parseGradleVersion(sample)
        assertEquals(4, code)
        assertEquals("1.3", name)
    }

    @Test
    fun `isBranchAheadOfRelease prevents offering stale release APK when branch is newer`() {
        assertTrue(GitHubAppUpdater.compareVersionNames("1.4", "1.3") > 0)
        // Branch moved to 1.4 (Build 5) while Release is still at v1.3
        assertTrue(
            GitHubAppUpdater.isBranchAheadOfRelease(
                releaseTag = "v1.3",
                syncedVersionName = "1.4",
                syncedVersionCode = 5,
                installedVersionName = "1.3",
                installedVersionCode = 4
            )
        )
        // Release v1.4 matches branch 1.4 -> safe to offer v1.4 release APK
        assertTrue(
            !GitHubAppUpdater.isBranchAheadOfRelease(
                releaseTag = "v1.4",
                syncedVersionName = "1.4",
                syncedVersionCode = 5,
                installedVersionName = "1.3",
                installedVersionCode = 4
            )
        )
    }
}
