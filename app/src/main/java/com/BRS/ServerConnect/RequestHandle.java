package com.BRS.ServerConnect;

import android.os.Handler;
import android.util.Log;

import com.BRS.baseball.MainActivity;

import java.io.File;
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
    private final String baseurl = "http://140.115.51.180:30000";
    private int DEFAULT_TIMEOUT = 1000;
    private OkHttpClient httpClient =
            new OkHttpClient.Builder().connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .build();
    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseurl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()).build();
    private Request_Interface requestInterface = retrofit.create(Request_Interface.class);
    private String camera_position = "";
    private Handler handler;
    private static boolean startRecord = false;

    public void uploadVideo(File file){
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
        if(camera_position.equals("side")){
            handler.sendEmptyMessageDelayed(MainActivity.checkingStartRecord, 800);
        }
    }

    public boolean uploadImage(File file) throws Exception {
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
//                CameraAction.setFlag(Boolean.valueOf(message));
               RequestHandle.setStartRecord(Boolean.valueOf(message));
               handler.sendEmptyMessage(MainActivity.FinishImageUpload);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error", t.getMessage());
                handler.sendEmptyMessage(MainActivity.FinishImageUpload);
            }
        });
        return startRecord;
    }

    public void checkStartRecording(){
        Call<ResponseBody> call = requestInterface.checkStartRecording();
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
                RequestHandle.setStartRecord(Boolean.valueOf(message));
                handler.sendEmptyMessage(MainActivity.getStartRecord);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error", t.getMessage());
                handler.sendEmptyMessage(MainActivity.getStartRecord);
            }
        });
    }

    public void setCamera_position(String s){
        camera_position = s;
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    public static void setStartRecord(boolean startRecord) {
       RequestHandle.startRecord = startRecord;
    }

    public static boolean getStartRecord(){
        return startRecord;
    }

}
