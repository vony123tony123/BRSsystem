package com.BRS.baseball;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class CameraAction {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView viewFinder;
    private Button videocaptureBtn;
    private VideoCapture<Recorder> videoCapture;
    private ImageCapture imageCapture;
    private Recorder recorder;
    private Recording recording;
    private LifecycleOwner lifeCycleOwner;
    private Context context;
    private ProcessCameraProvider cameraProvider;

    private static final String TAG = "Baseball";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static boolean flag = false;
    private RequestHandle requestHandle;
    private Camera camera;

    public CameraAction(ListenableFuture<ProcessCameraProvider> cameraProviderFuture,
                        PreviewView viewFinder, Button videocaptureBtn,
                        LifecycleOwner lifeCycleOwner,
                        Context context,
                        RequestHandle requestHandle) {
        this.cameraProviderFuture = cameraProviderFuture;
        this.viewFinder = viewFinder;
        this.lifeCycleOwner = lifeCycleOwner;
        this.context = context;
        this.videocaptureBtn = videocaptureBtn;
        this.requestHandle = requestHandle;
    }

    public static void setFlag(boolean b){
        flag = b;
    }

    protected void enableCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindImageCapture();
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(context));
    }

    void bindVideoCapture() {
        recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
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

    void bindImageCapture(){
        imageCapture = new ImageCapture.Builder().build();
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    lifeCycleOwner, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            e.printStackTrace();
        }
    }

    protected void captureVideo() throws IOException {
        if(videoCapture == null) throw new AssertionError("videoCapture isn't initial");

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINESE).format(System.currentTimeMillis());
        FileOutputOptions fileOutputOptions = new FileOutputOptions
                .Builder(File.createTempFile(name,".mp4"))
                .build();

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
                                    Uri recorduri = ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                                    String s = "Video capture succeeded: " + recorduri;
                                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, s);
                                    File file = new File(recorduri.getPath());
                                    requestHandle.uploadVideo(file);
                                }else {
                                    Log.e(TAG, "Video capture ends with error: " +
                                            ((VideoRecordEvent.Finalize) videoRecordEvent).getError());
                                }
                            }
                        });
    }

    protected void closeVideoCapture(){
        if(recording != null){
            recording.close();
            recording = null;
            return;
        }
    }

    protected boolean imageCapture() throws Exception {
        if (imageCapture == null) throw new AssertionError("imageCapture isn't initial");
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINESE).format(System.currentTimeMillis());
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(File.createTempFile(name, ".jpg")).build();
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(context,"Success:"+outputFileResults.getSavedUri().getPath(),Toast.LENGTH_SHORT).show();
                        Uri uri = outputFileResults.getSavedUri();
                        File file = new File(uri.getPath());
                        try {
                            requestHandle.uploadImage(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("Failed", exception.getMessage());
                    }
        });
        return flag;
    }

    public void enableFlash(){
        camera.getCameraControl().enableTorch(true);
    }

    public void disableFlash(){
        camera.getCameraControl().enableTorch(false);
    }
}
