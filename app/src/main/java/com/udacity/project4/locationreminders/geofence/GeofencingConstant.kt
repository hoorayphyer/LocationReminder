package com.udacity.project4.locationreminders.geofence

internal object GeofencingConstants {
    /**
     * Used to set an expiration time for a geofence. After this amount of time, Location services
     * stops tracking the geofence. For this sample, geofences expire after one hour.
     */
    const val GEOFENCE_RADIUS_IN_METERS = 100f
    const val LOGTAG = "LocationReminderGeofence"
}

