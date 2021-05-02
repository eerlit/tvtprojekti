package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.apollographql.apollo.ApolloCall;
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
    Boolean[] menuItem = {true,true,true,true, true, true};
    ArrayList<Marker> weatherMarkerList = new ArrayList<>();
    ArrayList<Marker> cameraMarkerList = new ArrayList<>();
    ArrayList<Marker> weatherCamMarkerList = new ArrayList<>();
    ArrayList<Marker> announcementMarkerList = new ArrayList<>();
    private ArrayList<Polyline> roadList = new ArrayList<>();
    private ArrayList<Double> latListForRoads = new ArrayList<Double>();
    private ArrayList<Double> longListForRoads = new ArrayList<Double>();
    private ArrayList<Double> longListForAnnouncements = new ArrayList<Double>();
    private ArrayList<Double> latListForAnnouncements = new ArrayList<Double>();
    ArrayList<Marker> busMarkerList = new ArrayList<>();
    ArrayList<Marker> parkHouseMarkerList = new ArrayList<>();

    Polyline uusiTie;
    String[] arrayForRoads;
    String[] arrayForAnnouncements;
    boolean roadsVisible = true;

    private RotationGestureOverlay mRotationGestureOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getAllWeatherStations();
        getAllCameras();
        getAllCarParks();
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

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

        getAllTrafficAnnouncements();
        getRoadCongestion();
        updateRoadMap();

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
                   //kameroiden näyttäminen menusta valistemalla
                   for (int i=0; i<cameraMarkerList.size(); i++){
                       cameraMarkerList.get(i).setVisible(true);
                   }
                   //kameroiden piilottaminen menusta valistemalla
                   for (int i=0; i<weatherCamMarkerList.size(); i++){
                       weatherCamMarkerList.get(i).setVisible(true);
                   }

               }
               if (!item.isChecked()){
                    menuItem[0] = false;
                   for (int i=0; i<cameraMarkerList.size(); i++){
                       cameraMarkerList.get(i).setVisible(false);
                   }
               }
               break;

           case R.id.sääasemat:
               item.setChecked(!item.isChecked());
               if (item.isChecked()){
                   menuItem[1] = true;
                   //sääasemien näyttäminen menusta valitsemalla
                   for (int i=0; i<weatherMarkerList.size(); i++){
                       weatherMarkerList.get(i).setVisible(true);
                   }
                   //sääkameroiden näyttäminen menusta valitsemalla
                   for (int i=0; i<weatherCamMarkerList.size(); i++){
                       weatherCamMarkerList.get(i).setVisible(true);
                   }
               }
               if (!item.isChecked()){
                   menuItem[1] = false;
                   //sääasemien piilottaminen menusta valitsemalla
                   for (int i=0; i<weatherMarkerList.size(); i++){
                       weatherMarkerList.get(i).setVisible(false);
                   }
               }

               break;

           case R.id.parkkihallit:
               item.setChecked(!item.isChecked());
               if(item.isChecked()){
                   menuItem[2] = true;
                   getAllCarParks();
               }
               if(!item.isChecked()){
                   menuItem[2] = false;
                   for (int i=0; i<parkHouseMarkerList.size();i++){
                       parkHouseMarkerList.get(i).remove(map);
                   }
               }
               break;

           case R.id.autotiet:
               item.setChecked(!item.isChecked());
               if(item.isChecked()){
                   menuItem[3] = true;
                   for( int k =0; k < roadList.size(); k++) {
                       roadList.get(k).setVisible(true);
                       roadsVisible = true;
                   }
               }
               if(!item.isChecked()){
                   menuItem[3] = false;
                    for( int k =0; k < roadList.size(); k++){
                        roadList.get(k).setVisible(false);
                        roadsVisible = false;
                    }
               }
               break;

           case R.id.hairioilmoitukset:
               item.setChecked(!item.isChecked());
               if(item.isChecked()){
                   menuItem[4] = true;
                   for(int k = 0; k < announcementMarkerList.size(); k++){
                       announcementMarkerList.get(k).setVisible(true);
                   }
               }
               if(!item.isChecked()){
                   menuItem[4] = false;
                   for(int k = 0; k < announcementMarkerList.size(); k++){
                       announcementMarkerList.get(k).setVisible(false);
                   }

               }
               break;

           case R.id.bussit:
               item.setChecked(!item.isChecked());
               if(item.isChecked()){
                   menuItem[5] = true;
                   getBusStops();

               }
               if(!item.isChecked()){
                   menuItem[5] = false;
                   for (int i=0; i<busMarkerList.size();i++){
                       busMarkerList.get(i).remove(map);
                   }
               }
               break;

       }

       //sääkameroiden piilotus jos kamerat ja sääasemat on unchecked menussa
       if (!menuItem[0] && !menuItem[1]){
           for (int i=0; i<weatherCamMarkerList.size(); i++){
               weatherCamMarkerList.get(i).setVisible(false);
           }
       }

        return super.onOptionsItemSelected(item);
    }

    private  void createWeatherCameraItems(){
        //sääkameroiden teko
        if(menuItem[1] || menuItem[0]) {
            for (int i = 0; i < cameras.size(); i++) {

                //kameran latitude ja longitude
                double cLat = cameras.get(i).lat;
                double cLon = cameras.get(i).lon;

                //käy kaikki sääasemat läpi
                for (int j = 0; j < weatherStations.size(); j++) {

                    int tempIndex = -1;
                    int moistureIndex = -1;
                    int windIndex = -1;

                    //kameran geopisteet GeoPointiksi
                    GeoPoint cameraItemPoint = new GeoPoint(cLat, cLon);

                    //jos kamera ja sääasema on 50m säteellä ne yhdistetään sääkameraksi
                    if (cameraItemPoint.distanceToAsDouble(new GeoPoint(weatherStations.get(j).lat, weatherStations.get(j).lon)) < 50 ) {

                        String[]  cameraDirection, cameraTime;
                        ArrayList<String> cameraURL = new ArrayList<>();
                        double temp, wind, moisture;
                        StringBuffer stringBuffer = new StringBuffer();
                        cameraDirection = new String[cameras.get(i).presets.size()];
                        cameraTime = new String[cameras.get(i).presets.size()];




                        //käy kameran datan läpi
                        for (int k = 0; k < cameras.get(i).presets.size(); k++) {
                            //hae kameran data
                            if (cameras.get(i).presets.get(k) != null){
                                cameraDirection[k] = cameras.get(i).presets.get(k).presentationName;
                                cameraURL.add(cameras.get(i).presets.get(k).imageUrl);
                                cameraTime[k] = cameras.get(i).presets.get(k).measuredTime;
                                //laita data stringbufferiin
                                if (cameraDirection[k] != null){
                                    stringBuffer.append(cameraDirection[k] + "dt" + cameraTime[k] + "SPLIT");
                                }
                            }

                        }

                        //käy sääasemat läpi ja hae halutun datan indeksi
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

                        //hae kosteus aikaisemmin haetulla indeksillä
                        if (moistureIndex != -1){
                            moisture = weatherStations.get(j).sensorValues.get(moistureIndex).sensorValue;
                            stringBuffer.append(moisture+"%" + "WSPL");
                        }else {
                            stringBuffer.append("NODATA" + "WSPL");
                        }
                        //hae tuulennopeus aikaisemmin haetulla indeksillä
                        if (windIndex != -1){
                            wind = weatherStations.get(j).sensorValues.get(windIndex).sensorValue;
                            stringBuffer.append(wind+ "m/s" + "WSPL");
                        }else {
                            stringBuffer.append("NODATA" + "WSPL");
                        }
                        //hae lämpötila aikaisemmin haetulla indeksillä
                        if (tempIndex != -1){
                            temp = weatherStations.get(j).sensorValues.get(tempIndex).sensorValue;
                            stringBuffer.append(temp + "°C"+"WSPL");
                        }else {
                            stringBuffer.append("NODATA" + "WSPL");
                        }

                        //laita kaikki data stringbufferista yhteen stringiin
                        String directionTimeWeather = stringBuffer.toString();

                        //lisää marker karttaan sekä listaan
                        weatherCamMarkerList.add(addMarkerWeatherCamera(cameraItemPoint, cameras.get(i).name, directionTimeWeather, cameraURL));
                        break;
                    }

                }
            }
        }




    }
    public void createWeatherItems(){
        if (menuItem[1]){
            //käy kaikki sääasemat läpi
            for (int j = 0; j < weatherStations.size(); j++) {
                boolean exists = false;
                //hae sääaseman geopisteet ja tee uusi GeoPoint
                GeoPoint weatherItemPoint = new GeoPoint(weatherStations.get(j).lat, weatherStations.get(j).lon);

                //tarkista jos sääkamera on olemassa
                for (int k = 0; k < weatherCamMarkerList.size(); k++) {
                    if (weatherCamMarkerList.get(k).getPosition().distanceToAsDouble(weatherItemPoint) < 50) {
                        exists = true;
                        break;
                    }
                }
                //jos sääkameraa ei ole olemassa tee sääasema
                if (!exists) {
                    StringBuffer stringBuffer = new StringBuffer();
                    double temp, wind, moisture;
                    int tempIndex = -1;
                    int moistureIndex = -1;
                    int windIndex = -1;

                    //käy sääasemat läpi ja hae halutun datan indeksi
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

                    //hae kosteus aikaisemmin haetulla indeksillä
                    if (moistureIndex != -1){
                        moisture = weatherStations.get(j).sensorValues.get(moistureIndex).sensorValue;
                        stringBuffer.append(moisture+"%" + "SPLIT");
                    }else {
                        stringBuffer.append("NODATA" + "SPLIT");
                    }
                    //hae tuulennopeus aikaisemmin haetulla indeksillä
                    if (windIndex != -1){
                        wind = weatherStations.get(j).sensorValues.get(windIndex).sensorValue;
                        stringBuffer.append(wind+ "m/s" + "SPLIT");
                    }else {
                        stringBuffer.append("NODATA" + "SPLIT");
                    }
                    //hae lämpötila aikaisemmin haetulla indeksillä
                    if (tempIndex != -1){
                        temp = weatherStations.get(j).sensorValues.get(tempIndex).sensorValue;
                        stringBuffer.append(temp + "°C"+"SPLIT");
                    }else {
                        stringBuffer.append("NODATA" + "SPLIT");
                    }

                    //laita kaikki data stringbufferista yhteen stringiin
                    String weatherTime = stringBuffer.toString() +weatherStations.get(j).measuredTime.toString();

                    //lisää marker karttaan sekä listaan
                    weatherMarkerList.add(addMarkerWeather(weatherItemPoint, weatherStations.get(j).name, weatherTime));
                }
            }
        }
    }


    public void createCameraItems(){
        if( menuItem[0]) {
            //käy kaikki kamerat läpi
            for (int i = 0; i < cameras.size(); i++) {
                boolean exists = false;
                //hae kameran geopisteet ja luo uusi GeoPoint
                GeoPoint cameraGeoPoint = new GeoPoint(cameras.get(i).lat, cameras.get(i).lon);

                //tarkista onko sääkamera olemassa
                for (int k = 0; k < weatherCamMarkerList.size(); k++){
                    if (weatherCamMarkerList.get(k).getPosition().distanceToAsDouble(cameraGeoPoint) == 0){
                        exists = true;
                        break;
                    }
                }

                //jos sääkameraa ei ole olemassa luo uusi kamera
                if (!exists){

                    String[]  cameraDirection, cameraTime;
                    StringBuffer stringBuffer = new StringBuffer();
                    ArrayList<String> cameraURL = new ArrayList<>();
                    cameraDirection = new String[cameras.get(i).presets.size()];
                    cameraTime = new String[cameras.get(i).presets.size()];

                    //käy kameran data läpi
                    for(int k = 0; k < cameras.get(i).presets.size(); k++){
                        //hae kameran data
                        if (cameras.get(i).presets.get(k) != null){
                            cameraDirection[k] = cameras.get(i).presets.get(k).presentationName ;
                            cameraURL.add(cameras.get(i).presets.get(k).imageUrl);
                            cameraTime[k] = cameras.get(i).presets.get(k).measuredTime;
                            //laita data stringbufferiin
                            if (cameraDirection[k] != null){
                                stringBuffer.append(cameraDirection[k]+ "dt" +cameraTime[k]+ "SPLIT");
                            }

                        }

                    }
                    //laita kaikki data stringbufferista yhteen stringiin
                    String directionAndTime = stringBuffer.toString();
                    //lisää marker karttaan ja listaan
                    cameraMarkerList.add(addMarkerCamera(cameraGeoPoint, cameras.get(i).name, directionAndTime, cameraURL ));
                }
            }
        }
    }


    public Marker addMarkerWeather(GeoPoint p, String title, String temp) {
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);

        //lisää markeer kartta overlayhyn
        map.getOverlays().add(marker);

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_cloud_24));
        marker.setTitle(title);
        marker.setSnippet(temp);
        marker.setInfoWindow(new CustomMarkerInfoWindow(map));
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);

        //sääasema markerin klikin kuuntelija
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //näytä popup window
                m.showInfoWindow();
                //siirrä näkymä keskelle markeria
                mapController.setCenter(p);
                return true;
            }
        });
        return marker;
    }

    public Marker addMarkerCarParks(GeoPoint p, String title, String temp) {
        StringBuffer sb = new StringBuffer(temp);
        sb.deleteCharAt(sb.length()-1);
        Marker marker = new Marker(map);
        marker.setPosition(p);

        //add marker to map overlay
        map.getOverlays().add(marker);

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(ctx,R.drawable.ic_baseline_local_parking_24));
        marker.setTitle(title);
        marker.setSnippet(sb.toString());
        //marker.setInfoWindow(new CustomMarkerInfoWindow(map));
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

        //lisää marker overlayhyn
        map.getOverlays().add(marker);

        //laita photoURL lista yhteen stringiin
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

        //kamera markerin klikin kuuntelija
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //näytä popup window
                m.showInfoWindow();
                //siirrä näkymä keskelle markeria
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

        //lisää marker overlayhyn
        map.getOverlays().add(marker);

        //laita photoURL lista yhteen stringiin
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

        //sääkamera markerin klikin kuuntelija
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView arg1) {
                //näytä popup window
                m.showInfoWindow();
                ////siirrä näkymä keskelle markeria
                mapController.setCenter(p);
                return true;
            }
        });
        return marker;
    }

    public Marker addMarkerBusStops(GeoPoint p, String title, String snippet){
        Marker marker = new Marker(map);
        marker = new Marker(map);
        marker.setPosition(p);


            //add marker to map overlay
            map.getOverlays().add(marker);

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.ic_baseline_directions_bus_24));
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

    //hae kaikki kamerat api:sta ja laita ne arraylistiin, sekä kutsu itemien luontifunktioita
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

    //hae kaikki sääasemat api:sta ja laita ne arraylistiin, sekä kutsu itemien luontifunktioita
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

    private void getAllCarParks(){
        ApolloConnector.setupApollo().query(new GetAllCarParksQuery())
                .enqueue(new ApolloCall.Callback<GetAllCarParksQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetAllCarParksQuery.Data> response) {
                        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
                        String[] separated = response.getData().carParks().toString().split(",");
                        int i = 1;
                        //StringBuffer sb = new StringBuffer();
                        while(i<separated.length){
                            String[] name = separated[i].split("=");
                            String[] lat = separated[i+1].split("=");
                            String[] lon = separated[i+2].split("=");
                            String[] spacesAvailable = separated[i+3].split("=");
                            //sb.deleteCharAt(spacesAvailable[1].length()-1);
                            parkHouseMarkerList.add(addMarkerCarParks(new GeoPoint(Double.parseDouble(lat[1]),Double.parseDouble(lon[1])),name[1],"Vapaana: "+spacesAvailable[1]));
                            //items.add(new OverlayItem(name[1], "Vapaana: "+spacesAvailable[1], new GeoPoint(Double.parseDouble(lat[1]),Double.parseDouble(lon[1])))); // Lat/Lon decimal degrees
                            if(i==16||i==76){
                                i=i+5;
                            }
                            i=i+5;
                        }
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

    private void updateRoadMap(){
        TimerTask updateMapTimer;
        final Handler handler = new Handler();
        Timer myTimer = new Timer();

        updateMapTimer = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(roadsVisible) {
                            getRoadCongestion();
                        }
                        System.out.println("taalla");
                    }
                });
            }
        };
        myTimer.schedule(updateMapTimer, 60000, 60000);
    }
    private void getAllTrafficAnnouncements(){

        ApolloConnector.setupApollo().query(GetAllTrafficAnnouncementsQuery.builder().build()).enqueue(new ApolloCall.Callback<GetAllTrafficAnnouncementsQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<GetAllTrafficAnnouncementsQuery.Data> response) {

                ArrayList<GeoPoint> trafficProblemCoords = new ArrayList<>();

                String announcementCoords;
                String announcementDescription;
                String announcementTitle;
                boolean outDatedAnnouncement = false;
                SimpleDateFormat presentDate = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat sdf;
                String announcementEndDate;
                String announcementSeverity;


                //System.out.println(response.getData().trafficAnnouncements.get(0).severity);

                List<GetAllTrafficAnnouncementsQuery.TrafficAnnouncement> lista = response.getData().trafficAnnouncements;

                for(int i = 0; i <= lista.size()-1; i++)
                {
                    announcementCoords = lista.get(i).geojson.toString();

                    announcementCoords = announcementCoords.replaceAll(".*coordinates=", "");
                    announcementCoords = announcementCoords.replaceAll(".*properties=", "");
                    announcementCoords = announcementCoords.replaceAll("[{}]", "");
                    announcementCoords = announcementCoords.replaceAll("\\[", "");
                    announcementCoords = announcementCoords.replaceAll("\\]", "");

                    if(announcementCoords.isEmpty()){
                        continue;
                    }
                    arrayForAnnouncements = announcementCoords.split(",");

                    for(int j = 0; j < arrayForAnnouncements.length; j++){

                        if(j %2 == 0){

                            longListForAnnouncements.add(Double.parseDouble(arrayForAnnouncements[j]));

                        }else{

                            latListForAnnouncements.add(Double.parseDouble(arrayForAnnouncements[j]));
                        }

                    }
                    for(int h = 0; h < longListForAnnouncements.size()-1; h++) {

                        announcementDescription = response.getData().trafficAnnouncements.get(i).description.fi;
                        announcementTitle = response.getData().trafficAnnouncements.get(i).title.fi;
                        announcementSeverity = response.getData().trafficAnnouncements.get(i).severity.rawValue();

                        try {
                            announcementEndDate = response.getData().trafficAnnouncements.get(i).endTime.toString();
                            if(announcementEndDate != ""){
                                announcementEndDate = announcementEndDate.replaceAll("[^\\d.]", "");
                                String year;
                                String day;
                                String month;
                                String result = "";
                                year = announcementEndDate.substring(0, 4);
                                month = announcementEndDate.substring(4,6);
                                day = announcementEndDate.substring(6,8);
                                result = result.concat(day + "/");
                                result = result.concat(month + "/");
                                result = result.concat(year);


                                Calendar cal = Calendar.getInstance();
                                String getCurrentDateTime = presentDate.format(cal.getTime());
                                sdf = new SimpleDateFormat("dd/MM/yyyy");
                                Date validUntilDate = null;

                                try {
                                    validUntilDate = sdf.parse(result);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }


                                if(new Date().after(validUntilDate)) {

                                    outDatedAnnouncement = true;
                                }

                                if(outDatedAnnouncement){
                                    continue;
                                }
                            }
                        }catch(NullPointerException e){

                        }


                        trafficProblemCoords.add(new GeoPoint(latListForAnnouncements.get(h), longListForAnnouncements.get(h)));

                        Marker roadWorkMarker = new Marker(map);

                        roadWorkMarker.setPosition(trafficProblemCoords.get(0));
                        switch(announcementSeverity){
                            case "HIGH":{
                                roadWorkMarker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.alarm));
                                break;
                            }
                            case "MEDIUM":{
                                roadWorkMarker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.caution));
                                break;
                            }

                        }
                        map.getOverlays().add(roadWorkMarker);
                        announcementMarkerList.add(roadWorkMarker);
                        roadWorkMarker.setTitle(announcementTitle);
                        roadWorkMarker.setSnippet(announcementDescription);

                    }

                    trafficProblemCoords.clear();
                    longListForAnnouncements.clear();
                    latListForAnnouncements.clear();

                }

            }

            @Override
            public void onFailure(@NotNull ApolloException e) {

            }
        });

    }
    private void getRoadCongestion() {


        ApolloConnector.setupApollo().query(
                GetTrafficFluencyFeatureCollectionQuery.builder()
                        .build()).enqueue(new ApolloCall.Callback<GetTrafficFluencyFeatureCollectionQuery.Data>() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override

            public void onResponse(@NotNull Response<GetTrafficFluencyFeatureCollectionQuery.Data> response) {


                String coordinates;
                ArrayList<GeoPoint> road = new ArrayList<>();
                int roadNumber = 0;
                int i = 0;
                //haetaan liikennedata Oulun API:n kautta lista-muuttujaan
                List<GetTrafficFluencyFeatureCollectionQuery.Feature> lista = Objects.requireNonNull(Objects.requireNonNull(response.getData()).trafficFluencyFeatureCollection).features;

                //listan koko
                int koko = lista.size();

                //for-looppi, jossa jokainen listalla oleva tie käydään läpi+

                for (int t = 0; t <= koko - 1; t++) {


                    //haetaan yksittäisen tien koordinaatit String-muuttujaan
                    coordinates = lista.get(t).geometry.toString();
                    //poistetaan haetusta datasta turhat merkit
                    coordinates = coordinates.replaceAll(".*coordinates=", "");
                    coordinates = coordinates.replaceAll("[{}]", "");
                    coordinates = coordinates.replaceAll("\\[", "");
                    coordinates = coordinates.replaceAll("\\]", "");
                    //jos koordinaatit-kohta on tyhjä jonkun tien kohdalla, hypätään for loopissa seuraavaan tiehen.
                    if (coordinates.isEmpty()) {
                        continue;
                    }
                    //jaetaan string-muuttujassa olevat arvot eri indekseihin taulukkoon.
                    arrayForRoads = coordinates.split(", ");

                    //for-looppi, jossa edellä täytetystä taulukosta tallennetaan arvot double:na listaan
                    for (i = 0; i < arrayForRoads.length; i++) {

                        //joka toinen arvo on longitude-arvo, tällä if-lauseella varmistetaan että oikeat arvot menevät oikeaan listaan.
                        if (i % 2 == 0) {

                            longListForRoads.add(Double.parseDouble(arrayForRoads[i]));

                        } else {

                            latListForRoads.add(Double.parseDouble(arrayForRoads[i]));
                        }
                    }
                    //tässä for-loopissa käydään läpi edellä täytettyjä longitude- ja latitude listoja, ja tallennetaan niistä tiedot edelleen road-geopoint listaan
                    for (int j = 0; j < latListForRoads.size(); j++) {

                        road.add(new GeoPoint(latListForRoads.get(j), longListForRoads.get(j)));

                    }

                    //haetaan yksittäisten teiden liikenteensujuvuusdataa
                    String trafficFlow = response.getData().trafficFluencyFeatureCollection.features.get(roadNumber).properties.trafficFlow.rawValue();
                    String roadDirection = response.getData().trafficFluencyFeatureCollection.features.get(roadNumber).properties.trafficDirectionName;
                    String roadName = response.getData().trafficFluencyFeatureCollection.features.get(roadNumber).properties.name;

                    String averageSpeed = String.valueOf(response.getData().trafficFluencyFeatureCollection.features.get(roadNumber).properties.averageSpeed);
                    if (averageSpeed == null) {
                        averageSpeed = "";
                    }


                    //funktio, jolla tie piirretään kartalle
                    createRoad(road, trafficFlow, roadDirection, roadName, averageSpeed);

                    //muuttuja, jolla seurataan missä tien numerossa ollaan menossa.
                    roadNumber++;

                    //tyhjennetään edellä käytetyt listat
                    road.clear();
                    latListForRoads.clear();
                    longListForRoads.clear();

                    //päivitetään kartta
                    //map.invalidate();


                }
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {

            }
        });

    }
    private void createRoad(ArrayList<GeoPoint> arrayList, String trafficFlow, String roadDirection, String roadName, String averageSpeed) {
        //tiet piirretään kartalle Polyline:nä, parametrina map että saadaan infowindow-näkyviin kun tietä klikkaa
         uusiTie = new Polyline(map);


        //varmistetaan että haetussa datassa on vain uniikkeja arvoja
        Set<GeoPoint> setti2 = new LinkedHashSet<>(arrayList);
        arrayList.clear();
        arrayList.addAll(setti2);
        setti2.clear();

//lisätään geopoint listan pisteet kartalle
        uusiTie.setPoints(arrayList);

        String directionHelp = "suunta ";
        String directionResult = "";
        String averageSpeedHelp = "keskinopeus ";
        String avgSpeedResult = "";
        String kmh = "km/h";


        if (roadDirection != null) {

            directionResult = directionHelp.concat(roadDirection);
        }

        if (averageSpeed != null) {

            avgSpeedResult = averageSpeedHelp.concat(averageSpeed);
            avgSpeedResult = avgSpeedResult.concat(kmh);
        }
        if (avgSpeedResult.contains("null")) {
            avgSpeedResult = "";
        }
        String roadInfoResult = directionResult + "\n" + " Tien nimi " + roadName + "\n" + avgSpeedResult;

        //liikenteensujuvuuden mukaan väritetään tiet tietynvärisiksi
        switch (trafficFlow) {
            case "TRAFFIC_FLOW_NORMAL":
                uusiTie.getOutlinePaint().setColor(Color.GREEN);
                uusiTie.getOutlinePaint().setStrokeWidth(6);
                uusiTie.setTitle(roadInfoResult);
                break;
            case "TRAFFIC_HEAVIER_THAN_NORMAL":
                uusiTie.getOutlinePaint().setColor(Color.RED);
                uusiTie.getOutlinePaint().setStrokeWidth(6);
                String ruuhka = " Ruuhkauntunut tie";

                roadInfoResult = ruuhka + "\n" + roadInfoResult;
                uusiTie.setTitle(roadInfoResult);
                break;
            case "TRAFFIC_MUCH_HEAVIER_THAN_NORMAL":
                uusiTie.getOutlinePaint().setColor(Color.rgb(150, 0, 0));
                uusiTie.getOutlinePaint().setStrokeWidth(6);
                uusiTie.setTitle("todella ruuhkautunut tie" + "\n" + roadName + "\n" + avgSpeedResult);

                break;
            case "TRAFFIC_FLOW_UNKNOWN":
                uusiTie.getOutlinePaint().setColor(Color.BLACK);
                uusiTie.getOutlinePaint().setStrokeWidth(3);
                uusiTie.setTitle("ei dataa");
                break;
        }
        roadList.add(uusiTie);
        //lisätään karttaan polyline, joka sisältää yksittäisen tien koordinaatit


            map.getOverlays().add(0,uusiTie);

        arrayList.clear();
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


                        //haetaan queryn koko jotta päästään käsiksi kaikkiin datoihin stopsByBboxin alla
                        for(int i = 0; i<response.getData().stopsByBbox().size()-1; i++)
                        {
                            //Haetaan queryn koko uudestaan kohdasta stoptimesWithoutPatterns jotta päästään käsiksi dataan stoptimesWithOutPatternsissä
                            for(int j = 0; j < response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().size()-3;j++) {
                                //4 seuraavan saapumisaika pysäkille
                                int scheduledArrival = response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival();
                                int scheduledArrival1 = response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival();
                                int scheduledArrival2 = response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival();
                                int scheduledArrival3 = response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival();

                                //muutetaan aika Date muotoon ja kerrotaan se 1000 koska se muuttaa nuo sekunnit millisekunneiksi
                                Date time = new Date(scheduledArrival * 1000L);
                                Date time1 = new Date(scheduledArrival1 * 1000L);
                                Date time2 = new Date(scheduledArrival2 * 1000L);
                                Date time3 = new Date(scheduledArrival3 * 1000L);
                                //formatoidaan aika muotoon HH:mm jotta saadaan aika näyttämään 24 tunnin muodossa
                                SimpleDateFormat  timeFormat = new SimpleDateFormat("HH:mm");
                                String sTime = timeFormat.format(time);
                                String sTime1 = timeFormat.format(time1);
                                String sTime2 = timeFormat.format(time2);
                                String sTime3 = timeFormat.format(time3);

                                //lisätään tiedot markkeriin ja annetaan markkerille sen sijainti
                                busMarkerList.add(addMarkerBusStops(new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()),
                                        response.getData().stopsByBbox().get(i).name(),
                                        response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
                                                sTime + "\n" +

                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
                                                sTime1 + "\n" +

                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
                                                sTime2 + "\n" +


                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
                                                response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
                                                sTime3
                                ));



                            }
                        }

                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("Apollo", "toimiiko vai ei error", e);
                    }
                });
    }

}