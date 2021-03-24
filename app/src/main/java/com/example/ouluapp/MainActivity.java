package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.GetAllCarParksQuery;

import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private  MyLocationNewOverlay mLocationOverlay = null;
    Context ctx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectToAPI();
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(14);
        GeoPoint startPoint = new GeoPoint(65.012615, 25.471453);
        mapController.setCenter(startPoint);

        requestPermissionsIfNecessary(new String[] {
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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

