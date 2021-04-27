package com.example.ouluapp;

import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

public class BusDialog extends MarkerInfoWindow {

    public BusDialog(MapView mapView) {
        super(R.layout.custom_dialog, mapView);}
    @Override
    public void onOpen(Object item) {
        Marker m = (Marker) item;


        TextView title = (TextView) mView.findViewById(R.id.map_popup_header);
        title.setText(m.getTitle());

        TextView snippet = (TextView) mView
                .findViewById(R.id.map_popup_body);
        snippet.setText(m.getSnippet());
    }
}
