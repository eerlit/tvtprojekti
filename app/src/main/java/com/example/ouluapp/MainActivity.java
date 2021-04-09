package com.example.ouluapp;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.squareup.picasso.Picasso;


import org.jetbrains.annotations.NotNull;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private  MyLocationNewOverlay mLocationOverlay = null;
    Context ctx;
    IMapController mapController;
    List<GetAllCamerasQuery.Camera> cameras = new ArrayList<>();
    List<GetAllWeatherStationsQuery.WeatherStation> weatherStations = new ArrayList<>();

    ArrayList<OverlayItem> cameraItems = new ArrayList<OverlayItem>();
    ArrayList<OverlayItem> weatherItems = new ArrayList<OverlayItem>();
    ArrayList<Marker> weatherMarkerList = new ArrayList<>();
    ArrayList<Marker> cameraMarkerList = new ArrayList<>();
    ArrayList<Marker> weatherCamMarkerList = new ArrayList<>();
    int tet = 0;
    Dialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectToAPI();
        getAllWeatherStations();
        getAllCameras();

        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);



        mapController = map.getController();
        mapController.setZoom(14);
        GeoPoint startPoint = new GeoPoint(65.012615, 25.471453);
        mapController.setCenter(startPoint);

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx),map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        requestPermissionsIfNecessary(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });


    }


    private  void updateMap(String itemToUpdate){
        Marker marker;



        if(itemToUpdate == "weather") {
            for (int i = 0; i < cameras.size(); i++) {


                double cLat = cameras.get(i).lat.doubleValue();
                double cLon = cameras.get(i).lon.doubleValue();


                for (int j = 0; j < weatherStations.size(); j++) {
                    double wLat = weatherStations.get(j).lat.doubleValue();
                    double wLon = weatherStations.get(j).lon.doubleValue();
                    int tempIndex = 0;
                    GeoPoint itemGeopoint = new GeoPoint(cLat, cLon);
                    GeoPoint weatherItemPoint = new GeoPoint(wLat, wLon);
                    double dist = itemGeopoint.distanceToAsDouble(weatherItemPoint);
                    if (/*wLat == cLat && cLon == wLon*//*1.99995 < weatherDc && weatherDc < 2.00001*/dist < 50) {

                        String[] cameraURL, cameraDirection, cameraTime;
                        double temp;


                        cameraURL = new String[cameras.get(i).presets.size()];
                        cameraDirection = new String[cameras.get(i).presets.size()];
                        cameraTime = new String[cameras.get(i).presets.size()];
                        for (int w = 0; w < weatherStations.get(j).sensorValues.size(); w++){
                            if (weatherStations.get(j).sensorValues.get(w).name.contains("ILMA") && !weatherStations.get(j).sensorValues.get(w).name.contains("_")){
                                tempIndex = w;
                            }
                        }
                        temp = weatherStations.get(j).sensorValues.get(tempIndex).sensorValue;


                        for (int k = 0; k < cameras.get(i).presets.size(); k++) {
                            cameraDirection[k] = cameras.get(i).presets.get(k).presentationName;
                            cameraURL[k] = cameras.get(i).presets.get(k).imageUrl;
                            cameraTime[k] = cameras.get(i).presets.get(k).measuredTime;
                        }
                        StringBuffer stringBuffer = new StringBuffer();
                        for (int b = 0; b < cameraTime.length; b++){
                            stringBuffer.append(cameraDirection[b]+ "dt" +cameraTime[b]+ "SPLIT");
                        }
                        stringBuffer.append(temp);
                        String directionAndTime = stringBuffer.toString();
                        weatherCamMarkerList.add(addMarkerCamera(itemGeopoint, cameras.get(i).name, directionAndTime, cameraURL, "WEATHERCAMERA"));
                    }

                }
            }
                for (int i = 0; i < cameras.size(); i++) {


                    double cLat = cameras.get(i).lat.doubleValue();
                    double cLon = cameras.get(i).lon.doubleValue();


                    for (int j = 0; j < weatherStations.size(); j++) {
                        boolean exists = false;
                        double wLat = weatherStations.get(j).lat.doubleValue();
                        double wLon = weatherStations.get(j).lon.doubleValue();
                        GeoPoint itemGeopoint = new GeoPoint(cLat, cLon);
                        GeoPoint weatherItemPoint = new GeoPoint(wLat, wLon);
                        double dist = itemGeopoint.distanceToAsDouble(weatherItemPoint);
                        String time = weatherStations.get(j).measuredTime.toString();

                        for (int k = 0; k < weatherCamMarkerList.size(); k++) {
                            if (weatherCamMarkerList.get(k).getPosition().distanceToAsDouble(weatherItemPoint) < 50) {
                                exists = true;
                                break;
                            }
                        }


                        if (dist >= 50 && exists == false) {
                            int tempIndex = 0;
                            String temp;
                            for (int w = 0; w < weatherStations.get(j).sensorValues.size(); w++){
                                if (weatherStations.get(j).sensorValues.get(w).name.contains("ILMA") && !weatherStations.get(j).sensorValues.get(w).name.contains("_")){
                                    tempIndex = w;
                                }
                            }
                            temp = weatherStations.get(j).sensorValues.get(tempIndex).sensorValue.toString()+ "SPLIT" +time;
                            weatherMarkerList.add(addMarkerWeather(weatherItemPoint, weatherStations.get(j).name, temp, "WEATHER"));

                        }


                    }

                }


            }

        else if(itemToUpdate == "cameras") {
            for (int i = 0; i < cameras.size(); i++) {
                boolean exists = false;
                GeoPoint cameraGeoPoint = new GeoPoint(cameras.get(i).lat, cameras.get(i).lon);
                for (int k = 0; k < weatherCamMarkerList.size(); k++){
                    if (weatherCamMarkerList.get(k).getPosition().distanceToAsDouble(cameraGeoPoint) == 0){
                        exists = true;
                        break;
                    }
                }

                if (exists == false){
                    String[] cameraURL, cameraDirection, cameraTime, cameraName;

                    cameraURL = new String[cameras.get(i).presets.size()];
                    cameraDirection = new String[cameras.get(i).presets.size()];
                    cameraTime = new String[cameras.get(i).presets.size()];


                    for(int k = 0; k < cameras.get(i).presets.size(); k++){
                        cameraDirection[k] = cameras.get(i).presets.get(k).presentationName;
                        cameraURL[k] = cameras.get(i).presets.get(k).imageUrl;
                        cameraTime[k] = cameras.get(i).presets.get(k).measuredTime;
                    }
                    StringBuffer stringBuffer = new StringBuffer();
                    for (int b = 0; b < cameraTime.length; b++){
                        stringBuffer.append(cameraDirection[b]+ "dt" +cameraTime[b]+ "SPLIT");
                    }
                    String directionAndTime = stringBuffer.toString();
                    cameraMarkerList.add(addMarkerCamera(cameraGeoPoint, cameras.get(i).name, directionAndTime, cameraURL,"CAMERA" ));

                }
            }
        }


    }
    public Marker addMarkerWeather(GeoPoint p, String title, String temp, String id) {
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);
        map.getOverlays().add(marker);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_cloud_24));
        marker.setTitle(title);

        String[] tempDate = temp.split("SPLIT");

        marker.setSnippet(temp);
        marker.setInfoWindow(new CustomMarkerInfoWindow(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {


            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                Log.i("Script", "onMarkerClick()");
                m.showInfoWindow();
                mapController.setCenter(p);
                return true;
            }


        });


        return marker;


    }
    public Marker addMarkerCamera(GeoPoint p, String name, String directionTime, String[] photoURL, String id) {
        Marker marker = new Marker(map);
        marker = new Marker(map);
        StringBuffer photoBuffer = new StringBuffer();

        marker.setPosition(p);
        map.getOverlays().add(marker);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if (id == "WEATHERCAMERA"){
            marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_weather_camera_24));
        }
        if (id == "CAMERA"){
            marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_camera_alt_24));
        }
        for (int i = 0; i < photoURL.length; i++){
            photoBuffer.append(photoURL[i] + "SPLIT");
        }

        String photoString = photoBuffer.toString();


        marker.setTitle(name);
        marker.setSnippet(directionTime);
        marker.setSubDescription(photoString);

        marker.setInfoWindow(new CustomCameraMarkerInfoWindow(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {


            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                Log.i("Script", "onMarkerClick()");
                m.showInfoWindow();
                mapController.setCenter(p);
                return true;
            }


        });



        return marker;


    }
    private void getAllCameras(){
        Log.d("MainAcitvity", "GetAllCameras");
        ApolloConnector.setupApollo().query(
                GetAllCamerasQuery
                .builder()
                .build())
                .enqueue(new ApolloCall.Callback<GetAllCamerasQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllCamerasQuery.Data> response) {
                        cameras = response.data().cameras;
                        updateMap("cameras");
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.d("MainActigetAllCameras", "Exception " + e.getMessage(), e);
                    }
                });


    }

    private void getAllWeatherStations(){
        ApolloConnector.setupApollo().query(
                GetAllWeatherStationsQuery
                .builder()
                .build())
                .enqueue(new ApolloCall.Callback<GetAllWeatherStationsQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllWeatherStationsQuery.Data> response) {
                        weatherStations = response.data().weatherStations;
                        updateMap("weather");
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {

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
                        Log.e("Apollo","Testing: "+response.getData().carParks());
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("Apollo", "Error", e);
                    }
                });
    }
}