package com.example.ouluapp;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public class PlaceholderAPI {
    @GET("posts")
    static Call<List> getPosts() {
        return null;
    }
}
