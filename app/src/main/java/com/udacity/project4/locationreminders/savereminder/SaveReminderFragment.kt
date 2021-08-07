package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private var backgroundPermissionApproved = false

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back calling
        // addGeofences() and removeGeofences() on geofencingClient
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // a temporary variable to hold the reminder variable set by the user and stored in view model
    private var reminderFromViewModel = ReminderDataItem("", "", "", 0.0, 0.0);

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            backgroundPermissionApproved = granted;
            if (granted) {
                doThisWhenBackgroundLocationPermissionIsGranted()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failure: LocationReminder must be granted background location permission at all times to function properly!",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

    private fun doThisWhenBackgroundLocationPermissionIsGranted() {
        checkDeviceLocationSettings(true)
    }

    @SuppressLint("MissingPermission", "LongLogTag")
    private fun doThisWhenDeviceLocationIsEnabled() {
        val geofenceRequest = createGeofenceRequest(reminderFromViewModel)

        // Add request to geofencingClient
        geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.saveReminder(reminderFromViewModel)
            }
            addOnFailureListener {
                Toast.makeText(
                    requireContext(), "Failed to add a geofence",
                    Toast.LENGTH_SHORT
                ).show()
                if (it.message != null) {
                    Log.w(GeofencingConstants.LOGTAG, it.message!!)
                }
            }
        }
    }

    private val requestDeviceLocationOn = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // recurve this function to deal with user's decisions. We pass false so that this branch is not hit again, so users will not be bothered a second time
        checkDeviceLocationSettings(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            reminderFromViewModel = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )

            if (!_viewModel.validateEnteredData(reminderFromViewModel)) {
                Toast.makeText(
                    requireContext(), "ERROR: Invalid reminder data!!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // this has async call
            backgroundPermissionApproved = getBackgroundLocationPermission()

            if (backgroundPermissionApproved) {
                doThisWhenBackgroundLocationPermissionIsGranted()
                return@setOnClickListener
            }

            // don't use ActivityCompat.requestPermissions here. Use the fragment's requestPermissions
            // requestPermissions is deprecated. See here for alternative https://developer.android.com/reference/androidx/fragment/app/Fragment#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract%3CI,%20O%3E,%20androidx.activity.result.ActivityResultCallback%3CO%3E)
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun createGeofenceRequest(
        reminder: ReminderDataItem
    ): GeofencingRequest {
        val requestID = System.currentTimeMillis().toString()
        val longitude = reminder.longitude!!
        val latitude = reminder.latitude!!

        // Build a geofence object
        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(requestID)
            // Set the circular region of this geofence.
            .setCircularRegion(
                latitude,
                longitude,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // Set the transition types of interest. Alerts are only generated for these
            // transition.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            // Use a larger responsiveness to reduce power consumption. Unit is ms
            // See https://developer.android.com/training/location/geofencing.html#reduce-power-consumption
            .setNotificationResponsiveness(300000)

            // Create the geofence.
            .build()

        // Create a geofence request
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()
    }

    @TargetApi(29)
    private fun getBackgroundLocationPermission(): Boolean {
        /*
        * Request location permission, so that we can get the location of the
        * device.
        */
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
    }

    // it's important that the user's device has location turned on
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    // this prompts user to turn on device location
                    // the following uses the new api documented at https://developer.android.com/training/basics/intents/result#kotlin
                    // also see https://stackoverflow.com/a/67355829
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    requestDeviceLocationOn.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(
                        "SaveReminderFragment",
                        "Error getting location settings resolution: " + sendEx.message
                    )
                }
            } else {
                Snackbar.make(
                    binding.saveReminder,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                // at this point, we know that the location is on. Right after this, we add a geofence
                doThisWhenDeviceLocationIsEnabled()
            }
        }
    }
}
