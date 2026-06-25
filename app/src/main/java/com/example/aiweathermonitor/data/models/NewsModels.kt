package com.example.aiweathermonitor.data.models

import kotlinx.serialization.Serializable

/**
 * A single news headline parsed from an RSS 2.0 feed `<item>` element.
 *
 * Side-effect free and serializable so it can be cached alongside the rest of
 * the app state if ever needed. [link] is opened in the browser when tapped.
 */
@Serializable
data class NewsArticle(
    val title: String = "",
    val link: String = "",
    val source: String = "",          // Publisher / feed source name
    val pubDate: String = "",         // Raw RFC-822 date string from the feed
    val description: String = ""      // Plain-text summary (HTML stripped)
)
