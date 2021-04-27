package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;



import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;


import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;

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


public class MainActivity extends AppCompatActivity {

    private final int request_permissions_request_code = 1;
    private MapView map = null;
    private ArrayList<Double> latListForRoads = new ArrayList<Double>();
    private ArrayList<Double> longListForRoads = new ArrayList<Double>();
    private ArrayList<Double> longListForAnnouncements = new ArrayList<Double>();
    private ArrayList<Double> latListForAnnouncements = new ArrayList<Double>();

    Context ctx;
    String[] arrayForRoads;
    String[] arrayForAnnouncements;
    Toolbar toolbar;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        IMapController mapController = map.getController();
        mapController.setZoom(14);
        GeoPoint startPoint = new GeoPoint(65.0158377521294, 25.470374591550694);
        mapController.setCenter(startPoint);



        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        });
        getAllTrafficAnnouncements();
        getRoadCongestion();
        updateRoadMap();


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
                    request_permissions_request_code);
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
                    request_permissions_request_code);
        }
    }

    private void updateRoadMap() {
        TimerTask updateMapTimer;
        final Handler handler = new Handler();
        Timer myTimer = new Timer();

        updateMapTimer = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getRoadCongestion();
                        System.out.println("taalla");
                    }
                });
            }
        };
        myTimer.schedule(updateMapTimer, 30000, 30000);



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


                roadWorkMarker.setTitle(announcementTitle);
                roadWorkMarker.setSnippet(announcementDescription);
                map.getOverlays().add(roadWorkMarker);

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
                //for-looppi, jossa jokainen listalla oleva tie käydään läpi
                for (int t = 0; t <= koko - 1; t++) {
                    //535 bugaa

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
                    map.invalidate();

                }
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {

            }
        });

    }

    private void createRoad(ArrayList<GeoPoint> arrayList, String trafficFlow, String roadDirection, String roadName, String averageSpeed) {
        //tiet piirretään kartalle Polyline:nä, parametrina map että saadaan infowindow-näkyviin kun tietä klikkaa
        Polyline uusiTie = new Polyline(map);
        //uusiTie.setTitle(String.valueOf(roadName));

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
        //lisätään karttaan polyline, joka sisältää yksittäisen tien koordinaatit
        map.getOverlays().add(uusiTie);

        arrayList.clear();
    }



}
