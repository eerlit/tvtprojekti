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
    String[] cameraPhoto;
    String[] cameraName;
    Float x1, x2, y1, y2;
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


        cameraPhoto = (String[])intent.getSerializableExtra("CAMERA");
        cameraName = (String[])intent.getSerializableExtra("CAMERA_NAME");
        cameraPhotoLength= cameraPhoto.length-1;
        Picasso.with(context).load(cameraPhoto[photoID]).into(imageView);
        textView.setText(cameraName[photoID]);


        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent touchevent) {
                switch (touchevent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = touchevent.getX();
                        y1 = touchevent.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = touchevent.getX();
                        y2 = touchevent.getY();
                        if (x1 < x2) {
                            photoID++;
                            if(photoID > cameraPhotoLength){
                                photoID = 0;
                            }
                            Picasso.with(context).load(cameraPhoto[photoID]).into(imageView);

                        }
                        break;

                }
                return false;
            }

        });




    }
    public boolean onTouchEvent(MotionEvent touchevent){
        switch (touchevent.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1 = touchevent.getX();
                y1 = touchevent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchevent.getX();
                y2 = touchevent.getY();
                if(x1 < x2){
                    photoID++;
                    if(photoID > cameraPhotoLength){
                        photoID = 0;
                    }
                    System.out.println("PHOTOLENGTH: " +cameraPhotoLength);
                    System.out.println("PHOTOID: " +photoID);
                    Picasso.with(context).load(cameraPhoto[photoID]).into(imageView);
                    textView.setText(cameraName[photoID]);

                }
                if(x1 > x2){
                    photoID--;
                    if(photoID == -1){
                        photoID = cameraPhotoLength;
                    }
                    Picasso.with(context).load(cameraPhoto[photoID]).into(imageView);
                    textView.setText(cameraName[photoID]);
                }
                break;
        }
        return false;
    }
}
