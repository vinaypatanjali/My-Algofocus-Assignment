package com.example.myassignment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "request";
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.android.hellosharedprefs";
    private TextView usernameTextView, emailTextView, addressTextView;
    private Button signoutButton;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates;
    private LocationRequest locationRequest;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        //requesting permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 1);


        //getting users saved details
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        String username = mPreferences.getString("username", "username");
        String email = mPreferences.getString("email", "email");


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        //fetching users location
        fetchLastLocation();


        usernameTextView = findViewById(R.id.username);
        emailTextView = findViewById(R.id.email);
        addressTextView = findViewById(R.id.address);
        signoutButton = findViewById(R.id.signout);


        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (R.id.signout == view.getId()) {
                    logout();
                }
            }
        });


        //setting users name and email
        usernameTextView.setText(username);
        emailTextView.setText(email);


        locationRequest = LocationRequest.create();
        locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setSmallestDisplacement(30);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //updating bundle values
        updateValuesFromBundle(savedInstanceState);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    addressTextView.setText(getCompleteAddressString(location.getLatitude(), location.getLongitude()));
                }
            }
        };

    }


    /**
     * fetching users location
     */
    private void fetchLastLocation() {


        if (ActivityCompat.checkSelfPermission(WelcomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(WelcomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(WelcomeActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            addressTextView.setText(getCompleteAddressString(location.getLatitude(), location.getLongitude()));
                            Log.e("LAST LOCATION: ", location.toString()); // You will get your last location here
                        }
                    }
                });

    }


    /**
     * updating bundle values
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }


    /**
     * Function to
     * stop updating users location
     */
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                requestingLocationUpdates);
        // ...
        super.onSaveInstanceState(outState);
    }


    /**
     * Function to show settings alert dialog.
     * On pressing the Yes button it will launch Settings Options and after enabling location users gets to login screen.
     * else it will logout the user
     */
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 5);


                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        showToast("Please enable GPS to use the app");
                        logout();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    /**
     * Function to
     * start updating location
     * if location is null
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 5) {
            logout();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // this.showToast("accepted");
                    final LocationManager manager = (LocationManager) getSystemService(WelcomeActivity.this.LOCATION_SERVICE);
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        buildAlertMessageNoGps();
                    }


                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

                        showToast("Permissions not granted");

                        return;
                    }


                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {

                                        String address = getCompleteAddressString(location.getLatitude(), location.getLongitude());

                                        addressTextView.setText(address);

                                    } else {
                                        startLocationUpdates();
                                    }
                                }
                            });

                } else {
                    showToast("Please allow Location Access to use the app");
                    logout();
                }
                return;

            }
            default:
                logout();
                break;

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /**
     * Function for
     * converting latitude
     * and longitude
     * into a address
     * using reverse geocoding
     */
    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
                Log.w("address", strReturnedAddress.toString());
            } else {
                Log.w("address", "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("address", "Canont get Address!");
        }
        return strAdd;
    }


    /**
     * Function to
     * show some message/toast
     * to user
     */
    public void showToast(String message) {
        Toast.makeText(WelcomeActivity.this, message, Toast.LENGTH_SHORT).show();
    }


    /**
     * Function to
     * logout user
     * from current session
     */
    public void logout() {

        //clearing the saved details on signout of user
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.clear();
        preferencesEditor.apply();


        FirebaseUser currentUser = MainActivity.mAuth.getCurrentUser();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth != null) {
            auth.signOut();
            LoginManager.getInstance().logOut();
        }


        if (currentUser != null||MainActivity.mAuth!=null) {
            MainActivity.mAuth.signOut();
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if(account!=null)
            FirebaseAuth.getInstance().signOut();



        if (MainActivity.mGoogleSignInClient != null) {
            MainActivity.mGoogleSignInClient.signOut();
        }


        showToast("Signed out");

        Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);

        startActivity(intent);
        finish();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }


}