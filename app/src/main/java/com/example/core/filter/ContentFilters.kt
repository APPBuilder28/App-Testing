package com.example.core.filter

import com.example.viewmodel.LiveCatalogData
import com.example.viewmodel.MoviesCatalogData
import com.example.viewmodel.SeriesCatalogData

object ContentFilters {
    private val UK_REGEX = Regex(
        pattern = """(?:^|[^a-zA-Z0-9])(UK|U\.K\.|UNITED[\s._-]+KINGDOM)(?:[^a-zA-Z0-9]|$)""",
        option = RegexOption.IGNORE_CASE
    )

    private val ENGLISH_OR_ENG_REGEX = Regex(
        pattern = """(?:^|[^a-zA-Z0-9])(ENGLISH|ENG)(?:[^a-zA-Z0-9]|$)""",
        option = RegexOption.IGNORE_CASE
    )

    private val LEADING_EN_CATEGORY_REGEX = Regex(
        pattern = """^\s*(?:[\[\|(]\s*)?EN\s*(?:[\]|):/\-_]|$)""",
        option = RegexOption.IGNORE_CASE
    )

    private val EN_TAG_CATEGORY_REGEX = Regex(
        pattern = """(?:[\[\|(]\s*EN\s*[\]|)]|(?:^|[^a-zA-Z0-9])EN\s*[:|\-/])""",
        option = RegexOption.IGNORE_CASE
    )

    fun isUkPlaylistName(name: String): Boolean {
        if (name.isBlank()) return false
        return UK_REGEX.containsMatchIn(name)
    }

    fun isEnglishOrEngCategoryName(name: String): Boolean {
        if (name.isBlank()) return false
        return ENGLISH_OR_ENG_REGEX.containsMatchIn(name) ||
            LEADING_EN_CATEGORY_REGEX.containsMatchIn(name) ||
            EN_TAG_CATEGORY_REGEX.containsMatchIn(name)
    }

    fun isEnglishOrEngTitle(name: String, genre: String = ""): Boolean {
        return (name.isNotBlank() && ENGLISH_OR_ENG_REGEX.containsMatchIn(name)) ||
            (genre.isNotBlank() && ENGLISH_OR_ENG_REGEX.containsMatchIn(genre))
    }

    fun filterLiveCatalog(data: LiveCatalogData, ukOnly: Boolean): LiveCatalogData {
        if (!ukOnly) return data
        val ukCategories = data.categories.filter { isUkPlaylistName(it.categoryName) }
        val ukCategoryIds = ukCategories.map { it.categoryId }.toSet()
        val ukChannels = data.channels.filter { ch ->
            ukCategoryIds.contains(ch.categoryId) || isUkPlaylistName(ch.name)
        }
        return LiveCatalogData(categories = ukCategories, channels = ukChannels)
    }

    fun filterMoviesCatalog(data: MoviesCatalogData, englishOnly: Boolean): MoviesCatalogData {
        if (!englishOnly) return data
        val matchingCategories = data.categories.filter { isEnglishOrEngCategoryName(it.categoryName) }
        val matchingCatIds = matchingCategories.map { it.categoryId }.toSet()
        val filteredMovies = data.movies.filter { movie ->
            matchingCatIds.contains(movie.categoryId) || isEnglishOrEngTitle(movie.name, movie.genre)
        }
        val effectiveCategories = if (matchingCategories.isNotEmpty()) {
            matchingCategories
        } else {
            val catsWithMatchingMovies = filteredMovies.map { it.categoryId }.toSet()
            data.categories.filter { catsWithMatchingMovies.contains(it.categoryId) }
        }
        return MoviesCatalogData(categories = effectiveCategories, movies = filteredMovies)
    }

    fun filterSeriesCatalog(data: SeriesCatalogData, englishOnly: Boolean): SeriesCatalogData {
        if (!englishOnly) return data
        val matchingCategories = data.categories.filter { isEnglishOrEngCategoryName(it.categoryName) }
        val matchingCatIds = matchingCategories.map { it.categoryId }.toSet()
        val filteredSeries = data.series.filter { series ->
            matchingCatIds.contains(series.categoryId) || isEnglishOrEngTitle(series.name, series.genre)
        }
        val effectiveCategories = if (matchingCategories.isNotEmpty()) {
            matchingCategories
        } else {
            val catsWithMatchingSeries = filteredSeries.map { it.categoryId }.toSet()
            data.categories.filter { catsWithMatchingSeries.contains(it.categoryId) }
        }
        return SeriesCatalogData(categories = effectiveCategories, series = filteredSeries)
    }
}
