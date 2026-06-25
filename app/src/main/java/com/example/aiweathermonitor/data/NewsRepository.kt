package com.example.aiweathermonitor.data

import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.data.models.NewsArticle
import com.example.aiweathermonitor.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

private const val TAG = "NewsRepository"

/**
 * Data-access layer for news headlines. Owns the RSS fetch + XML parsing so the
 * ViewModel stays focused on UI state. Pure and side-effect free (no Android
 * Context), making it unit-testable with a mock [OkHttpClient].
 */
interface NewsRepository {
    /**
     * Fetches and parses the configured RSS feed, returning up to
     * [WeatherApiConfig.NEWS_ITEM_LIMIT] articles. Honours an optional [feedUrl]
     * override (defaults to the weather news feed in config).
     */
    suspend fun fetchNews(
        feedUrl: String = WeatherApiConfig.Endpoints.NEWS_RSS_FEED
    ): Result<List<NewsArticle>>
}

class DefaultNewsRepository(
    private val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NewsRepository {

    override suspend fun fetchNews(feedUrl: String): Result<List<NewsArticle>> =
        withContext(ioDispatcher) {
            try {
                val request = Request.Builder()
                    .url(feedUrl)
                    .header("User-Agent", "AIWeatherMonitor/1.0 (Android)")
                    .build()
                val xml = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("RSS feed error: ${response.code}")
                        )
                    }
                    response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty RSS response"))
                }
                Result.success(parseRss(xml))
            } catch (e: Exception) {
                AppLogger.error("Failed to fetch news", TAG, e)
                Result.failure(e)
            }
        }

    /**
     * Parses an RSS 2.0 document into [NewsArticle]s using a pull parser.
     * Reads `<title>`, `<link>`, `<pubDate>`, `<description>`, and `<source>`
     * inside each `<item>`. Skips the channel-level metadata.
     */
    private fun parseRss(xml: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val parser = android.util.Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }

        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var description = ""
        var source = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name?.lowercase()) {
                        "item" -> {
                            inItem = true
                            title = ""; link = ""; pubDate = ""; description = ""; source = ""
                        }
                        "title"       -> if (inItem) title = parser.nextTextSafe()
                        "link"        -> if (inItem) link = parser.nextTextSafe()
                        "pubdate"     -> if (inItem) pubDate = parser.nextTextSafe()
                        "description" -> if (inItem) description = stripHtml(parser.nextTextSafe())
                        "source"      -> if (inItem) source = parser.nextTextSafe()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name?.lowercase() == "item" && inItem) {
                        inItem = false
                        if (title.isNotBlank() && link.isNotBlank()) {
                            articles.add(
                                NewsArticle(
                                    title = cleanTitle(title),
                                    link = link.trim(),
                                    source = source.trim(),
                                    pubDate = pubDate.trim(),
                                    description = description.trim()
                                )
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return sortAndLimit(articles)
    }

    /** RFC-1123 pubDate (e.g. "Wed, 03 Jun 2026 10:17:35 GMT") → epoch millis; 0 if missing/unparseable. */
    private fun parsePubMillis(pubDate: String): Long = runCatching {
        if (pubDate.isBlank()) 0L
        else java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
            .parse(pubDate)?.time ?: 0L
    }.getOrDefault(0L)

    /**
     * Newest-first ordering. Drops clearly-stale items (older than 30 days) so old
     * Google-News matches (some from years ago) don't surface — but falls back to
     * the full sorted list if nothing recent is left.
     */
    private fun sortAndLimit(articles: List<NewsArticle>): List<NewsArticle> {
        val dated = articles.map { it to parsePubMillis(it.pubDate) }
            .sortedByDescending { it.second }
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recent = dated.filter { it.second == 0L || it.second >= cutoff }
        return (if (recent.isNotEmpty()) recent else dated)
            .take(WeatherApiConfig.NEWS_ITEM_LIMIT)
            .map { it.first }
    }

    /** Safely reads the text content of the current element, returning "" on any issue. */
    private fun XmlPullParser.nextTextSafe(): String = runCatching {
        if (next() == XmlPullParser.TEXT) text?.trim().orEmpty() else ""
    }.getOrDefault("")

    /** Google News titles are "Headline - Publisher"; trim a trailing publisher suffix. */
    private fun cleanTitle(raw: String): String {
        val decoded = stripHtml(raw)
        val idx = decoded.lastIndexOf(" - ")
        return if (idx > 0 && idx > decoded.length - 60) decoded.substring(0, idx).trim()
        else decoded
    }

    /** Removes HTML tags and decodes a few common entities — keeps the repo Android-UI free. */
    private fun stripHtml(input: String): String =
        input.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
}
