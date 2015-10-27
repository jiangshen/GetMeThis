package com.example.jiangshen.getmethis;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
    //private ArrayList<Marker> markers;
    private Map<String, Marker> markerMap;

    private String foodName;

    private static final int REQUEST_LOCATION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }

        // Get the message from the intent
        Intent intent = getIntent();
        this.foodName = intent.getStringExtra(GetMeThisMain.MAP_FOOD);

        setContentView(R.layout.activity_maps);
        items = new ArrayList<String>();
        //markers = new ArrayList<Marker>();
        markerMap = new HashMap<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        m_Location = getLocation();
        //new GetPlaces(this).execute();
        updateListView();

        //get the RecyclerView
        //RecyclerView rv = (RecyclerView)findViewById(R.id.rv);

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

        Toolbar mActionBar = (Toolbar) findViewById(R.id.toolbar);
        mActionBar.setTitle("Find your treat");
        mActionBar.setTitleTextColor(Color.WHITE);

        Window window = this.getWindow();
    }

    @Override
    public void onLocationChanged(Location location) {
//        TextView locationTv = (TextView) findViewById(R.id.Text);
        boolean bPass = (items.size() == 0);
        m_Location = location;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        if (bPass) {
            //new GetPlaces(this).execute();
            updateListView();
        }
//        LatLng latLng = new LatLng(latitude, longitude);
        //m_Map.addMarker(new MarkerOptions().position(latLng));
//        m_Map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        m_Map.animateCamera(CameraUpdateFactory.zoomTo(14));
//        locationTv.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
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

    public Location getLocation() {
        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
        m_LocationManager= (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = m_LocationManager.getBestProvider(criteria, true);
        Location location = m_LocationManager.getLastKnownLocation(bestProvider);
        m_LocationManager.requestLocationUpdates(bestProvider, 20000, 0, this);
//        m_Map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
//        m_Map.animateCamera(CameraUpdateFactory.zoomTo(14));
        return location;
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
        Location location = getLocation();

        if (location != null) {
            onLocationChanged(location);
            m_Map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12));
        }
    }


    class GetPlaces extends AsyncTask<Void, Void, ArrayList<Place>> {
        Context context;
        //private ListView listView;
        private ProgressDialog bar;
        public GetPlaces(Context context/*, ListView listView*/) {
            // TODO Auto-generated constructor stub
            this.context = context;
            //this.listView = listView;
        }

        @Override
        protected void onPostExecute(ArrayList<Place> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            bar.dismiss();
            items.clear();
            markerMap.clear();
            //markers.clear();
            //this.listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, m_Places));
            for (int i = 0; i < result.size(); i++) {
                Marker m = m_Map.addMarker(new MarkerOptions()
                        .title(result.get(i).getName())
                        .position(new LatLng(result.get(i).getLatitude(), result.get(i).getLongitude()))
                                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                        .snippet(result.get(i).getVicinity()));
                items.add(m.getTitle() + ", " + m.getSnippet());
                markerMap.put(m.getTitle() + ", " + m.getSnippet(), m);
                //markers.add(m);
            }
            updateListView();
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            bar =  new ProgressDialog(context);
            bar.setIndeterminate(true);
            bar.setTitle("Loading...");
            bar.show();
        }

        @Override
        protected ArrayList<Place> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            return findNearLocation();
            //return null;
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
