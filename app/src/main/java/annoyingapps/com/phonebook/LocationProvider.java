package annoyingapps.com.phonebook;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

/*---------- Listener class to get coordinates ------------- */
public class LocationProvider {
    Context mAppContext;
    public LocationProvider(Context argAppContext){
        Longitude = null;
        Latitude = null;
        mAppContext = argAppContext;
        GetLocation();
    }

    private void GetLocation(){
        if (BuildConfig.DEBUG) {
            new MockLocationProvider(LocationManager.GPS_PROVIDER, mAppContext).pushLocation(375555.029869, -7655555.345222);
        }

        LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) { Log.w("PhoneBook","onLocationChanged Method Called!"); CreateUserLocationInfo(location); }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Location lcMobileLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lcMobileLocation != null) { Log.w("PhoneBook", "Location Exists!"); CreateUserLocationInfo(lcMobileLocation); }
    }

    private void CreateUserLocationInfo(Location argLocation){
        Longitude = argLocation.getLongitude();
        Latitude = argLocation.getLatitude();
    }
    public Double Longitude;
    public Double Latitude;
}