package com.example.executor

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class LunaWebSearchExecutor(private val context: Context) {

    companion object {
        private const val TAG = "LunaWebSearchExecutor"
    }

    data class SearchResult(
        val success: Boolean,
        val message: String
    )

    fun performSearch(query: String): SearchResult {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return SearchResult(false, "What would you like me to search for?")
        }

        // Try direct ACTION_WEB_SEARCH first
        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, cleanQuery)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(webSearchIntent)
            SearchResult(true, "Searching for $cleanQuery.")
        } catch (e: Exception) {
            // Fallback to opening Google in default browser
            try {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(cleanQuery)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                SearchResult(true, "Searching for $cleanQuery.")
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening browser search", e2)
                SearchResult(false, "Could not open browser for search.")
            }
        }
    }
}
