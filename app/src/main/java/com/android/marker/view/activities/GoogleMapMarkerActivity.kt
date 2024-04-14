package com.android.marker.view.activities

import android.content.res.ColorStateList
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.observe
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.android.marker.R
import com.android.marker.databinding.ActivityGoogleMapMarkerBinding
import com.android.marker.databinding.DialogLayoutBinding
import com.android.marker.databinding.MarkersBottomsheetBinding
import com.android.marker.model.database.MarkersRoomDB
import com.android.marker.model.database.daos.MarkersDao
import com.android.marker.model.models.SavedMarkers
import com.android.marker.utils.formatLatLong
import com.android.marker.view.adapters.MarkersAdapter
import com.android.marker.viewModel.viewModels.MarkersViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.IOException

@AndroidEntryPoint
class GoogleMapMarkerActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnInfoWindowClickListener {
    lateinit var binding: ActivityGoogleMapMarkerBinding
    private var markersAdapter: MarkersAdapter? = null
    private val viewModel by viewModels<MarkersViewModel>()
    private lateinit var gMap: GoogleMap
    private lateinit var markerDao: MarkersDao
    private var allMarkersList: MutableList<SavedMarkers> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleMapMarkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get an instance of the MarkerDao
        markerDao = MarkersRoomDB.getDatabase(this).markerDao()

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        getAllMarker() //get all saved markers in DB

