package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;


import org.jetbrains.annotations.NotNull;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    Context ctx;
    IMapController mapController;
    List<GetAllCamerasQuery.Camera> cameras = new ArrayList<>();
    List<GetAllWeatherStationsQuery.WeatherStation> weatherStations = new ArrayList<>();
    Boolean[] menuItem = {true,true,true,true};
    ArrayList<Marker> weatherMarkerList = new ArrayList<>();
    ArrayList<Marker> cameraMarkerList = new ArrayList<>();
    ArrayList<Marker> weatherCamMarkerList = new ArrayList<>();
    ArrayList<Marker> busMarkerList = new ArrayList<>();

    private RotationGestureOverlay mRotationGestureOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getAllWeatherStations();
        getAllCameras();

        connectToAPI();
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(14.0);
        GeoPoint startPoint = new GeoPoint(65.012615, 25.471453);
        mapController.setCenter(startPoint);

        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);

        //kartan pyörittäminen sormilla
        mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(this.mRotationGestureOverlay);

        requestPermissionsIfNecessary(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //Items on map switch
       switch (item.getItemId()){
           case R.id.kamerat:
               item.setChecked(!item.isChecked());
               if (item.isChecked()){
                   menuItem[0] = true;
                   createWeatherCameraItems();
                   createCameraItems();

               }
               if (!item.isChecked()){
                    menuItem[0] = false;
                   for (int i=0; i<cameraMarkerList.size(); i++){
                       cameraMarkerList.get(i).remove(map);
                   }
               }
               break;
           case R.id.sääasemat:
               item.setChecked(!item.isChecked());
               if (item.isChecked()){
                   menuItem[1] = true;
                   createWeatherItems();
                   createWeatherCameraItems();
               }
               if (!item.isChecked()){
                   menuItem[1] = false;
                   for (int i=0; i<weatherMarkerList.size(); i++){
                       weatherMarkerList.get(i).remove(map);
                   }
               }

               break;
           case R.id.bussit:
               item.setChecked(!item.isChecked());
               if(item.isChecked()){
                   menuItem[3] = true;
                   getBusStops();
               }
               if(!item.isChecked()){
                   menuItem[3] = false;
                   for (int i=0; i<busMarkerList.size();i++){
                       busMarkerList.get(i).remove(map);
                   }
               }
       }
       if (!menuItem[0] && !menuItem[1]){
           for (int i=0; i<weatherCamMarkerList.size(); i++){
               weatherCamMarkerList.get(i).remove(map);
           }
       }

        return super.onOptionsItemSelected(item);
    }

    private  void createWeatherCameraItems(){
        //weathercamera creation
        if(menuItem[1] || menuItem[0]) {
            for (int i = 0; i < cameras.size(); i++) {

                //camera latitude and longitude
                double cLat = cameras.get(i).lat;
                double cLon = cameras.get(i).lon;

                //go through all waetherstations
                for (int j = 0; j < weatherStations.size(); j++) {

                    //weatherstation latitude and longitude

                    int tempIndex = -1;
                    int moistureIndex = -1;
                    int windIndex = -1;
                    GeoPoint cameraItemPoint = new GeoPoint(cLat, cLon);

                    if (cameraItemPoint.distanceToAsDouble(new GeoPoint(weatherStations.get(j).lat, weatherStations.get(j).lon)) < 50 ) { // weathercamera

                        String[]  cameraDirection, cameraTime;
                        ArrayList<String> cameraURL = new ArrayList<>();
                        double temp, wind, moisture;
                        StringBuffer stringBuffer = new StringBuffer();
                        cameraDirection = new String[cameras.get(i).presets.size()];
                        cameraTime = new String[cameras.get(i).presets.size()];




                        //go trough camera info
                        for (int k = 0; k < cameras.get(i).presets.size(); k++) {
                            //get info about camera
                            if (cameras.get(i).presets.get(k) != null){
                                cameraDirection[k] = cameras.get(i).presets.get(k).presentationName;
                                cameraURL.add(cameras.get(i).presets.get(k).imageUrl);
                                cameraTime[k] = cameras.get(i).presets.get(k).measuredTime;
                                if (cameraDirection[k] != null){
                                    stringBuffer.append(cameraDirection[k] + "dt" + cameraTime[k] + "SPLIT");
                                }
                            }

                        }

                        //go trough weatherstation sensor values and get index of wanted measurements
                        for (int w = 0; w < weatherStations.get(j).sensorValues.size(); w++) {
                            if (weatherStations.get(j).sensorValues.get(w).name.contains("ILMA") && !weatherStations.get(j).sensorValues.get(w).name.contains("_")) {
                                tempIndex = w;
                            }
                            if (weatherStations.get(j).sensorValues.get(w).name.contains("KESKITUULI")) {
                                windIndex = w;
                            }
                            if (weatherStations.get(j).sensorValues.get(w).name.contains("ILMAN_KOSTEUS")) {
                                moistureIndex = w;
                            }

                        }

                        //get measurements

                        if (moistureIndex != -1){
                            moisture = weatherStations.get(j).sensorValues.get(moistureIndex).sensorValue;
                            stringBuffer.append(moisture+"%" + "WSPL");
                        }else {
                            stringBuffer.append("NODATA" + "WSPL");
                        }
                        if (windIndex != -1){
                            wind = weatherStations.get(j).sensorValues.get(windIndex).sensorValue;
                            stringBuffer.append(wind+ "m/s" + "WSPL");
                        }else {
                            stringBuffer.append("NODATA" + "WSPL");
                        }
                        if (tempIndex != -1){
                            temp = weatherStations.get(j).sensorValues.get(tempIndex).sensorValue;
                            stringBuffer.append(temp + "°C"+"WSPL");
                        }else {
                            stringBuffer.append("NODATA" + "WSPL");
                        }


                        String directionTimeWeather = stringBuffer.toString();

                        //add marker to the map and to the marker list

                        weatherCamMarkerList.add(addMarkerWeatherCamera(cameraItemPoint, cameras.get(i).name, directionTimeWeather, cameraURL));
                        break;
                    }

                }
            }
        }




    }
    public void createWeatherItems(){
        if (menuItem[1]){
            //go trough every weatherstation
            for (int j = 0; j < weatherStations.size(); j++) {
                boolean exists = false;
                GeoPoint weatherItemPoint = new GeoPoint(weatherStations.get(j).lat, weatherStations.get(j).lon);

                //check if weathercamera exists
                for (int k = 0; k < weatherCamMarkerList.size(); k++) {
                    if (weatherCamMarkerList.get(k).getPosition().distanceToAsDouble(weatherItemPoint) < 50) {
                        exists = true;
                        break;
                    }
                }
                //if weathercamera does not exist create weatherstation
                if (!exists) {
                    StringBuffer stringBuffer = new StringBuffer();
                    double temp, wind, moisture;
                    int tempIndex = -1;
                    int moistureIndex = -1;
                    int windIndex = -1;

                    //go trough weatherstation sensor values and get index of wanted measurements
                    for (int w = 0; w < weatherStations.get(j).sensorValues.size(); w++){
                        if (weatherStations.get(j).sensorValues.get(w).name.contains("ILMA") && !weatherStations.get(j).sensorValues.get(w).name.contains("_")){
                            tempIndex = w;
                        }
                        if (weatherStations.get(j).sensorValues.get(w).name.contains("ILMAN_KOSTEUS") ){
                            moistureIndex = w;
                        }
                        if (weatherStations.get(j).sensorValues.get(w).name.contains("KESKITUULI")){
                            windIndex = w;
                        }
                    }

                    if (moistureIndex != -1){
                        moisture = weatherStations.get(j).sensorValues.get(moistureIndex).sensorValue;
                        stringBuffer.append(moisture+"%" + "SPLIT");
                    }else {
                        stringBuffer.append("NODATA" + "SPLIT");
                    }
                    if (windIndex != -1){
                        wind = weatherStations.get(j).sensorValues.get(windIndex).sensorValue;
                        stringBuffer.append(wind+ "m/s" + "SPLIT");
                    }else {
                        stringBuffer.append("NODATA" + "SPLIT");
                    }
                    if (tempIndex != -1){
                        temp = weatherStations.get(j).sensorValues.get(tempIndex).sensorValue;
                        stringBuffer.append(temp + "°C"+"SPLIT");
                    }else {
                        stringBuffer.append("NODATA" + "SPLIT");
                    }

                    //put all data on a single string
                    String weatherTime = stringBuffer.toString() +weatherStations.get(j).measuredTime.toString();

                    //add marker to the map and to the marker list
                    weatherMarkerList.add(addMarkerWeather(weatherItemPoint, weatherStations.get(j).name, weatherTime));
                }
            }
        }
    }


    public void createCameraItems(){
        if( menuItem[0]) {
            for (int i = 0; i < cameras.size(); i++) {
                boolean exists = false;
                GeoPoint cameraGeoPoint = new GeoPoint(cameras.get(i).lat, cameras.get(i).lon);

                //check if weathercamera exists
                for (int k = 0; k < weatherCamMarkerList.size(); k++){
                    if (weatherCamMarkerList.get(k).getPosition().distanceToAsDouble(cameraGeoPoint) == 0){
                        exists = true;
                        break;
                    }
                }

                //if weathercamera does not exist create camera
                if (!exists){

                    String[]  cameraDirection, cameraTime;
                    StringBuffer stringBuffer = new StringBuffer();
                    ArrayList<String> cameraURL = new ArrayList<>();
                    cameraDirection = new String[cameras.get(i).presets.size()];
                    cameraTime = new String[cameras.get(i).presets.size()];

                    //go trough camera info
                    for(int k = 0; k < cameras.get(i).presets.size(); k++){
                        //get info about camera
                        if (cameras.get(i).presets.get(k) != null){
                            cameraDirection[k] = cameras.get(i).presets.get(k).presentationName ;
                            cameraURL.add(cameras.get(i).presets.get(k).imageUrl);
                            cameraTime[k] = cameras.get(i).presets.get(k).measuredTime;
                            if (cameraDirection[k] != null){
                                stringBuffer.append(cameraDirection[k]+ "dt" +cameraTime[k]+ "SPLIT");
                            }

                        }

                    }
                    String directionAndTime = stringBuffer.toString();
                    //add marker to the map and to the marker list
                    cameraMarkerList.add(addMarkerCamera(cameraGeoPoint, cameras.get(i).name, directionAndTime, cameraURL ));
                }
            }
        }
    }


    public Marker addMarkerWeather(GeoPoint p, String title, String temp) {
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);

        //add marker to map overlay
        map.getOverlays().add(marker);

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_cloud_24));
        marker.setTitle(title);
        marker.setSnippet(temp);
        marker.setInfoWindow(new CustomMarkerInfoWindow(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);


        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //show popup window
                m.showInfoWindow();
                //set marker to center of screen
                mapController.setCenter(p);
                return true;
            }
        });
        return marker;
    }

    public Marker addBusStop(GeoPoint p, String title, String snippet){
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);

        //add marker to map overlay
        map.getOverlays().add(marker);

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_directions_bus_24));
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setInfoWindow(new BusDialog(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);


        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //show popup window
                m.showInfoWindow();
                //set marker to center of screen
                mapController.setCenter(p);
                return true;
            }
        });
        return marker;
    }


    public Marker addMarkerCamera(GeoPoint p, String name, String directionTime, ArrayList photoURL) {
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);

        //add marker to map overlay
        map.getOverlays().add(marker);

        //put photo url's to single string
        StringBuffer photoBuffer = new StringBuffer();
        for (int i = 0; i < photoURL.size(); i++){
            photoBuffer.append(photoURL.get(i)+ "SPLIT");
        }
        String photoString = photoBuffer.toString();

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_camera_alt_24));
        marker.setTitle(name);
        marker.setSnippet(directionTime);
        marker.setSubDescription(photoString);
        marker.setInfoWindow(new CustomCameraMarkerInfoWindow(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);

        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //show popup window
                m.showInfoWindow();
                //set marker to center of screen
                mapController.setCenter(p);
                return true;
            }
        });

        return marker;
    }


    public Marker addMarkerWeatherCamera(GeoPoint p, String name, String directionTime, ArrayList photoURL) {
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);

        //add marker to map overlay
        map.getOverlays().add(marker);

        //put photo url's to single string
        StringBuffer photoBuffer = new StringBuffer();
        for (int i = 0; i < photoURL.size(); i++){
            photoBuffer.append(photoURL.get(i) + "SPLIT");
        }
        String photoString = photoBuffer.toString();

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_weather_camera_24));
        marker.setTitle(name);
        marker.setSnippet(directionTime);
        marker.setSubDescription(photoString);
        marker.setInfoWindow(new CustomCameraMarkerInfoWindow(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);

        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //show popup window
                m.showInfoWindow();
                //set marker to center of screen
                mapController.setCenter(p);
                return true;
            }
        });
        return marker;
    }

    //get cameras from API
    private void getAllCameras(){
        ApolloConnector.setupApollo().query(
                GetAllCamerasQuery
                .builder()
                .build())
                .enqueue(new ApolloCall.Callback<GetAllCamerasQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllCamerasQuery.Data> response) {
                        //put response data to list
                        cameras = response.getData().cameras;
                        createCameraItems();
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.d("MaingetAllCameras", "Exception " + e.getMessage(), e);
                    }
                });

    }

    //get weatherstations from api
    private void getAllWeatherStations(){
        ApolloConnector.setupApollo().query(
                GetAllWeatherStationsQuery
                .builder()
                .build())
                .enqueue(new ApolloCall.Callback<GetAllWeatherStationsQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllWeatherStationsQuery.Data> response) {
                        //put response data to list
                        weatherStations = response.getData().weatherStations;
                        createWeatherCameraItems();
                        createWeatherItems();
                    }
                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.d("MaingetAllWeather", "Exception " + e.getMessage(), e);
                    }
                });

    }


    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void connectToAPI(){
        // First, create an `ApolloClient`
        // Replace the serverUrl with your GraphQL endpoint
        ApolloClient apolloClient = ApolloClient.builder()
                .serverUrl("https://api.oulunliikenne.fi/proxy/graphql")
                .build();

        // Then enqueue your query
        apolloClient.query(new GetAllCarParksQuery())
                .enqueue(new ApolloCall.Callback<GetAllCarParksQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllCarParksQuery.Data> response) {
                        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
                        String[] seperated = response.getData().carParks().toString().split(",");
                        int i = 1;
                        while(i<seperated.length){
                            String[] name = seperated[i].split("=");
                            String[] lat = seperated[i+1].split("=");
                            String[] lon = seperated[i+2].split("=");
                            String[] spacesAvailable = seperated[i+3].split("=");
                            items.add(new OverlayItem(name[1], "Vapaana: "+spacesAvailable[1], new GeoPoint(Double.parseDouble(lat[1]),Double.parseDouble(lon[1])))); // Lat/Lon decimal degrees
                            if(i==16||i==76){
                                i=i+5;
                            }
                            i=i+5;
                        }

                        Drawable newMarker = ctx.getResources().getDrawable(R.drawable.mymarker);

                        /*ItemizedIconOverlay<OverlayItem> mOverlay = new ItemizedIconOverlay<OverlayItem>(items,newMarker,new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                            @Override
                            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                                //do something
                                return true;
                            }
                            @Override
                            public boolean onItemLongPress(final int index, final OverlayItem item) {
                                return false;
                            }
                        },ctx);*/

                        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,newMarker,newMarker, Color.WHITE,
                                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                                    @Override
                                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                                        //do something
                                        return true;
                                    }
                                    @Override
                                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                                        return false;
                                    }
                                }, ctx);


                        mOverlay.setFocusItemsOnTap(true);
                        map.getOverlays().add(mOverlay);
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("Apollo", "Error", e);
                    }
                });
    }

    private void getBusStops()
    {
        ApolloConnector.setupApollo().query(
                StopsByBboxQuery
                        .builder()
                        .build())
                .enqueue(new ApolloCall.Callback<StopsByBboxQuery.Data>(){
                    @Override
                    public void onResponse(@NotNull Response<StopsByBboxQuery.Data> response) {

                        //your items
                        //haetaan tarvittava data graphqlstä ja lisätään se arraylistiin
                        ArrayList<OverlayItem> stops = new ArrayList<OverlayItem>();

                        // KÄYTÄ NESTED SWITCH CASE IF ELSEN SIJASTA.


                        for(int i = 0; i<response.getData().stopsByBbox().size()-1; i++)
                        {
                            for(int j = 0; j < response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().size()-3;j++) {
                                int scheduledArrival = response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).scheduledArrival();
                                int hours = scheduledArrival / 60;
                                int minutes = hours % 60;
                                hours = hours / 60;
                                if (hours >= 0 ) {
                                    busMarkerList.add(addBusStop(new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()),
                                                                                response.getData().stopsByBbox().get(i).name(),
                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":" +
                                                   response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
                                                    +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +


                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
                                                   response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
                                                   response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":" +
                                                   response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60
                                                    ));
//                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
//                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
//                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +
//
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
//                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
//                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
//                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,
//
//                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees

                                } else if (minutes <= 9) {
//                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
//                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60  + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60  + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":0" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,
//                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees

                                } else if (hours <= 9) {
//                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
//                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
//                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
//                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
//                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
//                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,
//
//                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees

                                } else if (hours >= 9 && minutes >= 9) {

//                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
//                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +
//
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":" +
//                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,
//                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees
                                }
                            }
                        }

//                        Drawable busStop = ctx.getResources().getDrawable(R.drawable.busicon);
//
//                        //the overlay
//                        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(stops, busStop, busStop, Color.WHITE,
//                                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
//                                    @Override
//                                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
//                                        //do something
//
//                                        return true;
//                                    }
//
//                                    @Override
//                                    public boolean onItemLongPress(final int index, final OverlayItem item) {
//                                        return false;
//                                    }
//                                }, ctx);
//                        mOverlay.setFocusItemsOnTap(true);
//
//                        map.getOverlays().add(mOverlay);
//                        Log.d("MainActivity", "Response: " + response.getData().stopsByBbox().get(0).stoptimesWithoutPatterns.size());
                   }




                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("Apollo", "toimiiko vai ei error", e);
                    }
                });



    }
}