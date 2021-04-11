package com.example.ouluapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

public class CustomCameraMarkerInfoWindow extends MarkerInfoWindow {
    int photoID = 0;
    String cameraString, dataString;
    String[] cameraPhoto, dataDT, dataSplit;
    int cameraPhotoLength;
    float x1,x2;
    ImageView imageView = (ImageView) mView.findViewById(R.id.camera_image_view);
    TextView snippet = (TextView) mView.findViewById(R.id.map_popup_body);

    public CustomCameraMarkerInfoWindow(MapView mapView) {
        super(R.layout.camera_popup, mapView);

    }

    @Override
    public void onOpen(Object item) {
        Marker m = (Marker) item;

        TextView title = (TextView) mView.findViewById(R.id.map_popup_header);
        title.setText(m.getTitle());

        dataString = m.getSnippet();

        cameraString = m.getSubDescription();
        cameraPhoto = cameraString.split("SPLIT");
        cameraPhotoLength = cameraPhoto.length-1;

        switchPhoto(photoID);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent touchevent) {

                switch (touchevent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        x1 = touchevent.getX();
                        System.out.println("ACTION DOWN");
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = touchevent.getX();
                        System.out.println("ACTION UP");
                        if(x1 < x2){
                            photoID--;
                            if(photoID == -1){
                                photoID = cameraPhotoLength;
                            }
                            switchPhoto(photoID);


                        }
                        if(x1 > x2){
                            photoID++;
                            if(photoID > cameraPhotoLength){
                                photoID = 0;
                            }
                            switchPhoto(photoID);

                        }
                        break;
                }
                return true;
            }
        });


    }
    public void switchPhoto(int id){
        String dateTime;
        String finalSubTitle;
        Picasso.get().load(cameraPhoto[id]).into(imageView);
        String  weather = "NULL";
        dataSplit = dataString.split("SPLIT");
        dataDT = dataSplit[id].split("dt");
        if (!dataSplit[dataSplit.length-1].contains(":")){
            weather = dataSplit[dataSplit.length-1];
        }



        if (dataDT[1] != null){
            String[] dateParts = dataDT[1].split("T");
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



            finalSubTitle = dataDT[0] + " \n" + dateTime + "\n\n" +"Kuva " +(photoID+1) + "/" +(cameraPhotoLength+1);

        if (weather != "NULL"){
            String[] moistWindTemp = weather.split("WSPL");
            String temp = "Lämpötila: " +moistWindTemp[2] + "°C";
            String wind = "Tuulen Nopeus: " +moistWindTemp[1] + "m/s";
            String moisture = "Kosteus: " +moistWindTemp[0] + "%";
            finalSubTitle = dataDT[0] + " \n" + dateTime + " \n" + temp + "\n" + wind + "\n"+ moisture + "\n\n" + "Kuva " +(photoID+1) + "/" +(cameraPhotoLength+1);
        }


        snippet.setText(finalSubTitle);

    }
}