package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import java.util.concurrent.atomic.AtomicInteger;

public class Uploadpage extends Activity {

    Button button2;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private Button getImages;
    private Uri[] selectedImageUris = new Uri[2]; // 배열로 변경


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploadpage);

        getImages = findViewById(R.id.getImages);
        button2 = findViewById(R.id.button2);

        getImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkPermissionAndOpenGallery();
            }
        });



        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // button2를 눌렀을 때 S3에 이미지 업로드
                if (selectedImageUris[0] != null && selectedImageUris[1] != null) {
                    // 두 개의 이미지를 업로드
                    Uri[] selectedImages = new Uri[] { selectedImageUris[0], selectedImageUris[1] };
                    uploadImagesToS3(selectedImages);

                } else {
                    showToast("이미지를 선택하세요.");
                }
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 다중 선택을 허용
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                // 선택한 이미지 수 확인
                int imageCount = data.getClipData() != null ? data.getClipData().getItemCount() : 0;
                if (imageCount == 2) {
                    // 이미지 URI를 배열에 저장
                    selectedImageUris[0] = data.getClipData().getItemAt(0).getUri();

                    if (selectedImageUris[0] != null) {
                        selectedImageUris[1] = data.getClipData().getItemAt(1).getUri();
                        showToast("이미지 2개가 선택되었습니다.");

                    } else {
                        showToast("첫 번째 이미지 선택 오류가 발생했습니다.");
                    }

                } else {
                    showToast("2개의 이미지를 선택하세요.");
                }
            } else {
                showToast("이미지 선택 오류가 발생했습니다.");
            }
        }
    }

    private void uploadImagesToS3(Uri[] imageUris) {
        TransferNetworkLossHandler.getInstance(this);
        // 이미지 업로드 코드 추가
        // AWS 자격 증명 설정
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIASN5TEJIHGAWDYCGM", "6S35y1m1SwbxXwqY3d0TlCLRRe+ar8848bhyhedH");
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials, Region.getRegion(Regions.AP_NORTHEAST_2));

        // TransferUtility 설정
        TransferUtility transferUtility = TransferUtility.builder()
                .s3Client(s3Client)
                .context(this)
                .build();

        int totalImages = imageUris.length;

        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();
        AtomicInteger uploadedCount = new AtomicInteger(0);

        for (int i = 0; i < imageUris.length; i++) {
            Uri imageUri = imageUris[i];

            // 이미지 파일로 변환
            String filePath = getRealPathFromURI(imageUri);
            File file = new File(filePath);

            // S3 버킷 및 파일 경로 설정
            String bucketName = "s3-driver-upload";

            // 파일 이름을 적절하게 변경
            String fileName = generateUniqueFileName();

            final int imageIndex = i; // 이미지의 인덱스를 최종 변수로 설정


            // CompletableFuture를 사용하여 업로드 작업 병렬 실행
            CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
                TransferObserver uploadObserver = transferUtility.upload(bucketName, fileName, file);


                uploadObserver.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state == TransferState.COMPLETED) {
                            int index = imageIndex; // 최종 변수 사용
                            showToast("이미지 " + (index + 1) + " 업로드 완료!");
                            uploadedCount.incrementAndGet(); // 업로드 완료 횟수 증가

                            if (uploadedCount.get() == totalImages) {
                                showToast("모든 이미지 업로드 완료!");

                                Intent intent = new Intent(Uploadpage.this, loadingpage.class);
                                startActivity(intent);
                            }
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
            });

            uploadFutures.add(uploadFuture);
        }


        // 모든 CompletableFuture가 완료될 때까지 대기
        CompletableFuture<Void> allOf = CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0]));

        allOf.thenRun(() -> {
            showToast("모든 이미지 업로드 완료!");


        });

    }


    // 이 함수로 각 이미지에 대한 고유한 파일 이름을 생성합니다.
    private int imageIndex = 0; // 이미지 인덱스 초기화

    // 파일 이름 생성 메서드
    private String generateUniqueFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timeStamp = dateFormat.format(new Date());
        imageIndex++; // 이미지 인덱스 증가
        return "image_" + timeStamp + "_" + imageIndex + ".jpg"; // 이미지 인덱스를 파일 이름에 추가
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