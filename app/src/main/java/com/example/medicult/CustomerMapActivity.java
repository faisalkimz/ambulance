package com.example.medicult;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout,mRequest,mSettings,mHistory;
    private LatLng pickupLocation;
    private Boolean requestBol = false;
    private Marker pickupMarker;

    private SupportMapFragment mapFragment;

    private String destination;

    private LatLng destinationLatlng;

    private RatingBar mRatingBar;

    private LinearLayout mDriverInfo;

    private TextView mDriverName, mDriverPhone, mDriverCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        destinationLatlng = new LatLng(0.0, 0.0);

        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);

        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);

        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);
        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
        mHistory = (Button) findViewById(R.id.history);
        mLogout = (Button) findViewById(R.id.logout);



        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, Welcome_Activity.class);
                startActivity(intent);
                finish();
                return;
            }

        });


        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBol){
                    endRide();


                }else {
                }
                requestBol = true;
                String userId=FirebaseAuth.getInstance().getCurrentUser().getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                pickupLocation = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.patient)));

                mRequest.setText("Getting Your Ambulance...");

                getClosestDriver();
            }
        });


        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }

        });


        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CustomerMapActivity.this,HistoryActivity.class);
                intent.putExtra("customerOrDriver","Customers");
                startActivity(intent);
                return;

            }
        });

/*
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);


        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destination = place.getName().toString();
                destinationLatlng = place.getLatLng();
            }
            @Override
            public void onError(Status status) {

            }
        });*/

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatlng = place.getLatLng();
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });

    }

    private int radius=1;
    private Boolean driverFound = false;
    private String driverFoundID;

    GeoQuery geoQuery;


    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol){
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                               // Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if (driverFound){
                                    return;
                                }


                                    driverFound = true;
                                    driverFoundID = dataSnapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideId", customerId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatlng.latitude);
                                    map.put("destinationLng", destinationLatlng.longitude);
                                    driverRef.updateChildren(map);

                                    getDriverLocation();
                                    getDriverInfo();
                                    getHasRideEnded();
                                    mRequest.setText("Looking for Driver's Location....");
                                }

                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
/*
    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire=new GeoFire(driverLocation);

        geoQuery=  geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId",customerId);
                    map.put("destination",destination);
                    map.put("destinationLat",destinationLatlng.latitude);
                    map.put("destinationLng",destinationLatlng.longitude);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    getHasRideEnded();
                    mRequest.setText("Looking for Ambulance Location...");
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }*/
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");
        driverLocationRefListener=driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol){
                    List<Object> map=(List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;
                   // mRequest.setText("Ambulance Found");
                    if(map.get(0)!= null){
                        LocationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1)!= null){
                        LocationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(LocationLat, LocationLng);
                    if(mDriverMarker!=null){
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if(distance<100){
                        mRequest.setText("Ambulance Arrived");
                    }else{
                        int dis = (int)distance/1000;
                        mRequest.setText("Ambulance Found: "+ String.valueOf(dis)+ " Kms away...");

                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Ambulance").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ambulance)));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    if(dataSnapshot.child("Name")!=null){
                        mDriverName.setText(dataSnapshot.child("Name").getValue().toString());
                    }
                    if(dataSnapshot.child("Phone")!=null){
                        mDriverPhone.setText(dataSnapshot.child("Phone").getValue().toString());
                    }
                    if(dataSnapshot.child("Car")!=null){
                        mDriverCar.setText(dataSnapshot.child("Car").getValue().toString());
                    }

                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if(ratingsTotal!= 0){
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {

                mLastLocation = location;

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                if(!getDriversAroundStarted)
                    getDriversAround();

            }
        }

    };

    boolean getDriversAroundStarted = false;
    List<Marker> markers = new ArrayList<Marker>();
    private void getDriversAround(){
        getDriversAroundStarted = true;
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude()), 999999999);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {



                    LatLng driverLocation = new LatLng(location.latitude, location.longitude);

                    Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ambulance)));
                    mDriverMarker.setTag(key);

                    markers.add(mDriverMarker);

                    for(Marker markerIt : markers){
                        if(markerIt.getTag().equals(key))
                            return;
                    }





            }

            @Override
            public void onKeyExited(String key) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.remove();
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                new  android.app.AlertDialog.Builder(this)
                        .setTitle("Please give permission...")
                        .setMessage("Please give permission...")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission...", Toast.LENGTH_LONG).show();
                }
                break;
            }


        }}

        private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;
    private void getHasRideEnded(){
       // String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driveHasEndedRef  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){


                }else{

                    endRide();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void endRide()
    {

        requestBol = false;
          if(geoQuery != null){
              geoQuery.removeAllListeners();
          }
          if(driverLocationRefListener != null&& driveHasEndedRefListener != null){
              driverLocationRef.removeEventListener(driverLocationRefListener);
              driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
          }


        if(driverFoundID!=null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
            driverRef.removeValue();
            driverFoundID = null;

        }
        driverFound = false;
        radius = 1;

        String userId=FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
        if( pickupMarker!=null){
            pickupMarker.remove();
        }
        if (mDriverMarker != null){
            mDriverMarker.remove();
        }
        mRequest.setText("Request An Ambulance");

        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");

    }


   }