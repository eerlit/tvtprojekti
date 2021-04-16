package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.exception.ApolloException;

import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;


import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.PolyOverlayWithIW;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private final int request_permissions_request_code = 1;
    private MapView map = null;

    private ArrayList<Double> latLista = new ArrayList<Double>();
    private ArrayList<Double> longLista = new ArrayList<Double>();
    private DatagramPacket response;



    Context ctx;
    String[] taulukko;
    int count = 0;



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
        getRoadCongestion();
        map.invalidate();



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

        private void getRoadCongestion( ){


            ApolloConnector.setupApollo().query(
              GetTrafficFluencyFeatureCollectionQuery.builder()
               .build()).enqueue(new ApolloCall.Callback<GetTrafficFluencyFeatureCollectionQuery.Data>() {

                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override

           public void onResponse(@NotNull Response<GetTrafficFluencyFeatureCollectionQuery.Data> response) {


                    int a = 0;
                    String derp;


                    ArrayList<GeoPoint> road = new ArrayList<>();

                    int roadNumber = 0;


                    int i = 0;



                    List<GetTrafficFluencyFeatureCollectionQuery.Feature> lista = Objects.requireNonNull(Objects.requireNonNull(response.getData()).trafficFluencyFeatureCollection).features;
                    int koko = lista.size();

                    for (int t =0; t <= koko-1; t++) {
                        //535 bugaa


                        derp = lista.get(t).geometry.toString();

                        derp = derp.replaceAll(".*coordinates=", "");
                        derp = derp.replaceAll("[{}]", "");
                        derp = derp.replaceAll("\\[", "");
                        derp = derp.replaceAll("\\]", "");
                        if(derp.isEmpty()) {
                            continue;
                        }
                            taulukko = derp.split(", ");

                            for (i = 0; i < taulukko.length; i++) {


                                if (i % 2 == 0) {

                                    longLista.add(Double.parseDouble(taulukko[i]));

                                } else {

                                    latLista.add(Double.parseDouble(taulukko[i]));
                                }
                            }

                            for (int j = 0; j < latLista.size(); j++) {




                                    road.add(new GeoPoint(latLista.get(j), longLista.get(j)));

                            }


                            String trafficFlow = response.getData().trafficFluencyFeatureCollection.features.get(roadNumber).properties.trafficFlow.rawValue();

                            createRoad(road, trafficFlow);


                            roadNumber++;

                            road.clear();
                            map.invalidate();
                            latLista.clear();
                            longLista.clear();

                    }
                    }





           @Override
           public void onFailure(@NotNull ApolloException e) {


           }
       });

    }

private void createRoad(ArrayList<GeoPoint> arrayList, String trafficFlow)
    {

        Polyline uusiTie = new Polyline();


        Set<GeoPoint> setti2 = new LinkedHashSet<>(arrayList);
        arrayList.clear();
        arrayList.addAll(setti2);
        setti2.clear();

        uusiTie.setPoints(arrayList);

        switch(trafficFlow){
            case "TRAFFIC_FLOW_NORMAL":
                uusiTie.getOutlinePaint().setColor(Color.GREEN);
                uusiTie.getOutlinePaint().setStrokeWidth(6);
                break;
            case "TRAFFIC_HEAVIER_THAN_NORMAL":
                uusiTie.getOutlinePaint().setColor(Color.rgb(255,59, 0));
                break;
            case "TRAFFIC_MUCH_HEAVIER_THAN_NORMAL":
                uusiTie.getOutlinePaint().setColor(Color.RED);
                break;
            case "TRAFFIC_FLOW_UNKNOWN":
                uusiTie.getOutlinePaint().setColor(Color.BLACK);
                uusiTie.getOutlinePaint().setStrokeWidth(5);
                break;
        }


        map.getOverlays().add(uusiTie);

            arrayList.clear();
    }


    }
