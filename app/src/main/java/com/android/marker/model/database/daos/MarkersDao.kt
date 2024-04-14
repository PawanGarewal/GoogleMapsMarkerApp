package com.android.marker.model.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.android.marker.model.models.SavedMarkers

@Dao
interface MarkersDao {
    @Insert
    suspend fun insertMarker(marker: SavedMarkers)

    @Query("SELECT * FROM markers")
    fun getAllSavedMarkersLD(): LiveData<List<SavedMarkers>>

    @Query("SELECT * FROM markers WHERE latitude = :lat and longitude=:lon")
    fun getMarkerByLatLonLiveData(lat: Double,lon:Double): LiveData<SavedMarkers?>

    @Query("DELETE FROM markers WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun deleteMarkerByLatLng(latitude: Double, longitude: Double)
}