package com.BRS.baseball;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.BRS.ServerConnect.RequestHandle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int startImageCapture = 1;
    public static final int FinishImageUpload = 2;
    public static final int startVideoCapture = 3;
    public static final int finishVideoCapture_Behind = 4;
    public static final int checkingStartRecord = 5;
    public static final int getStartRecord = 6;
    public static final int finishVideoCapture_Side = 7;
    public static final int flashOpen = 8;
    public static final int flashClose = 9;

    private ArrayList<String> permissionlist = new ArrayList<String>();
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private Button videocaptureBtn;
    private CameraAction cameraAction;
    private String cameraPosition = "";
    private Handler handler;
    private boolean isHandlerWake;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        cameraPosition = intent.getStringExtra("cameraPosition");

        permissionlist.add(Manifest.permission.CAMERA);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            permissionlist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        videocaptureBtn = findViewById(R.id.video_capture_button);
        videocaptureBtn.setOnClickListener(this);

        RequestHandle requestHandle = new RequestHandle();
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                boolean startRecording;
                if (isHandlerWake) {
                    switch (msg.what) {
                        case startImageCapture:
                            //Behind app start capture imageCapture
                            try {
                                cameraAction.imageCapture();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case FinishImageUpload:
                            //Image Capture which behind app upload finish
                            startRecording = requestHandle.getStartRecord();
                            if (startRecording == false) {
                                handler.sendEmptyMessage(startImageCapture);
                            } else {
                                  handler.sendEmptyMessage(flashOpen);
                            }
                            break;
                        case startVideoCapture:
                            try {
                                cameraAction.bindVideoCapture();
                                cameraAction.captureVideo();
                            } catch (IOException e) {
                                Log.e("Handler of Case 3", "cameraAction have an IOExcetion");
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessageDelayed(finishVideoCapture_Behind, 4000);
                            break;
                        case finishVideoCapture_Behind:
                            cameraAction.closeVideoCapture();
                            cameraAction.bindImageCapture();
                            sendEmptyMessage(startImageCapture);
                            break;
                        case checkingStartRecord:
                            requestHandle.checkStartRecording();
                            break;
                        case getStartRecord:
                            startRecording = requestHandle.getStartRecord();
                            if (startRecording == false) {
                                handler.sendEmptyMessage(checkingStartRecord);
                            } else {
                                try {
                                    cameraAction.captureVideo();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                handler.sendEmptyMessageDelayed(finishVideoCapture_Side, 4000);
                            }
                            break;
                        case finishVideoCapture_Side:
                            cameraAction.closeVideoCapture();
//                            handler.sendEmptyMessageDelayed(MainActivity.checkingStartRecord, 800);
                            break;
                        case flashOpen:
                            cameraAction.enableFlash();
                            handler.sendEmptyMessageDelayed(flashClose, 1000);
                            break;
                        case flashClose:
                            cameraAction.disableFlash();
                            handler.sendEmptyMessage(startVideoCapture);
                    }
                }
            }
        };
        requestHandle.setCamera_position(cameraPosition);
        requestHandle.setHandler(handler);
        isHandlerWake = false;

        cameraAction = new CameraAction(ProcessCameraProvider.getInstance(this),
                findViewById(R.id.viewFinder),
                videocaptureBtn,
                this,
                this,
                requestHandle);

        // Request camera permissions
        if (allPermissionsGranted()) {
            cameraAction.enableCamera();
        } else {
            requestPermission();
        }
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
            dir.delete();
            return dir.mkdir();
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
                isHandlerWake = true;
                if(videocaptureBtn.getText() == getString(R.string.start_capture)) {
                    if(cameraPosition.equals("behind")){
//                        cameraAction.bindImageCapture();
//                        behindCameraTask.run();
                        cameraAction.bindImageCapture();
                        handler.sendEmptyMessage(startImageCapture);
                    }else if(cameraPosition.equals("side")){
//                        cameraAction.bindVideoCapture();
//                        sideCameraTask.run();
                        cameraAction.bindVideoCapture();
                        handler.sendEmptyMessage(checkingStartRecord);
                    }
                    videocaptureBtn.setText(R.string.stop_capture);
                }else{
//                    if(cameraPosition.equals("behind")){
//                        handler.removeCallbacks(closeTask_behind);
//                        handler.removeCallbacks(behindCameraTask);
//                    }else if(cameraPosition.equals("side")){
//                        handler.removeCallbacks(closeTask_side);
//                        handler.removeCallbacks(sideCameraTask);
//                    }
                    isHandlerWake = false;
                    deleteCache(this);
                    videocaptureBtn.setText(R.string.start_capture);
                }
                break;
        }
    }
}