package com.example.ui

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class CachingTileProvider(
    context: Context,
    private val isDarkTheme: Boolean
) : TileProvider {

    private val tileWidth = 256
    private val tileHeight = 256
    private val cacheDir = File(context.cacheDir, "map_tiles")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val themeKey = if (isDarkTheme) "dark" else "light"
        val tileFileName = "tile_${themeKey}_${zoom}_${x}_${y}.png"
        val cacheFile = File(cacheDir, tileFileName)

        // 1. Check disk cache
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val bytes = cacheFile.readBytes()
                Log.d("CachingTileProvider", "Loaded tile from cache: $tileFileName")
                return Tile(tileWidth, tileHeight, bytes)
            } catch (e: Exception) {
                Log.e("CachingTileProvider", "Error reading tile from cache: $tileFileName", e)
            }
        }

        // 2. Fetch from network
        // We use CartoDB tiles as a clean, styled tile provider.
        val urlString = if (isDarkTheme) {
            "https://basemaps.cartocdn.com/dark_all/$zoom/$x/$y.png"
        } else {
            "https://basemaps.cartocdn.com/rastertiles/voyager/$zoom/$x/$y.png"
        }

        try {
            Log.d("CachingTileProvider", "Fetching tile from network: $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.useCaches = false
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val tileData = connection.inputStream.use { inputStream ->
                    val buffer = ByteArray(4096)
                    val outputStream = ByteArrayOutputStream()
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.toByteArray()
                }

                // Save to cache directory
                try {
                    FileOutputStream(cacheFile).use { fos ->
                        fos.write(tileData)
                    }
                    Log.d("CachingTileProvider", "Saved tile to cache: $tileFileName")
                } catch (e: Exception) {
                    Log.e("CachingTileProvider", "Error saving tile to cache: $tileFileName", e)
                }

                return Tile(tileWidth, tileHeight, tileData)
            }
        } catch (e: Exception) {
            Log.e("CachingTileProvider", "Network error fetching tile: $urlString", e)
        }

        // Return empty/NO_TILE or null to let Google Maps background show
        return TileProvider.NO_TILE
    }
}