        viewModel.markerListLiveData.observe(this) { markerList ->
            allMarkersList.clear()
            allMarkersList = markerList as MutableList<SavedMarkers>
            Timber.v("GetAllMarkers ${allMarkersList}")

            gMap.clear()

            if (allMarkersList.isEmpty()) {
                gMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            23.016622339444567,
                            72.57339801639318
                        ), 0f
                    )//move camera to ahmedabad if no saved marker found
                )
            }

            allMarkersList.forEach {
                val location = LatLng(it.latitude, it.longitude)
                val markerOptions = MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                gMap.addMarker(markerOptions)
            }
        }

        //Bottom sheet to show all saved markers
        val bottomSheetBinding = MarkersBottomsheetBinding.inflate(layoutInflater)
        // Inflate the bottom sheet layout
        val bottomSheetView = bottomSheetBinding.root
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(bottomSheetView)

        binding.buttonViewAllMarkers.setOnClickListener {

            if (allMarkersList.isEmpty()) {
                bottomSheetBinding.noDataTv.visibility = View.VISIBLE
            } else {
                bottomSheetBinding.noDataTv.visibility = View.GONE
            }
            // Show the bottom sheet
            bottomSheetDialog.show()

            // Set up the RecyclerView with an adapter
            markersAdapter = MarkersAdapter(allMarkersList, onItemClick = {

                gMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            it.latitude,
                            it.longitude
                        ), 15f
                    )
                )

                bottomSheetDialog.dismiss()
            })
            bottomSheetBinding.recyclerViewMarkers.adapter = markersAdapter
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        // Set default marker when user clicks on the map
        gMap.setOnMapClickListener { latLng ->
            addDefaultMarker(latLng) // Add default marker at clicked location
        }

        // Set custom info window adapter
        gMap.setInfoWindowAdapter(CustomInfoWindowAdapter())

        gMap.setOnInfoWindowClickListener(this)
    }

    private fun addDefaultMarker(latLng: LatLng) {
        gMap.addMarker(MarkerOptions().position(latLng))
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        private val window: View = layoutInflater.inflate(R.layout.custom_marker_snippet, null)

        override fun getInfoContents(marker: Marker): View? {
            val snippetTextView = window.findViewById<TextView>(R.id.snippet_text)
            marker.position.let { latLng ->
                val address = getAddressFromLatLng(latLng)
                val markerWithSameCoordinates =
                    allMarkersList.find { it.latitude == latLng.latitude && it.longitude == latLng.longitude }
                if (markerWithSameCoordinates?.name.isNullOrBlank()) {
                    val formattedText = getString(
                        R.string.lat_long_address_values,
                        latLng.latitude.formatLatLong(6),
                        latLng.longitude.formatLatLong(6),
                        address
                    )
                    snippetTextView.text =
                        (HtmlCompat.fromHtml(formattedText, HtmlCompat.FROM_HTML_MODE_LEGACY))
                } else {
                    val formattedText = getString(
                        R.string.name_address_values,
                        markerWithSameCoordinates?.name,
                        markerWithSameCoordinates?.address
                    )
                    snippetTextView.text =
                        (HtmlCompat.fromHtml(formattedText, HtmlCompat.FROM_HTML_MODE_LEGACY))
                }
            }
            return window
        }


        override fun getInfoWindow(marker: Marker): View? {
            return null
        }
    }

    private fun getAddressFromLatLng(latLng: LatLng): String {
        try {
            val geocoder = Geocoder(this@GoogleMapMarkerActivity)
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                return address.getAddressLine(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return getString(R.string.address_not_found)
    }

    private fun saveMarker(markerEntity: SavedMarkers) {
        // Use CoroutineScope to perform database operations asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            markerDao.insertMarker(markerEntity)
        }
    }

    private fun getAllMarker() {
        // Use CoroutineScope to perform database operations asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            markerDao.getAllSavedMarkersLD()
        }
    }

    fun deleteMarkerByLatLng(latitude: Double, longitude: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            markerDao.deleteMarkerByLatLng(latitude, longitude)
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        showDialog(marker)
    }

    private fun showDeleteConfirmationDialog(marker: Marker) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.you_want_to_delete))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deleteMarkerByLatLng(marker.position.latitude, marker.position.longitude)
                Toasty.success(applicationContext, "Marker Deleted", Toasty.LENGTH_SHORT, true)
                    .show()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun showDialog(marker: Marker) {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogBinding = DialogLayoutBinding.inflate(layoutInflater)
        dialogBuilder.setView(dialogBinding.root)

        val title = dialogBinding.titleDetail
        val positiveButton = dialogBinding.saveButton
        val closeButton = dialogBinding.closeButton
        val name = dialogBinding.nameEdt
        val age = dialogBinding.ageEdt
        val relation = dialogBinding.relationEdt
        val latlong = dialogBinding.latlongEdt
        val address = dialogBinding.addressEdt

        val alertDialog = dialogBuilder.create()
        var addressFromLatLon: String
        title.text = getString(R.string.enter_person_details)
        val markerWithSameCoordinates =
            allMarkersList.find { it.latitude == marker.position.latitude && it.longitude == marker.position.longitude }

        if (!markerWithSameCoordinates?.name.isNullOrBlank()) { //Delete UI
            name.setText(markerWithSameCoordinates?.name.toString())
            age.setText(markerWithSameCoordinates?.age.toString())
            relation.setText(markerWithSameCoordinates?.relation.toString())
            name.isEnabled = false
            age.isEnabled = false
            relation.isEnabled = false
            title.text = getString(R.string.person_details)

            val color = ContextCompat.getColor(this, R.color.colorRed)
            val colorStateList = ColorStateList.valueOf(color)
            positiveButton.backgroundTintList = colorStateList
            positiveButton.text = getString(R.string.delete)
        }

        latlong.setText(
            getString(
                R.string.lat_long_regular,
                marker.position.latitude.formatLatLong(6),
                marker.position.longitude.formatLatLong(6)
            )
        )

        marker.position.let { latLng ->
            addressFromLatLon = getAddressFromLatLng(latLng)
        }
        address.setText(addressFromLatLon)

        latlong.isEnabled = false
        address.isEnabled = false

        positiveButton.setOnClickListener {
            if (!markerWithSameCoordinates?.name.isNullOrBlank()) {
                //Delete button
                showDeleteConfirmationDialog(marker)
                alertDialog.dismiss() // Dismiss the dialog after deletion

            } else {
                // Save a marker to the database
                if (!name.text.isNullOrBlank() && !age.text.isNullOrBlank() && !relation.text.isNullOrBlank()) {
                    val markerEntity = SavedMarkers(
                        name = name.text.toString().trim(),
                        age = age.text.toString().trim().toInt(),
                        relation = relation.text.toString().trim(),
                        latitude = marker.position.latitude,
                        longitude = marker.position.longitude,
                        address = addressFromLatLon
                    )
                    saveMarker(markerEntity)
                    Toasty.success(applicationContext, "Marker Saved", Toasty.LENGTH_SHORT, true)
                        .show()
                    alertDialog.dismiss()
                } else {
                    Toasty.error(
                        applicationContext,
                        getString(R.string.fill_details),
                        Toasty.LENGTH_SHORT,
                        true
                    ).show()
                }
            }
        }

        closeButton.setOnClickListener {
            // Handle negative button click (dismiss dialog)
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set the dialog background to transparent
        alertDialog.show()
    }
}