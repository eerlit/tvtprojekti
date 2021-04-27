package com.example.ouluapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntegerRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.ouluapp.StopsByBboxQuery;


import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    Context ctx;
    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));


        setContentView(R.layout.activity_main);
        getBusStops();


        map = (MapView) findViewById(R.id.map);
        //map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);



        IMapController mapController = map.getController();
        mapController.setZoom(14);
        GeoPoint startPoint = new GeoPoint(65.012615, 25.471453);
        mapController.setCenter(startPoint);

        //laitteen paikannus
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx),map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        //kartan pyörittäminen sormilla
        mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(this.mRotationGestureOverlay);

        requestPermissionsIfNecessary(new String[] {
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        });




        map.setMultiTouchControls(true);

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
                                //tunnit ja minuutit uusiksi if lausekkeen sisälle. Ongelmana voi ilmetä jos scheduledarrival kohdasta 0 = 17:50 ja kohdata 1 aika on = 18:01. Näyttääkö se ajan
                                //muodossa 17:50 ja 18:1 <- näyttää tälleen vittu
                                if (hours <= 9 && minutes <= 9) {
                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +


                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
                                                    "0" +response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,

                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees

                                } else if (minutes <= 9) {
                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60  + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60  + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":0" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,
                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees

                                } else if (hours <= 9) {
                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
                                                    "0" + response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,

                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees

                                } else if (hours >= 9 && minutes >= 9) {

                                    stops.add(new OverlayItem(response.getData().stopsByBbox().get(i).name(),
                                            response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(0).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(1).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(2).realtimeArrival() / 60 % 60 + "\n" +

                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).headsign() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).trip().routeShortName() + " " +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 / 60 + ":" +
                                                    response.getData().stopsByBbox().get(i).stoptimesWithoutPatterns().get(3).realtimeArrival() / 60 % 60,
                                            new GeoPoint(response.getData().stopsByBbox().get(i).lat(), response.getData().stopsByBbox().get(i).lon()))); // Lat/Lon decimal degrees
                                }
                            }
                        }

                            Drawable busStop = ctx.getResources().getDrawable(R.drawable.busicon);

                            //the overlay
                            ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(stops, busStop, busStop, Color.WHITE,
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
                            Log.d("MainActivity", "Response: " + response.getData().stopsByBbox().get(0).stoptimesWithoutPatterns.size());
                        }




                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("Apollo", "toimiiko vai ei error", e);
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