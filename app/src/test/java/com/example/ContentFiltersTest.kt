package com.example

import com.example.core.filter.ContentFilters
import com.example.data.remote.XtreamCategory
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamMovie
import com.example.data.remote.XtreamSeries
import com.example.viewmodel.LiveCatalogData
import com.example.viewmodel.MoviesCatalogData
import com.example.viewmodel.SeriesCatalogData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFiltersTest {

    @Test
    fun ukPlaylistMatcherHandlesCommonFormatsWithoutFalsePositives() {
        assertTrue(ContentFilters.isUkPlaylistName("UK | Entertainment"))
        assertTrue(ContentFilters.isUkPlaylistName("|UK| Sports FHD"))
        assertTrue(ContentFilters.isUkPlaylistName("[UK] News"))
        assertTrue(ContentFilters.isUkPlaylistName("UK_NEWS"))
        assertTrue(ContentFilters.isUkPlaylistName("United Kingdom Cinema"))
        assertTrue(ContentFilters.isUkPlaylistName("U.K. Kids"))
        assertTrue(ContentFilters.isUkPlaylistName("|4K| Cinema Entertainment"))
        assertTrue(ContentFilters.isUkPlaylistName("8K Ultra Sports"))
        assertTrue(ContentFilters.isUkPlaylistName("[UHD] Regional Channels"))
        assertTrue(ContentFilters.isUkPlaylistName("UK 4K 8K UHD"))

        assertFalse(ContentFilters.isUkPlaylistName("Ukraine TV"))
        assertFalse(ContentFilters.isUkPlaylistName("Turkish Drama"))
        assertFalse(ContentFilters.isUkPlaylistName("USA Sports"))
        assertFalse(ContentFilters.isUkPlaylistName("104K Mix"))
    }

    @Test
    fun englishCategoryAndTitleMatcherHandlesEnglishAndEngWithoutSpanishEnFalsePositives() {
        assertTrue(ContentFilters.isEnglishOrEngCategoryName("English Movies"))
        assertTrue(ContentFilters.isEnglishOrEngCategoryName("ENG | Action"))
        assertTrue(ContentFilters.isEnglishOrEngCategoryName("[ENG] 4K"))
        assertTrue(ContentFilters.isEnglishOrEngCategoryName("EN | New Releases"))
        assertTrue(ContentFilters.isEnglishOrEngCategoryName("|EN| Drama"))
        assertTrue(ContentFilters.isEnglishOrEngCategoryName("[EN] Comedy"))

        assertFalse(ContentFilters.isEnglishOrEngCategoryName("Peliculas en HD"))
        assertFalse(ContentFilters.isEnglishOrEngCategoryName("CINE EN ESPAÑOL"))
        assertFalse(ContentFilters.isEnglishOrEngCategoryName("FR | Cinema"))

        assertTrue(ContentFilters.isEnglishOrEngTitle("The Matrix (ENG)"))
        assertTrue(ContentFilters.isEnglishOrEngTitle("Planet Earth [English]"))
        assertFalse(ContentFilters.isEnglishOrEngTitle("El Camino en Vivo"))
        assertFalse(ContentFilters.isEnglishOrEngTitle("Engineering Marvels"))
    }

    @Test
    fun filterLiveCatalogKeepsOnlyUkCategoriesAndChannels() {
        val data = LiveCatalogData(
            categories = listOf(
                XtreamCategory("1", "UK | Sports"),
                XtreamCategory("2", "USA | News"),
                XtreamCategory("3", "FR | Cinema")
            ),
            channels = listOf(
                XtreamLiveChannel(streamId = "10", name = "Sky Sports", categoryId = "1"),
                XtreamLiveChannel(streamId = "20", name = "CNN US", categoryId = "2"),
                XtreamLiveChannel(streamId = "30", name = "BBC One UK", categoryId = "3")
            )
        )

        val filtered = ContentFilters.filterLiveCatalog(data, ukOnly = true)
        assertEquals(listOf("1"), filtered.categories.map { it.categoryId })
        assertEquals(listOf("10", "30"), filtered.channels.map { it.streamId })
    }

    @Test
    fun filterMoviesAndSeriesCatalogKeepsOnlyEnglishOrEngItems() {
        val moviesData = MoviesCatalogData(
            categories = listOf(
                XtreamCategory("1", "ENG | Action"),
                XtreamCategory("2", "ES | Accion")
            ),
            movies = listOf(
                XtreamMovie(streamId = "100", name = "Inception", categoryId = "1"),
                XtreamMovie(streamId = "101", name = "La Casa", categoryId = "2"),
                XtreamMovie(streamId = "102", name = "Gladiator (English)", categoryId = "2")
            )
        )
        val filteredMovies = ContentFilters.filterMoviesCatalog(moviesData, englishOnly = true)
        assertEquals(listOf("1"), filteredMovies.categories.map { it.categoryId })
        assertEquals(listOf("100", "102"), filteredMovies.movies.map { it.streamId })

        val seriesData = SeriesCatalogData(
            categories = listOf(
                XtreamCategory("1", "English Series"),
                XtreamCategory("2", "Arabic Series")
            ),
            series = listOf(
                XtreamSeries(seriesId = "200", name = "Sherlock", categoryId = "1"),
                XtreamSeries(seriesId = "201", name = "Al Hayba", categoryId = "2"),
                XtreamSeries(seriesId = "202", name = "Planet Earth [ENG]", categoryId = "2")
            )
        )
        val filteredSeries = ContentFilters.filterSeriesCatalog(seriesData, englishOnly = true)
        assertEquals(listOf("1"), filteredSeries.categories.map { it.categoryId })
        assertEquals(listOf("200", "202"), filteredSeries.series.map { it.seriesId })
    }
}
