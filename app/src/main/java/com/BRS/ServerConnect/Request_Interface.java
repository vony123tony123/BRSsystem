package com.BRS.ServerConnect;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;

public interface Request_Interface {
    @Multipart
    @POST("/upload/")
    Call<ResponseBody> upload(
            @Part MultipartBody.Part file
    );

    @FormUrlEncoded
    @POST("/wav2midi/")
    Call<ResponseBody> wav2midiRequest(
            @Field("filepath") String path
    );

    @FormUrlEncoded
    @Streaming
    @POST("/file_download/")
    Call<ResponseBody> downloadRequest(
            @Field("filepath") String path
    );
}
