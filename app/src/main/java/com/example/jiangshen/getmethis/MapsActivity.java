package com.example.jiangshen.getmethis;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap m_Map;
    private LocationManager m_LocationManager;
    private String[] m_Places;
    private String[] m_URL;
    private Location m_Location;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> items;
    private Map<String, Marker> markerMap;

    private String foodName;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Get the message from the intent
        Intent intent = getIntent();
        this.foodName = intent.getStringExtra(GetMeThisMain.MAP_FOOD);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        items = new ArrayList<>();
        markerMap = new HashMap<>();

        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Marker m = markerMap.get(items.get(position));
                m.showInfoWindow();
                LatLng markerPosition = m.getPosition();
                m_Map.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 16));
            }
        });

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                updateLocation();
                m_Location = getLocation();
                zoomToCurrLocation();
            }
        });

        checkPhoneLocationProvided();
        updateListView();
        updateLocation();
        m_Location = getLocation();
        zoomToCurrLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
        boolean bPass = (items.size() == 0);
        m_Location = location;
        if (bPass) {
            updateListView();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    public void updateListView() {
        boolean bPass = (items.size() < 1);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        adapter.notifyDataSetChanged();

        ListView oList = (ListView) findViewById(R.id.listView);
        oList.setAdapter(adapter);
        if (bPass) {
            new GetPlaces(this).execute();
        }

    }

    private void updateLocation() {
        if (m_Map == null) {
            m_Map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                m_Map.setMyLocationEnabled(true);
            } else {
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
            if (m_Map != null) {
                m_Map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location location) {
                        m_Location = location;
                    }
                });

            }
        }
    }

    public Location getLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        m_LocationManager= (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = m_LocationManager.getBestProvider(criteria, true);
        Location location = m_LocationManager.getLastKnownLocation(bestProvider);
        return location;
    }

    private void zoomToCurrLocation() {
        if (m_Location != null) {
            LatLng target = new LatLng(m_Location.getLatitude(), m_Location.getLongitude());
            CameraPosition.Builder builder = new CameraPosition.Builder();
            builder.zoom(13);
            builder.target(target);
            this.m_Map.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
        }
    }

    private void checkPhoneLocationProvided() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(MapsActivity.this);
            dialog.setTitle("You location required");
            dialog.setMessage("Please enable your location to see the list of our results");
            dialog.setPositiveButton("Open Location Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Map", "Permission granted");
                    updateLocation();
                    m_Location = getLocation();
                    zoomToCurrLocation();
                } else {
                    Log.d("Map", "Permission denied");
                }
                return;
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        m_Map = googleMap;
        m_Map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        m_Map.setMyLocationEnabled(true);

        m_Map.getUiSettings().setMapToolbarEnabled(true);
        m_Map.getUiSettings().setMyLocationButtonEnabled(false);
        m_Map.getUiSettings().setZoomControlsEnabled(true);

        Location location = getLocation();

        if (location != null) {
            onLocationChanged(location);
            m_Map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12));
        }
    }


    private class GetPlaces extends AsyncTask<Void, Void, ArrayList<Place>> {
        Context context;
        private ProgressDialog bar;
        public GetPlaces(Context context) {
            this.context = context;
        }

        @Override
        protected void onPostExecute(ArrayList<Place> result) {
            super.onPostExecute(result);
            bar.dismiss();
            items.clear();
            markerMap.clear();
            for (int i = 0; i < result.size(); i++) {
                Marker m = m_Map.addMarker(new MarkerOptions()
                        .title(result.get(i).getName())
                        .position(new LatLng(result.get(i).getLatitude(), result.get(i).getLongitude()))
                        .snippet(result.get(i).getVicinity()));
                items.add(m.getTitle());
                markerMap.put(m.getTitle(), m);
            }
            updateListView();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar =  new ProgressDialog(context);
            bar.setIndeterminate(true);
            bar.setTitle("Loading...");
            bar.show();
        }

        @Override
        protected ArrayList<Place> doInBackground(Void... arg0) {
            return findNearLocation();
        }

    }

    public ArrayList<Place> findNearLocation()   {

        PlacesService service = new PlacesService("AIzaSyDWy1BBZPKzuXcEd_aD32Uqu4CipbbMcC0");
        /*
        Here you should call the method find nearest place near to central park new delhi
        then we pass the lat and lang of central park. here you can pass your current location lat and long.
        The third argument is used to set the specific place if you pass the atm the it will return the list of nearest atm list.
        If you want to get the every thing then you should be pass "" only
        */
        /* here you should be pass the you current location latitude and longitude, */
        List<Place> findPlaces = new ArrayList<>();
        if (null != m_Location) {
            findPlaces = service.findPlaces(m_Location.getLatitude(), m_Location.getLongitude(), foodName);
            m_Places = new String[findPlaces.size()];
            m_URL = new String[findPlaces.size()];

            for (int i = 0; i < findPlaces.size(); i++) {
                Place placeDetail = findPlaces.get(i);
                placeDetail.getIcon();
                System.out.println(placeDetail.getName());
                m_Places[i] =placeDetail.getName();
                m_URL[i] =placeDetail.getIcon();
            }
        }
        return (ArrayList<Place>)findPlaces;
    }
}
