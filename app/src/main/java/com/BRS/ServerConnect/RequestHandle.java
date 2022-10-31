package com.BRS.ServerConnect;

import android.util.Log;

import com.BRS.baseball.CameraAction;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;

public class RequestHandle {
    private static final String baseurl = "http://192.168.100.124:30000";

    private static int DEFAULT_TIMEOUT = 1000;

    private static OkHttpClient httpClient =
            new OkHttpClient.Builder().connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .build();

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseurl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()).build();

    private static Request_Interface requestInterface = retrofit.create(Request_Interface.class);

    private static String camera_position = "";
    private static boolean startRecord = false;

    public static void setCamera_position(String s){
        camera_position = s;
    }
    public static void setStartRecord(boolean b){startRecord = b;}

    public static boolean uploadVideo(File file){
        RequestBody videodirPart = RequestBody.create(MultipartBody.FORM, camera_position);
        RequestBody videoPart = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part bodyfile = MultipartBody.Part.createFormData("recordFile", file.getName(), videoPart);

        Call<ResponseBody> call = requestInterface.uploadVideo(videodirPart,bodyfile);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response_raw) {
                String recordfilepath_in_server = "";
                try {
                    recordfilepath_in_server = response_raw.body().string();;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e("Upload Success", "result = " + recordfilepath_in_server);
                file.delete();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error", t.getMessage());
            }
        });
        return true;
    }

    public static boolean uploadImage(File file) throws Exception {
        RequestBody videoPart = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part bodyfile = MultipartBody.Part.createFormData("imagefile", file.getName(), videoPart);
        Call<ResponseBody> call = requestInterface.uploadImage(bodyfile);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response_raw) {
                String message = "";
                try {
                    message = response_raw.body().string();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e("Upload Success", "message = " + message);
                Log.d("File delete ", String.valueOf(file.delete()));
                CameraAction.setFlag(Boolean.valueOf(message));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error", t.getMessage());
            }
        });
        return startRecord;
    }

}
