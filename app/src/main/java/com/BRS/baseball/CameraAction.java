package com.BRS.baseball;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.BRS.ServerConnect.RequestHandle;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraAction {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView viewFinder;
    private Button videocaptureBtn;
    private VideoCapture<Recorder> videoCapture;
    private Recorder recorder;
    private Recording recording;
    private LifecycleOwner lifeCycleOwner;
    private Context context;
    private File recordedVideo;

    private static final String TAG = "Baseball";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    public CameraAction(ListenableFuture<ProcessCameraProvider> cameraProviderFuture,
                        PreviewView viewFinder, Button videocaptureBtn,
                        LifecycleOwner lifeCycleOwner, Context context) {
        this.cameraProviderFuture = cameraProviderFuture;
        this.viewFinder = viewFinder;
        this.lifeCycleOwner = lifeCycleOwner;
        this.context = context;
        this.videocaptureBtn = videocaptureBtn;
    }

    protected void enableCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(context));

        recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());


        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        Camera camera;
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    lifeCycleOwner, cameraSelector, preview, videoCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            e.printStackTrace();
        }
    }

    protected void captureVideo(){
        if(videoCapture == null) throw new AssertionError("videoCapture isn't initial");
        videocaptureBtn.setEnabled(false);
        Recording curRecording = recording;
        if(curRecording != null){
            curRecording.stop();
            recording = null;
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINESE).format(System.currentTimeMillis());
        File outputfile = null;
        try {
            outputfile = File.createTempFile(name,".mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputOptions fileOutputOptions = new FileOutputOptions
                .Builder(outputfile).build();

        recording = videoCapture.getOutput()
                .prepareRecording(context, fileOutputOptions)
                .start(ContextCompat.getMainExecutor(context),
                        videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                // Handle the start of a new active recording
                                videocaptureBtn.setEnabled(true);
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                                // Handles a finalize event for the active recording, checking Finalize.getError()
                                int error = finalizeEvent.getError();
                                if (error == VideoRecordEvent.Finalize.ERROR_NONE) {
                                    String s = "Video capture succeeded: " +
                                            ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, s);
                                    Uri recorduri = ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                                    File file = new File(recorduri.getPath());
                                    RequestHandle.upload(file);
                                }else {
                                    recording.close();
                                    recording = null;
                                    Log.e(TAG, "Video capture ends with error: " +
                                            ((VideoRecordEvent.Finalize) videoRecordEvent).getError());
                                }
                                videocaptureBtn.setEnabled(true);
                            }
                        });
    }

    protected void closeVideoCapture(){
        if(recording != null){
            recording.close();
            recording = null;
            videocaptureBtn.setEnabled(true);
            return;
        }
    }
}
