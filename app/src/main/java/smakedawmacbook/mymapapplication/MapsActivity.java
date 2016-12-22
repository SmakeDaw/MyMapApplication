package smakedawmacbook.mymapapplication;

import android.app.ProgressDialog;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String TAG = MapsActivity.class.getSimpleName();

    private ProgressDialog processDialog;
    private ListView listview;

    String Lat = "";
    String Long = "";
    String Radius = "";
    // URL to get contacts from JSON
    private static String url = "";

    ArrayList<HashMap<String, String>> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        //Sorting List
        Button btsort = (Button) findViewById(R.id.button2);
        btsort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collections.sort(contactList, mapComparator);
                listview.invalidateViews();
            }
        });

        //Search
        Button btsearch = (Button) findViewById(R.id.button3);
        btsearch.setOnClickListener(new View.OnClickListener() {
            //รับค่า Latitude Longtitude และ Radius
            @Override
            public void onClick(View v) {
                EditText eLat = (EditText) findViewById(R.id.editText);
                EditText eLng = (EditText) findViewById(R.id.editText2);
                EditText eRad = (EditText) findViewById(R.id.editText3);

                Lat = eLat.getText().toString(); //"18.7717874";
                Long = eLng.getText().toString(); //"98.9742796";
                Radius = eRad.getText().toString(); //"500";
                url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + Lat + "," + Long + "&radius=" + Radius + "&key=AIzaSyCZ1BCe4Q7YL1nCa_ovtet4Bjn52tT20T8";
                contactList = new ArrayList<>();
                Log.e("SmakeDaw", contactList.toString());
                new GetContacts().execute();


            }
        });

        listview = (ListView) findViewById(R.id.list);

        Switch mySwitch = (Switch) findViewById(R.id.switch1);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if (isChecked) {
                    listview.setAlpha(0);
                } else {
                    listview.setAlpha(1);
                }
            }
        });
    }

    public Comparator<HashMap<String, String>> mapComparator = new Comparator<HashMap<String, String>>() {
        public int compare(HashMap<String, String> m1, HashMap<String, String> m2) {
            return m1.get("name").compareTo(m2.get("name"));
        }
    };

    /**
     * Async task class to get json by making HTTP call
     */
    public class GetContacts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            processDialog = new ProgressDialog(MapsActivity.this);
            processDialog.setMessage("Please wait...");
            processDialog.setCancelable(false);
            processDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray contacts = jsonObj.getJSONArray("results");

                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        // Geometry node is JSON Object
                        JSONObject geometry = c.getJSONObject("geometry");

                        // Location node is JSON Object
                        JSONObject location = geometry.getJSONObject("location");
                        String lat = location.getString("lat");
                        String lng = location.getString("lng");

                        String name = c.getString("name");
                        String id = c.getString("id");

                        Location location1 = new Location("");
                        location1.setLatitude(Double.valueOf(Lat));
                        location1.setLongitude(Double.valueOf(Long));

                        Location location2 = new Location("");
                        location2.setLatitude(Double.valueOf(lat));
                        location2.setLongitude(Double.valueOf(lng));

                        float distanceInMeters = location1.distanceTo(location2);
                        // tmp hash map for single contact
                        HashMap<String, String> contact = new HashMap<>();

                        // adding each child node to HashMap key => value
                        contact.put("id", id);
                        contact.put("name", name);
                        contact.put("lat", lat);
                        contact.put("lng", lng);
                        contact.put("distance", String.format( "Distance : %.2f KM", distanceInMeters/1000.0 ));

                        // adding contact to contact list
                        contactList.add(contact);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (processDialog.isShowing())
                processDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */

            ListAdapter adapter = new SimpleAdapter(
                    MapsActivity.this, contactList,
                    R.layout.list_item, new String[]{"name", "distance"}, new int[]{R.id.name,
                    R.id.distance});

            listview.setAdapter(adapter);
            addPin();
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
        mMap = googleMap;


        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }

    public void addPin() {
        for (int i = 0; i < contactList.size(); i++ ) {
            LatLng place = new LatLng(Double.valueOf(contactList.get(i).get("lat")), Double.valueOf(contactList.get(i).get("lng")));

            mMap.addMarker(new MarkerOptions().position(place).title(contactList.get(i).get("name")));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(place));
        }
    }
}