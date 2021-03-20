package com.example.ouluapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import org.jetbrains.annotations.NotNull;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private  MyLocationNewOverlay mLocationOverlay = null;


    List<GetAllCamerasQuery.Camera> cameras = new ArrayList<>();
    ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
    Context ctx;
    private String[] cameraURL, cameraDirection, cameraTime, cameraName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAllCameras();
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


    private  void updatemap(){
        for (int i = 0; i < cameras.size(); i++) {
            OverlayItem olItem = new OverlayItem(cameras.get(i).name, "Kamera", new GeoPoint(cameras.get(i).lat.doubleValue(), cameras.get(i).lon.doubleValue()));
            Drawable newMarker = ctx.getResources().getDrawable(R.drawable.ic_baseline_camera_alt_24);
            olItem.setMarker(newMarker);
            items.add(olItem);
        }

        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {


                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Intent intent = new Intent(ctx, CameraPhoto.class);
                        cameraURL = new String[cameras.get(index).presets.size()];
                        cameraDirection = new String[cameras.get(index).presets.size()];
                        cameraTime = new String[cameras.get(index).presets.size()];
                        cameraName = new String[cameras.get(index).presets.size()];

                        for(int i = 0; i < cameras.get(index).presets.size(); i++){
                            Log.d("kamerat", cameras.get(index).presets.get(i).imageUrl);
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

        map.getOverlays().add(mOverlay);
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
                        updatemap();
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.d("MainActigetAllCameras", "Exception " + e.getMessage(), e);
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