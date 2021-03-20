package com.example.ouluapp;

import android.app.VoiceInteractor;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.provider.ContactsContract;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

public class CameraPhoto extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    String[] cameraPhoto, cameraName, time, cameraDirection;
    Float x1, x2;
    int cameraPhotoLength = 0;
    int photoID = 0;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cameraphoto);
        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);
        Intent intent = getIntent();
        context = getApplicationContext();

        cameraDirection = (String[])intent.getSerializableExtra("CAMERA_DIRECTION");
        cameraPhoto = (String[])intent.getSerializableExtra("CAMERA");
        cameraName = (String[])intent.getSerializableExtra("CAMERA_NAME");
        time = (String[])intent.getSerializableExtra("TIME");

        cameraPhotoLength= cameraPhoto.length-1;
        Picasso.with(context).load(cameraPhoto[photoID]).into(imageView);
        switchPhoto(photoID);

    }
    public boolean onTouchEvent(MotionEvent touchevent){
        switch (touchevent.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1 = touchevent.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchevent.getX();
                if(x1 < x2){
                    photoID++;
                    if(photoID > cameraPhotoLength){
                        photoID = 0;
                    }
                    switchPhoto(photoID);

                }
                if(x1 > x2){
                    photoID--;
                    if(photoID == -1){
                        photoID = cameraPhotoLength;
                    }
                    switchPhoto(photoID);
                }
                break;
        }
        return false;
    }
    public void switchPhoto(int id){
        Picasso.with(context).load(cameraPhoto[photoID]).into(imageView);
        String[] dateParts = time[photoID].split("T");
        String[] timeParts = dateParts[1].split("z");
        String[] finalTimeParts = timeParts[0].split("\\.");


        String[] dateSplit = dateParts[0].split("-");
        String finaldate = dateSplit[2] + "." + dateSplit[1] + "." + dateSplit[0];
        
        String dateTime = finaldate + " " + finalTimeParts[0];

        textView.setText(cameraName[id] + "\n" + cameraDirection[id] + "\n" + dateTime);
        System.out.println(finalTimeParts[0]);
    }
}
