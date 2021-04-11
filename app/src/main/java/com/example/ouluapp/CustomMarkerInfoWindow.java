package com.example.ouluapp;

import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

public class CustomMarkerInfoWindow extends MarkerInfoWindow {


    public CustomMarkerInfoWindow(MapView mapView) {
        super(R.layout.custom_dialog, mapView);
    }


    @Override
    public void onOpen(Object item) {
        Marker m = (Marker) item;
        String dateTime;

        TextView title = (TextView) mView.findViewById(R.id.map_popup_header);
        title.setText(m.getTitle());

        TextView snippet = (TextView) mView
                .findViewById(R.id.map_popup_body);
        String[] tempDate = m.getSnippet().split("SPLIT");

        if (tempDate != null){
            String[] dateParts = tempDate[tempDate.length-1].split("T");
            String[] timeParts = dateParts[1].split("Z");

            String[] timeParts2 = timeParts[0].split("\\.");
            String[] timeHMS = timeParts2[0].split(":");
            int hour = Integer.parseInt(timeHMS[0]);
            hour = hour + 3;
            String finalTimeParts = hour + ":" + timeHMS[1] + ":" + timeHMS[2].split("Z")[0];

            String[] dateSplit = dateParts[0].split("-");
            String finaldate = dateSplit[2] + "." + dateSplit[1] + "." + dateSplit[0];

            dateTime = finaldate + " " + finalTimeParts;
        }else {
            dateTime = "No Time";
        }
        String temp = "Lämpötila: " +tempDate[0] + "°C";
        String wind = "Tuulen Nopeus: " +tempDate[1] + "m/s";
        String moisture = "Kosteus: " +tempDate[2] + "%";
        String finalText = dateTime + "\n" + temp + "\n" + wind + "\n" + moisture;

        snippet.setText(finalText);


    }
}