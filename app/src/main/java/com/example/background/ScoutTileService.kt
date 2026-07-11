package com.example.background

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class ScoutTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        try {
            val tile = qsTile
            if (tile != null) {
                tile.state = Tile.STATE_ACTIVE
                tile.updateTile()
            }
        } catch (e: Exception) {
            Log.e("ScoutTileService", "Failed to set tile state in onStartListening", e)
        }
    }

    override fun onClick() {
        super.onClick()
        Log.d("ScoutTileService", "Quick Settings Tile tapped - performing background scout")
        try {
            val context = applicationContext
            BackgroundScoutHelper.performBackgroundScout(context, "Tile")
        } catch (e: Exception) {
            Log.e("ScoutTileService", "Failed to perform background scout from tile", e)
        }
    }
}
