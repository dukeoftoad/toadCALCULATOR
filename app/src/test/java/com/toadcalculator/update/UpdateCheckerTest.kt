// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.update

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertThrows
import com.toadcalculator.update.data.GithubReleaseChecker
import com.toadcalculator.update.data.UpdateCheckException
import com.toadcalculator.update.data.UpdateCheckRateLimitException
import com.toadcalculator.update.data.UpdateCheckRetryableException
import kotlinx.coroutines.CancellationException
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Parsing of the real GitHub releases/latest payload shape with org.json
 * (provided by Robolectric on the JVM — the same implementation Android ships)
 * plus the retry/rate-limit policy — both exercised with zero network, exactly
 * as in toadFILES' UpdateCheckerParsingTest / UpdateCheckerRetryTest, adapted
 * to this repo's Truth style and single-APK release convention.
 */
@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    // ---------- parsing ----------

    private val payload = """
        {
          "url": "https://api.github.com/repos/dukeoftoad/toadCALCULATOR/releases/1",
          "html_url": "https://github.com/dukeoftoad/toadCALCULATOR/releases/tag/v1.2.0",
          "tag_name": "v1.2.0",
          "target_commitish": "main",
          "name": "toadCALCULATOR 1.2.0",
          "body": "### Fixes\r\n- In-app update flow (ported from toadFILES)\r\n",
          "draft": false,
          "prerelease": false,
          "created_at": "2026-11-21T10:00:00Z",
          "published_at": "2026-11-21T10:12:00Z",
          "assets": [
            {
              "name": "toadCALCULATOR-v1.2.0-sources.zip",
              "content_type": "application/zip",
              "browser_download_url": "https://github.com/dukeoftoad/toadCALCULATOR/releases/download/v1.2.0/toadCALCULATOR-v1.2.0-sources.zip",
              "size": 204800
            },
            {
              "name": "toadCALCULATOR-v1.2.0.apk",
              "content_type": "application/vnd.android.package-archive",
              "browser_download_url": "https://github.com/dukeoftoad/toadCALCULATOR/releases/download/v1.2.0/toadCALCULATOR-v1.2.0.apk",
              "size": 1761280
            },
            {
              "name": "toadCALCULATOR-v1.2.0-sha256.txt",
              "content_type": "text/plain",
              "browser_download_url": "https://github.com/dukeoftoad/toadCALCULATOR/releases/download/v1.2.0/toadCALCULATOR-v1.2.0-sha256.txt",
              "size": 142
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parseRelease picks the first apk asset and the release fields`() {
        val release = GithubReleaseChecker.parseRelease(payload)

        assertThat(release.tagName).isEqualTo("v1.2.0")
        assertThat(release.htmlUrl)
            .isEqualTo("https://github.com/dukeoftoad/toadCALCULATOR/releases/tag/v1.2.0")
        // The zip and checksum assets must be skipped in favor of the .apk one.
        assertThat(release.apkName).isEqualTo("toadCALCULATOR-v1.2.0.apk")
        assertThat(release.apkUrl).isEqualTo(
            "https://github.com/dukeoftoad/toadCALCULATOR/releases/download/v1.2.0/toadCALCULATOR-v1.2.0.apk"
        )
        assertThat(release.apkSizeBytes).isEqualTo(1761280L)
        // Notes keep the body text (CR-LF normalized to LF), markdown kept raw.
        assertThat(release.notes).isNotNull()
        assertThat(release.notes!!).startsWith("### Fixes")
        assertThat(release.notes!!).contains("ported from toadFILES")
        assertThat(release.notes!!).doesNotContain("\r")
    }

    @Test
    fun `parseRelease tolerates a release with no apk asset`() {
        val noApk = """
            {
              "tag_name": "v1.3.0",
              "html_url": "https://github.com/dukeoftoad/toadCALCULATOR/releases/tag/v1.3.0",
              "body": "Sources only.",
              "assets": [
                { "name": "sources.zip", "browser_download_url": "https://x/s.zip", "size": 1 }
              ]
            }
        """.trimIndent()

        val release = GithubReleaseChecker.parseRelease(noApk)

        assertThat(release.tagName).isEqualTo("v1.3.0")
        assertThat(release.apkName).isNull()
        assertThat(release.apkUrl).isNull()
        assertThat(release.apkSizeBytes).isEqualTo(0L)
    }

    @Test
    fun `parseRelease treats a missing assets array and empty body honestly`() {
        val minimal = """{"tag_name": "v1.4.0"}"""

        val release = GithubReleaseChecker.parseRelease(minimal)

        assertThat(release.tagName).isEqualTo("v1.4.0")
        assertThat(release.apkUrl).isNull()
        assertThat(release.notes).isNull()
        // html_url falls back to the repo releases page so "View on GitHub" always works.
        assertThat(release.htmlUrl)
            .isEqualTo("https://github.com/dukeoftoad/toadCALCULATOR/releases")
    }

    @Test
    fun `parseRelease refuses a payload without tag_name`() {
        assertThrows(JSONException::class.java) {
            GithubReleaseChecker.parseRelease("""{"html_url": "https://github.com/x"}""")
        }
    }

    // ---------- retry / rate-limit policy ----------

    private class Counter {
        var calls = 0
            private set

        fun tick() {
            calls++
        }
    }

    @Test
    fun `transient IOException is retried once and then succeeds`() {
        val counter = Counter()
        val result = GithubReleaseChecker.runWithRetry(2) { attempt ->
            counter.tick()
            if (attempt == 0) throw IOException("connection reset") else "ok"
        }
        assertThat(result).isEqualTo("ok")
        assertThat(counter.calls).isEqualTo(2) // 1 fail + 1 retry
    }

    @Test
    fun `exhausted retries rethrow the last transient IOException`() {
        val e = assertThrows(IOException::class.java) {
            GithubReleaseChecker.runWithRetry(2) { _ -> throw IOException("no route to host") }
        }
        assertThat(e.message).isEqualTo("no route to host")
    }

    @Test
    fun `success on the first attempt never hits the retry path`() {
        val calls = intArrayOf(0)
        val out = GithubReleaseChecker.runWithRetry(2) { calls[0]++; "done" }
        assertThat(out).isEqualTo("done")
        assertThat(calls[0]).isEqualTo(1)
    }

    @Test
    fun `rate limit 403 is never retried`() {
        val calls = intArrayOf(0)
        val e = assertThrows(UpdateCheckRateLimitException::class.java) {
            GithubReleaseChecker.runWithRetry(2) {
                calls[0]++
                throw UpdateCheckRateLimitException()
            }
        }
        assertThat(e.message).contains("rate limit")
        assertThat(calls[0]).isEqualTo(1)
    }

    @Test
    fun `HTTP 5xx is retryable and surfaces its friendly message`() {
        val calls = intArrayOf(0)
        val e = assertThrows(UpdateCheckRetryableException::class.java) {
            GithubReleaseChecker.runWithRetry(2) {
                calls[0]++
                throw UpdateCheckRetryableException(503)
            }
        }
        assertThat(e.message).contains("503")
        assertThat(e.message).contains("try again")
        assertThat(calls[0]).isEqualTo(2) // 5xx IS retried
    }

    @Test
    fun `other 4xx is permanent and not retried`() {
        val calls = intArrayOf(0)
        val e = assertThrows(UpdateCheckException::class.java) {
            GithubReleaseChecker.runWithRetry(2) {
                calls[0]++
                throw GithubReleaseChecker.exceptionFor(404)
            }
        }
        assertThat(e.message).contains("404")
        assertThat(calls[0]).isEqualTo(1)
    }

    @Test
    fun `malformed payload JSON is not retried`() {
        val calls = intArrayOf(0)
        assertThrows(JSONException::class.java) {
            GithubReleaseChecker.runWithRetry(2) {
                calls[0]++
                throw JSONException("bad payload")
            }
        }
        assertThat(calls[0]).isEqualTo(1)
    }

    @Test
    fun `cancellation is propagated untouched, never retried`() {
        val calls = intArrayOf(0)
        assertThrows(CancellationException::class.java) {
            GithubReleaseChecker.runWithRetry(2) {
                calls[0]++
                throw CancellationException("user left Settings")
            }
        }
        assertThat(calls[0]).isEqualTo(1)
    }

    // ---------- status-code classification ----------

    @Test
    fun `exceptionFor maps 403, 5xx and other 4xx to distinct friendly types`() {
        val rate = GithubReleaseChecker.exceptionFor(403)
        assertThat(rate).isInstanceOf(UpdateCheckRateLimitException::class.java)
        assertThat(rate.message).contains("rate limit")

        val server = GithubReleaseChecker.exceptionFor(502)
        assertThat(server).isInstanceOf(UpdateCheckRetryableException::class.java)
        assertThat(server.message).contains("502")

        val other = GithubReleaseChecker.exceptionFor(418)
        assertThat(other).isInstanceOf(UpdateCheckException::class.java)
        assertThat(other).isNotInstanceOf(UpdateCheckRetryableException::class.java)
        assertThat(other.message).contains("418")
    }
}
