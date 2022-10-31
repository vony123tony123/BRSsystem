package com.BRS.baseball;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ArrayList<String> permissionlist = new ArrayList<String>();
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private Button videocaptureBtn;
    private CameraAction cameraAction;
    private Handler handler;
    private int repeatInternal = 1000;
    private Runnable repeatTask = null;
    private boolean result = false;
    private TimerTask closeTask = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionlist.add(Manifest.permission.CAMERA);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            permissionlist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        videocaptureBtn = findViewById(R.id.video_capture_button);
        videocaptureBtn.setOnClickListener(this);

        cameraAction = new CameraAction(ProcessCameraProvider.getInstance(this),
                findViewById(R.id.viewFinder),
                videocaptureBtn,
                this,
                this);

        // Request camera permissions
        if (allPermissionsGranted()) {
            cameraAction.enableCamera();
        } else {
            requestPermission();
        }

        closeTask = new TimerTask() {
            @Override
            public void run() {
                cameraAction.closeVideoCapture();
                cameraAction.bindImageCapture();
                cameraAction.setFlag(false);
                handler.post(repeatTask);
            }
        };

        handler = new Handler();
        repeatTask = new Runnable() {
            @Override
            public void run() {
                try{
                    result = cameraAction.imageCapture();
                }catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    if(result == false){
                        handler.postDelayed(repeatTask, repeatInternal);
                    }else{
                        cameraAction.bindVideoCapture();
                        try {
                            cameraAction.captureVideo();
                            handler.postDelayed(closeTask, 6000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    private boolean allPermissionsGranted() {
        for (String permission:permissionlist) {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private void requestPermission() {
        String[] permissionStringlist = permissionlist.toArray(new String[0]);
        ActivityCompat.requestPermissions(
                this,
                permissionStringlist,
                REQUEST_CODE_PERMISSIONS
        );
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) { e.printStackTrace();}
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                cameraAction.enableCamera();
            }else{
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.video_capture_button:
                if(videocaptureBtn.getText() == getString(R.string.start_capture)) {
                    cameraAction.bindImageCapture();
                    repeatTask.run();
                    videocaptureBtn.setText(R.string.stop_capture);
                }else{
                    handler.removeCallbacks(closeTask);
                    handler.removeCallbacks(repeatTask);
                    cameraAction.closeVideoCapture();
                    deleteCache(this);
                    videocaptureBtn.setText(R.string.start_capture);
                }
                break;
        }
    }
}