package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

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
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.GetAllCarParksQuery;

import org.jetbrains.annotations.NotNull;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private  MyLocationNewOverlay mLocationOverlay = null;
    int inteksi = 0;
    Context ctx;

    List<GetAllCamerasQuery.Camera> cameras = new ArrayList<>();
    List<GetAllWeatherStationsQuery.WeatherStation> weatherStations = new ArrayList<>();

    ArrayList<OverlayItem> cameraItems = new ArrayList<OverlayItem>();
    ArrayList<OverlayItem> weatherItems = new ArrayList<OverlayItem>();
    ArrayList<OverlayItem> carParksItems = new ArrayList<OverlayItem>();
    int tet = 0;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAllWeatherStations();
        getAllCameras();
        getAllCarParks();

        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);


        IMapController mapController = map.getController();
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

        if(itemToUpdate == "weather") {
            for (int i = 0; i < weatherStations.size(); i++) {
                System.out.println("WeatherNAME else : " + weatherStations.get(i).name + " LAT : " + weatherStations.get(i).lat + " LON : " + weatherStations.get(i).lon);
                OverlayItem weatherItem = new OverlayItem(weatherStations.get(i).name, "WeatherStation", new GeoPoint(weatherStations.get(i).lat.doubleValue(), weatherStations.get(i).lon.doubleValue()));

                Drawable newMarker = ContextCompat.getDrawable(ctx, R.drawable.ic_baseline_cloud_24);
                weatherItem.setMarker(newMarker);
                weatherItems.add(weatherItem);
            }
            for (int i = 0; i < weatherStations.size(); i++) {


                double wLat = weatherStations.get(i).lat.doubleValue();
                double wLon = weatherStations.get(i).lon.doubleValue();


                for (int j = 0; j <cameras.size(); j++){

                    double cLat = cameras.get(j).lat.doubleValue();
                    double cLon = cameras.get(j).lon.doubleValue();
                    double weatherDc = wLat/cLat + wLon/cLon;
                    if(/*wLat == cLat && cLon == wLon*/1.99995 < weatherDc && weatherDc < 2.00001) {
                        System.out.println("WEATHERDC" + weatherDc);


                        OverlayItem weatherCameraItem = new OverlayItem(cameras.get(j).name, "WeatherCamera", new GeoPoint(cLat,cLon));

                        Drawable weatherCamIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_baseline_weather_camera_24);
                        weatherCameraItem.setMarker(weatherCamIcon);
                        //cameraItems.set(j, weatherCameraItem);
                        weatherItems.set(i, weatherCameraItem);

                        System.out.println("CameraNAME : " +cameras.get(j).name + " LAT : " + cameras.get(j).lat + " LON : " + cameras.get(j).lon + " INDEX J : "+ j);
                        System.out.println("WeatherNAME : " + weatherStations.get(i).name + " LAT : " + weatherStations.get(i).lat + " LON : " + weatherStations.get(i).lon + " INDEX I : "+ i);
                        System.out.println(" ");
                        break;


                    }


                }

            }


        }
        else if(itemToUpdate == "cameras") {
            for (int i = 0; i < cameras.size(); i++) {
                OverlayItem cameraItem = new OverlayItem(cameras.get(i).name, "Camera", new GeoPoint(cameras.get(i).lat.doubleValue(), cameras.get(i).lon.doubleValue()));

                Drawable newMarker = ContextCompat.getDrawable(ctx, R.drawable.ic_baseline_camera_alt_24);
                cameraItem.setMarker(newMarker);
                cameraItems.add(cameraItem);
            }
        }

        ItemizedOverlayWithFocus<OverlayItem> cameraOverlay = new ItemizedOverlayWithFocus<OverlayItem>(cameraItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {


                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        System.out.println("Camera");
                        System.out.println("CameraNAME : " + cameras.get(index).name + " LAT : " + cameras.get(index).lat + " LON : " + cameras.get(index).lon + " INDEX  : "+ index);
                        String[] cameraURL, cameraDirection, cameraTime, cameraName;
                        System.out.println(index);
                        System.out.println(item);
                        System.out.println(cameras.get(index).name);
                        //System.out.println(weatherStations.get(index).name);
                        Intent intent = new Intent(ctx, CameraPhoto.class);

                        cameraURL = new String[cameras.get(index).presets.size()];
                        cameraDirection = new String[cameras.get(index).presets.size()];
                        cameraTime = new String[cameras.get(index).presets.size()];
                        cameraName = new String[cameras.get(index).presets.size()];

                        for(int i = 0; i < cameras.get(index).presets.size(); i++){
                            cameraDirection[i] = cameras.get(index).presets.get(i).presentationName;
                            cameraURL[i] = cameras.get(index).presets.get(i).imageUrl;
                            cameraTime[i] = cameras.get(index).presets.get(i).measuredTime;
                            cameraName[i] = cameras.get(index).name;


                        }
                        intent.putExtra("CAMERA_DIRECTION", cameraDirection);
                        intent.putExtra("CAMERA_NAME", cameraName);
                        intent.putExtra("CAMERA", cameraURL);
                        intent.putExtra("TIME",cameraTime);
                        startActivity(intent);
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, this);

        ItemizedOverlayWithFocus<OverlayItem> weatherOverlay = new ItemizedOverlayWithFocus<OverlayItem>(weatherItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {


                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        int tempIndex = 0;
                        int cameraIndex = -1;
                        System.out.println("WeatherStation");
                        System.out.println("WeatherNAME : " + weatherStations.get(index).name + " LAT : " + weatherStations.get(index).lat + " LON : " + weatherStations.get(index).lon + " INDEX I : "+ index);
                        for (int i = 0; i < cameras.size(); i++){
                            double weatherDc = weatherStations.get(index).lat.doubleValue()/cameras.get(i).lat.doubleValue() + weatherStations.get(index).lon.doubleValue()/cameras.get(i).lon.doubleValue();
                            if (1.99995 < weatherDc && weatherDc < 2.00001 ){
                                cameraIndex = i;
                                System.out.println("CAMERAINDEX: "+ cameraIndex);
                            }
                        }
                        if (cameraIndex != -1){
                            Double temp;
                            String[] cameraURL, cameraDirection, cameraTime, cameraName;
                            for (int i = 0; i < weatherStations.get(index).sensorValues.size(); i++){
                                if (weatherStations.get(index).sensorValues.get(i).name.contains("ILMA") && !weatherStations.get(index).sensorValues.get(i).name.contains("_")){
                                    tempIndex = i;
                                    System.out.println("TEMPINDEX: "+ tempIndex);
                                }
                            }


                            Intent intent = new Intent(ctx, CameraPhoto.class);

                            cameraURL = new String[cameras.get(cameraIndex).presets.size()];
                            cameraDirection = new String[cameras.get(cameraIndex).presets.size()];
                            cameraTime = new String[cameras.get(cameraIndex).presets.size()];
                            cameraName = new String[cameras.get(cameraIndex).presets.size()];
                            temp = weatherStations.get(index).sensorValues.get(tempIndex).sensorValue;

                            for(int i = 0; i < cameras.get(cameraIndex).presets.size(); i++){
                                cameraDirection[i] = cameras.get(cameraIndex).presets.get(i).presentationName;
                                cameraURL[i] = cameras.get(cameraIndex).presets.get(i).imageUrl;
                                cameraTime[i] = cameras.get(cameraIndex).presets.get(i).measuredTime;
                                cameraName[i] = cameras.get(cameraIndex).name;


                            }
                            intent.putExtra("TEMP", temp);
                            intent.putExtra("CAMERA_DIRECTION", cameraDirection);
                            intent.putExtra("CAMERA_NAME", cameraName);
                            intent.putExtra("CAMERA", cameraURL);
                            intent.putExtra("TIME",cameraTime);
                            startActivity(intent);
                        }
                        if (cameraIndex == -1){

                            System.out.println(weatherStations.get(index).name);
                        }


                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, this);

        Drawable pMarker = ctx.getResources().getDrawable(R.drawable.mymarker);
        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(carParksItems,pMarker,pMarker, Color.WHITE,
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

        weatherOverlay.setFocusItemsOnTap(true);
        mOverlay.setFocusItemsOnTap(true);

        map.getOverlays().add(mOverlay);
        map.getOverlays().add(cameraOverlay);
        map.getOverlays().add(weatherOverlay);


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

    private void getAllCarParks(){
        ApolloConnector.setupApollo().query(new GetAllCarParksQuery())
                .enqueue(new ApolloCall.Callback<GetAllCarParksQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllCarParksQuery.Data> response) {
                        String[] seperated = response.getData().carParks().toString().split(",");
                        int i = 1;
                        while(i<seperated.length){
                            String[] name = seperated[i].split("=");
                            String[] lat = seperated[i+1].split("=");
                            String[] lon = seperated[i+2].split("=");
                            String[] spacesAvailable = seperated[i+3].split("=");
                            carParksItems.add(new OverlayItem(name[1], "Vapaana: "+spacesAvailable[1], new GeoPoint(Double.parseDouble(lat[1]),Double.parseDouble(lon[1])))); // Lat/Lon decimal degrees
                            if(i==16||i==76){
                                i=i+5;
                            }
                            i=i+5;
                        }
                        //Log.e("Apollo","Testing: "+response.getData().carParks());
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("Apollo", "Error", e);
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
}