package com.BRS.baseball;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.BRS.ServerConnect.RequestHandle;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView viewFinder;
    private ArrayList<String> permissionlist = new ArrayList<String>();
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private Button videocaptureBtn;
    private Button uploadBtn;
    private CameraAction cameraAction;
    private Handler handler;
    private int repeatInternal = 6000;
    private Runnable repeatTask = null;

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
        uploadBtn = findViewById(R.id.uploadFile_button);
        uploadBtn.setOnClickListener(this);

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

        handler = new Handler();
        repeatTask = new Runnable() {
            @Override
            public void run() {
                try{
                    cameraAction.captureVideo();
                }finally {
                    handler.postDelayed(repeatTask, repeatInternal);
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
                if(videocaptureBtn.getText() == getString(R.string.start_capture)){
                    repeatTask.run();
                    videocaptureBtn.setText(R.string.stop_capture);
                }else{
                    handler.removeCallbacks(repeatTask);
                    cameraAction.closeVideoCapture();
                    videocaptureBtn.setText(R.string.start_capture);
                }
                break;
        }
    }
}