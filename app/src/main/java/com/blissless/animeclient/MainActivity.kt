package com.blissless.animeclient

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import org.json.JSONObject
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var extensionSpinner: Spinner
    private lateinit var animeInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultText: TextView

    private var selectedAuthority: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "Tensei Scraper"

        fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()

        // 1. Root Layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24))
            setBackgroundColor("#F0F2F5".toColorInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 2. Dynamically Find Installed Extensions
        val beaconIntent = Intent("com.blissless.animeclient.EXTENSION_BEACON")
        val resolveInfoList = packageManager.queryBroadcastReceivers(beaconIntent, 0)

        val extensionNames = mutableListOf<String>()
        val extensionAuthorities = mutableListOf<String>()

        for (info in resolveInfoList) {
            val packageName = info.activityInfo.packageName
            val label = info.loadLabel(packageManager).toString()

            if (label.startsWith("Tensei: ", ignoreCase = true)) {
                extensionNames.add(label)
                extensionAuthorities.add("$packageName.provider")
            }
        }

        // 3. Extension Selector Label
        val selectLabel = TextView(this).apply {
            text = "Select Extension:"
            textSize = 14f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
        }

        // 4. Dropdown Spinner
        extensionSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(24) }
        }

        if (extensionNames.isEmpty()) {
            extensionNames.add("No extensions installed")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, extensionNames)
        extensionSpinner.adapter = adapter

        // 5. Input Field
        animeInput = EditText(this).apply {
            hint = "Enter anime (e.g., blue lock)"
            inputType = InputType.TYPE_CLASS_TEXT
            setBackgroundResource(android.R.drawable.editbox_background_normal)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }
        }

        // 6. Search Button
        searchButton = Button(this).apply {
            text = "Get Magnets"
            setBackgroundColor("#6200EE".toColorInt())
            setTextColor(Color.WHITE)
            textSize = 16f
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(24) }
        }

        // 7. Result Text
        resultText = TextView(this).apply {
            text = "Waiting for search..."
            textSize = 14f
            setTextColor("#333333".toColorInt())
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            typeface = Typeface.MONOSPACE
            visibility = View.GONE
        }

        // 8. Scroll View
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            addView(resultText)
        }

        // 9. Add views to root
        rootLayout.addView(selectLabel)
        rootLayout.addView(extensionSpinner)
        rootLayout.addView(animeInput)
        rootLayout.addView(searchButton)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)

        // 10. Handle Extension Selection
        extensionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (extensionAuthorities.isNotEmpty()) {
                    selectedAuthority = extensionAuthorities[position]
                    animeInput.visibility = View.VISIBLE
                    searchButton.visibility = View.VISIBLE
                    resultText.visibility = View.VISIBLE
                    resultText.text = "Ready to search."
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 11. Click Listener
        searchButton.setOnClickListener {
            val query = animeInput.text.toString().trim()
            val authority = selectedAuthority
            if (query.isNotEmpty() && authority != null) {
                resultText.text = "Fetching Anilist data for '$query'..."

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // 1. Fetch Anilist Data
                        val animeInfo = AnilistApi.searchAnime(query)

                        withContext(Dispatchers.Main) {
                            resultText.text = "Scraping extension for:\n${animeInfo.englishName}..."
                        }

                        // 2. Pass BOTH name and ID to the extension
                        fetchMagnets(authority, animeInfo)

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            resultText.text = "Anilist Error: ${e.message}"
                        }
                    }
                }
            }
        }
    }

    private fun fetchMagnets(authority: String, animeInfo: AnilistApi.AnimeInfo) {
        val providerUri: Uri = "content://$authority/scrape".toUri()

        lifecycleScope.launch(Dispatchers.IO) {
            var jsonData: String? = null
            var cursor: Cursor? = null

            try {
                val queryUri = providerUri.buildUpon()
                    .appendQueryParameter("anime", animeInfo.englishName)
                    .appendQueryParameter("anilistId", animeInfo.id)
                    .build()

                Log.d("MainAppDebug", "Querying URI: $queryUri")
                cursor = contentResolver.query(queryUri, null, null, null, null)

                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex("data")
                    if (columnIndex != -1) {
                        jsonData = cursor.getString(columnIndex)
                        Log.d("MainAppDebug", "JSON Data: $jsonData")
                    }
                } else {
                    Log.e("MainAppDebug", "Cursor is null or empty.")
                }
            } catch (e: Exception) {
                Log.e("MainAppError", "ContentProvider query crashed", e)
                withContext(Dispatchers.Main) {
                    resultText.text = "Error: ${e.message}"
                }
                return@launch
            } finally {
                cursor?.close()
            }

            val displayText = if (jsonData != null) {
                parseAndFormatJson(jsonData)
            } else {
                "No data returned. Check Logcat."
            }

            withContext(Dispatchers.Main) {
                resultText.text = displayText
            }
        }
    }

    private fun parseAndFormatJson(jsonData: String): String {
        return try {
            // Try parsing as a JSONObject first (SubsPlease style & Errors)
            val jsonObject = JSONObject(jsonData)

            if (jsonObject.has("error")) {
                return "Extension Error:\n${jsonObject.getString("error")}"
            }

            val builder = StringBuilder()
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val epNum = keys.next()
                val qualities = jsonObject.getJSONObject(epNum)
                builder.append("Episode $epNum:\n")

                val qKeys = qualities.keys()
                while (qKeys.hasNext()) {
                    val q = qKeys.next()
                    builder.append("  $q:\n  ${qualities.getString(q)}\n\n")
                }
            }

            if (builder.isEmpty()) return "No episodes found."
            builder.toString()

        } catch (_: org.json.JSONException) {
            // If it fails as a JSONObject, try parsing as a JSONArray (SeaDex style)
            return try {
                val jsonArray = JSONArray(jsonData)
                if (jsonArray.length() == 0) return "No magnets found."

                val builder = StringBuilder()
                for (i in 0 until jsonArray.length()) {
                    builder.append("Magnet ${i + 1}:\n  ${jsonArray.getString(i)}\n\n")
                }
                builder.toString()
            } catch (e2: Exception) {
                "Failed to parse JSON: ${e2.message}"
            }
        } catch (e: Exception) {
            "Failed to parse JSON: ${e.message}"
        }
    }
}