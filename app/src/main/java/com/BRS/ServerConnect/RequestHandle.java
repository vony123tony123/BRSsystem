package com.BRS.ServerConnect;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;

public class RequestHandle {
    private static final String baseurl = "http://192.168.100.124:30000";

    private static int DEFAULT_TIMEOUT = 1000;

//    private static HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

    private static OkHttpClient httpClient =
            new OkHttpClient.Builder().connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
//                    .addInterceptor(loggingInterceptor)
                    .build();

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseurl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()).build();

    private static Request_Interface requestInterface = retrofit.create(Request_Interface.class);

    public static boolean upload(File file){
        RequestBody videoPart = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part bodyfile = MultipartBody.Part.createFormData("recordFile", file.getName(), videoPart);

        Call<ResponseBody> call = requestInterface.upload(bodyfile);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response_raw) {
                String recordfilepath_in_server = "";
                try {
                    recordfilepath_in_server = response_raw.message();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.v("Upload Success", "response path =" + recordfilepath_in_server);
                file.delete();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error", t.getMessage());
            }
        });
        return true;
    }


}
