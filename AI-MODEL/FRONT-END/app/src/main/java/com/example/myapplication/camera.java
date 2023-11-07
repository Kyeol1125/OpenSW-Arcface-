package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class camera extends AppCompatActivity {
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Button captureButton;
    private ImageView photoImageView;

    private static final String BUCKET_NAME = "s3-driver-upload";
    private static final String S3_DIRECTORY = "/License";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        captureButton = findViewById(R.id.captureButton);
        photoImageView = findViewById(R.id.photoImageView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera != null) {
                    captureButton.setEnabled(false);
                    camera.takePicture(null, null, pictureCallback);
                }
            }
        });

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera = Camera.open();
                    camera.setPreviewDisplay(holder);

                    // 미리보기 회전 설정
                    camera.setDisplayOrientation(90); // 원하는 회전 각도로 변경 (예: 90도는 시계 방향으로 회전)

                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap photoBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            if (photoBitmap != null) {
                // 이미지뷰에 표시
                photoImageView.setImageBitmap(photoBitmap);
                photoImageView.setRotation(90);
                photoImageView.setVisibility(View.VISIBLE);

                // 이미지를 S3로 업로드
                uploadImageToS3(photoBitmap);
            } else {
                Toast.makeText(getApplicationContext(), "이미지가 null입니다", Toast.LENGTH_SHORT).show();
            }

            // 사진 찍은 후 버튼을 다시 활성화
            captureButton.setEnabled(true);
        }
    };



    private void uploadImageToS3(Bitmap bitmap) {
        AWSCredentials awsCredentials = new BasicAWSCredentials("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY");
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials, Region.getRegion(Regions.AP_NORTHEAST_2));

        TransferUtility transferUtility = TransferUtility.builder()
                .s3Client(s3Client)
                .context(this)
                .build();

        String fileName = S3_DIRECTORY + "uploaded_image.jpg";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        InputStream inputStream = new ByteArrayInputStream(stream.toByteArray());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(stream.size()); // Set content length to the stream size

        TransferObserver uploadObserver = transferUtility.upload(
                BUCKET_NAME, // The name of the S3 bucket where you want to upload the file
                fileName,    // The name/key of the file in the S3 bucket
                new File(fileName), // The file you want to upload (in your case, it should be the image file)
                new ObjectMetadata()
        );


        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Toast.makeText(getApplicationContext(), "업로드 완료", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(camera.this, loadingpage.class);
                    startActivity(intent);

                }
            }

            @Override
            public void onProgressChanged(int id, long current, long total) {
                // 업로드 진행 상황을 추적할 수 있습니다.
            }

            @Override
            public void onError(int id, Exception ex) {
                Toast.makeText(getApplicationContext(), "업로드 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
