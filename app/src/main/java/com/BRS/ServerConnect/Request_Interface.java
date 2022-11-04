package com.BRS.ServerConnect;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Request_Interface {
    @Multipart
    @POST("/upload_video/")
    Call<ResponseBody> uploadVideo(
            @Part("videodir") RequestBody videodir,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("/behind_upload_image/")
    Call<ResponseBody> uploadImage(
            @Part MultipartBody.Part file
    );

    @GET("/side_waiting/")
    Call<ResponseBody> checkStartRecording();
}
