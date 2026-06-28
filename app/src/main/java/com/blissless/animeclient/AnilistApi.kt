package com.blissless.animeclient

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AnilistApi {
    private val client = OkHttpClient.Builder().build()
    private val url = "https://graphql.anilist.co"
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    data class AnimeInfo(val id: String, val englishName: String, val romajiName: String)

    fun searchAnime(query: String): AnimeInfo {
        val graphQL = $$"""
            query ($search: String) {
              Media(search: $search, type: ANIME) {
                id
                title { romaji english }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject()
            .put("query", graphQL)
            .put("variables", JSONObject().put("search", query))
            .toString()

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val rawJson = response.body?.string() ?: throw Exception("Anilist empty response")
            val data = JSONObject(rawJson).getJSONObject("data").getJSONObject("Media")

            val id = data.getString("id")
            val titles = data.getJSONObject("title")
            val english = titles.optString("english", query)
            val romaji = titles.optString("romaji", query)

            return AnimeInfo(id, english, romaji)
        }
    }
}