package za.co.whatsyourvibe.user.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Timer;
import java.util.TimerTask;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import za.co.whatsyourvibe.user.R;
import za.co.whatsyourvibe.user.adapters.SlidingImageAdapter;
import za.co.whatsyourvibe.user.models.Event;

public class EventDetailsActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "EventDetailsActivity";

    private Button btnShareVibe, btnRateVibe, btnCheckIn;

    private TextView title, overview, date, time;

    private TextView share, going, rate, fee;

    private ImageView coverImage;

    private FirebaseFirestore db;

    private int iGoing, iShares;

    private GoogleMap mMap;

    private float lat, lng, myLat, myLng;

    private final static int REQUEST_CHECK_GPS = 123;

    private Location mLastKnownLocation;

    private GoogleApiClient googleApiClient;

    private String eventId;

    private float distance[] = new float[1];

    private Event event;

    private TextView mTitle;

    private static ViewPager mPager;

    private static int currentPage = 0;

    private static int NUM_PAGES = 0;

    private String[] urls;

    @Override
    protected void attachBaseContext(Context newBase) {

        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_details);

        Toolbar toolbar = findViewById(R.id.event_details_toolbar);

        mTitle = toolbar.findViewById(R.id.toolbar_title);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        initViews();

        setListeners();

        loadData();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.event_details_map);
        mapFragment.getMapAsync(this);

        setSupGoogleClient();

    }

    private void loadData() {

        event = (Event) getIntent().getSerializableExtra("EVENT");

        if (event !=null) {

            urls = new String[] {event.getImage1(),event.getImage2(),event.getImage3()};

            title.setText(event.getName());

            overview.setText(event.getDescription());

            date.setText(event.getDate());

            time.setText(event.getTime());

            iGoing = event.getGoing();

            iShares = event.getShares();

            eventId = event.getId();

            going.setText(iGoing +"");

            share.setText(iShares+"");

            rate.setText(event.getRate()+ "");

            fee.setText("R " + event.getEventEntryFee());

//            Glide.with(this)
//                    .load(event.getPoster())
//                    // .placeholder(placeholder)
//                    // .fitCenter()
//                    .into(coverImage);

            mTitle.setText(event.getName());

            lat = (float) event.getLatitude();

            lng  = (float) event.getLongitude();

            initSlider();
        }


    }

    private void initSlider() {

        mPager = findViewById(R.id.event_details_viewPager);
        mPager.setAdapter(new SlidingImageAdapter(EventDetailsActivity.this,urls));

        CirclePageIndicator indicator = findViewById(R.id.event_details_indicator);

        indicator.setViewPager(mPager);

        final float density = getResources().getDisplayMetrics().density;

        //Set circle indicator radius
        indicator.setRadius(5 * density);

        NUM_PAGES = urls.length;

        // Auto start of viewpager
        final Handler handler = new Handler();
        final Runnable Update = new Runnable() {
            public void run() {
                if (currentPage == NUM_PAGES) {
                    currentPage = 0;
                }
                mPager.setCurrentItem(currentPage++, true);
            }
        };
        Timer swipeTimer = new Timer();
        swipeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(Update);
            }
        }, 3000, 3000);

        // Pager listener over indicator
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                currentPage = position;

            }

            @Override
            public void onPageScrolled(int pos, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int pos) {

            }
        });

    }

    private void setListeners() {

        btnCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkIn();
            }
        });

        btnShareVibe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                shareVibe();

            }
        });


        btnRateVibe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                rateVibe();

            }
        });

    }

    private void shareVibe() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share This Vibe");
        builder.setMessage("Feature not available in this version.");

        // add a button
        builder.setPositiveButton("DISMISS", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void rateVibe() {

        Location.distanceBetween(
                lat,
                lng,
                myLat,
                myLng,
                distance);

        if ((int) distance[0]> 100) {

            // setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Rate Vibe");
            builder.setMessage("You are not at the event, you are not allowed to rate");

            // add a button
            builder.setPositiveButton("DISMISS", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();

        }else{

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            iGoing = iGoing + 1;

            db.collection("events")
                    .document(eventId)
                    .update("rate", iGoing)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            rate.setText(iGoing +"");

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(EventDetailsActivity.this, "An error occurred, please try again", Toast.LENGTH_SHORT).show();

                    iGoing = iGoing - 1;

                    rate.setText(iGoing +"");

                }
            });

        }

    }

    private void checkIn() {

        Location.distanceBetween(
                lat,
                lng,
                myLat,
                myLng,
                distance);

        if ((int) distance[0]> 100) {

            // setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Check In");
            builder.setMessage("You are not at the event, you are not allowed to check in");

            // add a button
            builder.setPositiveButton("DISMISS", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();

        }else{

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            iGoing = iGoing + 1;

            db.collection("events")
                    .document(eventId)
                    .update("going", iGoing)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            going.setText(iGoing +"");

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(EventDetailsActivity.this, "An error occurred, please try again", Toast.LENGTH_SHORT).show();

                    iGoing = iGoing - 1;

                    going.setText(iGoing +"");

                }
            });

        }


    }

    private void initViews() {

        btnCheckIn = findViewById(R.id.event_details_btnCheckIn);

        btnShareVibe = findViewById(R.id.event_details_btnShareVibe);

        btnRateVibe = findViewById(R.id.event_details_btnRateVibe);

        title = findViewById(R.id.event_details_title);

        overview = findViewById(R.id.event_details_overview);

        date = findViewById(R.id.event_details_date);

        time = findViewById(R.id.event_details_time);

        share = findViewById(R.id.event_details_tvShare);

        rate = findViewById(R.id.event_details_tvRate);

        going = findViewById(R.id.event_details_tvGoing);

        fee = findViewById(R.id.event_details_tvFee);

       // coverImage = findViewById(R.id.event_details_coverImage);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        LatLng eventLocation = new LatLng(lat, lng);

        float zoomLevel = 11.0f; //This goes up to 21

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.primary_marker);

        mMap.addMarker(new MarkerOptions()
                .icon(icon)
                .position(eventLocation)
                .title(event.getName()));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, zoomLevel));

    }

    private void setSupGoogleClient() {

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        getMyLocation();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastKnownLocation = location;

        if (mLastKnownLocation !=null) {

            myLat = (float) mLastKnownLocation.getLatitude();

            myLng = (float) mLastKnownLocation.getLongitude();
        }

    }

    private void getMyLocation(){
        if(googleApiClient!=null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(3000);
                    locationRequest.setFastestInterval(3000);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi
                            .requestLocationUpdates(googleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result =
                            LocationServices.SettingsApi
                                    .checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied.
                                    // You can initialize location requests here.
                                    int permissionLocation = ContextCompat
                                            .checkSelfPermission(getApplicationContext(),
                                                    Manifest.permission.ACCESS_FINE_LOCATION);
                                    if (permissionLocation == PackageManager.PERMISSION_GRANTED) {


                                        mLastKnownLocation = LocationServices.FusedLocationApi
                                                .getLastLocation(googleApiClient);


                                    }
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied.
                                    // But could be fixed by showing the user a dialog.
                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        // Ask to turn on GPS automatically
                                        status.startResolutionForResult(EventDetailsActivity.this,
                                                REQUEST_CHECK_GPS);


                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }


                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    // Location settings are not satisfied.
                                    // However, we have no way
                                    // to fix the
                                    // settings so we won't show the dialog.
                                    // finish();
                                    break;
                            }
                        }
                    });

                }
            }
        }
    }
}