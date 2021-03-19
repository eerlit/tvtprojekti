package com.example.ouluapp;

import com.google.gson.annotations.SerializedName;

public class PlaceholderPost {
    private int cameraId;
    private String name;
    private double lat;
    private double lon;

    public int getCameraId() {
        return cameraId;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
