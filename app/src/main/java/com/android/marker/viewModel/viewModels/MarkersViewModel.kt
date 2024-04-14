package com.android.marker.viewModel.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import com.android.marker.model.models.SavedMarkers
import com.android.marker.viewModel.repositories.MarkersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MarkersViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val markerRepository = MarkersRepository.getInstance(application)

    val markerListLiveData: LiveData<List<SavedMarkers>> = liveData(Dispatchers.IO) {
        emitSource(markerRepository.getAllMarkersDataLD())
    }
}