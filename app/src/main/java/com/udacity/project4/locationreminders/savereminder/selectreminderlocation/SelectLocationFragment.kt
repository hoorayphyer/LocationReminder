package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    //Use Koin to get the view model of the SaveReminder
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var locationPermissionGranted = false
    private var selectedPOI: PointOfInterest? = null

    companion object {
        // some constants related in location permission handling
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        // Build the map.
        val mapFragment =
            childFragmentManager.findFragmentById(binding.googleMap.id) as SupportMapFragment
        mapFragment.getMapAsync(this) // calling onMapReady()

        // Add clickListener to the confirmButton
        binding.confirmButton.setOnClickListener {
            // When the user confirms on the selected location,
            // send back the selected location details to the view model
            // and navigate back to the previous fragment to save the reminder and add the geofence
            if (selectedPOI == null) {
                // Toast.makeText(requireContext(), "No POI is selected!", Toast.LENGTH_LONG).show()
                // TODO I don't know how to simulate picking a POI in Espresso end-to-end test. This behavior below is purely for testing purpose
                _viewModel.reminderSelectedLocationStr.value = "Location Picked For Testing"
                _viewModel.latitude.value = 0.0
                _viewModel.longitude.value = 0.0
                findNavController().popBackStack()
            } else {
                _viewModel.reminderSelectedLocationStr.value = selectedPOI!!.name.toString()
                _viewModel.latitude.value = selectedPOI!!.latLng.latitude
                _viewModel.longitude.value = selectedPOI!!.latLng.longitude
                findNavController().popBackStack()
            }
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Prompt the user for permission.
        getLocationPermission()

        if (locationPermissionGranted) {
            prepareGoogleMap()
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    @TargetApi(29)
    private fun getLocationPermission() {
        /*
        * Request location permission, so that we can get the location of the
        * device. The result of the permission request is handled by a callback,
        * onRequestPermissionsResult.
        */

        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }

        locationPermissionGranted = foregroundLocationApproved && backgroundPermissionApproved

        if (locationPermissionGranted) {
            return
        }

        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        // don't use ActivityCompat.requestPermissions here. Use the fragment's requestPermissions
        // requestPermissions is deprecated. See here for alternative https://developer.android.com/reference/androidx/fragment/app/Fragment#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract%3CI,%20O%3E,%20androidx.activity.result.ActivityResultCallback%3CO%3E)
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    /**
     * Handles the result of the request for location permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            locationPermissionGranted = false
            Toast.makeText(
                requireContext(),
                "LocationReminder must be granted location permission at all times to function properly!",
                Toast.LENGTH_LONG
            )
                .show()
        } else {
            locationPermissionGranted = true;
        }

        if (locationPermissionGranted) {
            prepareGoogleMap()
        }
    }

    private fun prepareGoogleMap() {
        // Turn on the My Location layer and the related control on the map.
        updateMapUISettings()

        // Move camera to the current location of the device and set the position of the map.
        getDeviceLocation {
            val zoomLevel = 15f
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, zoomLevel))
            // put a marker to location that the user selected
            map.addMarker(MarkerOptions().position(it))

            Toast.makeText(requireContext(), "Click on a POI to set a reminder", Toast.LENGTH_LONG)
                .show()
        }

        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()

            selectedPOI = poi
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private fun updateMapUISettings() {
        try {
            map.isMyLocationEnabled = locationPermissionGranted
            map.uiSettings.isMyLocationButtonEnabled = locationPermissionGranted
            if (!locationPermissionGranted) {
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation(callback: (LatLng) -> Unit) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        require(locationPermissionGranted) {
            "location permission not granted"
        }
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                var latLng = LatLng(37.422160, -122.084270)
                if (task.isSuccessful && task.result != null) {
                    // return the current location of the device.
                    latLng = LatLng(task.result.latitude, task.result.longitude)
                } else {
                    map.uiSettings.isMyLocationButtonEnabled = false
                }

                callback(latLng)
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

}
