package com.android.marker.viewModel.repositories

import android.app.Application
import androidx.lifecycle.LiveData
import com.android.marker.model.database.MarkersRoomDB
import com.android.marker.model.database.daos.MarkersDao
import com.android.marker.model.models.SavedMarkers
import timber.log.Timber
import javax.inject.Inject

class MarkersRepository @Inject constructor(application: Application) {

    private val markersDAO: MarkersDao = MarkersRoomDB.getDatabase(application).markerDao()

    init {
        Timber.d("${this.javaClass.name} init for the first time.")
    }

    fun getAllMarkersDataLD(): LiveData<List<SavedMarkers>> {
        return markersDAO.getAllSavedMarkersLD()
    }

    fun getMarkersByLatLongLiveData(lat: Double,lon:Double): LiveData<SavedMarkers?> {
        return markersDAO.getMarkerByLatLonLiveData(lat,lon)
    }

    companion object {
        private var INSTANCE: MarkersRepository? = null

        fun getInstance(application: Application): MarkersRepository = INSTANCE ?: kotlin.run {
            INSTANCE = MarkersRepository(application = application)
            INSTANCE!!
        }
    }
}
