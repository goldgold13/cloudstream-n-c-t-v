package com.lagradost.cloudstream3.plugins.nangcuc

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class NangCucProvider : MainAPI() {
    override var mainUrl = "https://nangcuc.dev"
    override var name = "Nắng Cực TV"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "vi"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page/$page").document
        val home = document.select("article.video-item").mapNotNull {
            it.toSearchResponse()
        }
        return newHomepageResponse("Phim Mới Nhất", home)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = select("h2 a").text() ?: return null
        val href = fixUrl(select("h2 a").attr("href"))
        val posterUrl = fixUrlNull(select("img").attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("article.video-item").mapNotNull {
            it.toSearchResponse()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.select("h1").text()
        val poster = fixUrlNull(document.select("img.wp-post-image").attr("src"))
        val plot = document.select("div.entry-content p").first()?.text()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.contains("http")) {
                callback.invoke(
                    ExtractorLink(
                        name,
                        name,
                        src,
                        "",
                        Qualities.P1080.value,
                        isM3u8 = src.contains(".m3u8")
                    )
                )
            }
        }
        return true
    }
}
