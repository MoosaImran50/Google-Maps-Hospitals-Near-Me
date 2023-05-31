package com.example.carepro

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.carepro.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object{
        private  const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        setUpMap()
        findHospitals()
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null){
                val currentLatLong = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f))
            }
        }
    }

    private fun findHospitals() {
        // Use the Places API to search for nearby hospitals
        Places.initialize(applicationContext, "AIzaSyB6DN98YbbcNAYK2aTGZc6No0sNjs5DpbE")
        val placesClient = Places.createClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentLocation = task.result
                val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                val radius = 5000 // in meters
                val type = "hospital"

                val bounds = LatLngBounds.builder()
                    .include(LatLng(latLng.latitude - 0.05, latLng.longitude - 0.05))
                    .include(LatLng(latLng.latitude + 0.05, latLng.longitude + 0.05))
                    .build()

                val request = FindAutocompletePredictionsRequest.builder()
                    .setLocationBias(RectangularBounds.newInstance(bounds))
                    .setTypeFilter(TypeFilter.ESTABLISHMENT)
                    .setQuery(type)
                    .build()

                placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                    for (prediction in response.autocompletePredictions) {
                        val placeId = prediction.placeId
                        val placeFields = listOf(
                            Place.Field.NAME,
                            Place.Field.LAT_LNG,
                            Place.Field.ADDRESS
                        )

                        // Retrieve the location data for each hospital and add a marker to the map
                        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
                        placesClient.fetchPlace(request).addOnSuccessListener { response ->
                            val place = response.place
                            val markerOptions = MarkerOptions().position(place.latLng!!)
                            markerOptions.title(place.name)
                            mMap.addMarker(markerOptions)
                        }
                    }

                }

            }

        }

    }

}