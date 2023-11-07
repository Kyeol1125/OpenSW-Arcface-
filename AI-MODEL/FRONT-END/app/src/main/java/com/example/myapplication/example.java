package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.net.Uri;
import android.provider.MediaStore;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.File;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;


public class example extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        button1 = findViewById(R.id.button1);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissionAndOpenGallery();
            }
        });
    }

    private void checkPermissionAndOpenGallery() {
        // 갤러리 접근 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 부여되지 않은 경우 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 부여된 경우 갤러리 열기
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();
                uploadImageToS3(imageUri);
            }
        }
    }

    private void uploadImageToS3(Uri imageUri) {
        // AWS 자격 증명 설정
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIASN5TEJIHK4ISKRVG", "30woDkDPqYNx8uJfjDNkmDFmp3VCYW6jtXU/Jr6i");
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials, Region.getRegion(Regions.AP_NORTHEAST_2));

        // TransferUtility 설정
        TransferUtility transferUtility = TransferUtility.builder()
                .s3Client(s3Client)
                .context(this)
                .build();

        // 이미지 파일로 변환
        String filePath = getRealPathFromURI(imageUri);
        File file = new File(filePath);

        // S3 버킷 및 파일 경로 설정
        String bucketName = "s3-driver-upload/upload";
        String fileName = file.getName(); // 또는 원하는 파일 이름으로 변경

        // 업로드 작업 시작
        TransferObserver uploadObserver = transferUtility.upload(bucketName, fileName, file);

        // 업로드 상태 리스너 설정
        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    showToast("이미지 업로드 완료!");
                }
            }

            @Override
            public void onProgressChanged(int id, long current, long total) {
                // 업로드 진행 상황을 추적할 수 있습니다.
            }

            @Override
            public void onError(int id, Exception ex) {
                showToast("업로드 오류: " + ex.getMessage());
            }
        });
    }

    private String getRealPathFromURI(Uri uri) {
        String filePath = null;
        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(column_index);
            }
            cursor.close();
        }

        return filePath;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인된 경우 갤러리 열기
                openGallery();
            } else {
                showToast("갤러리 접근 권한이 거부되었습니다.");
            }
        }
    }
}
