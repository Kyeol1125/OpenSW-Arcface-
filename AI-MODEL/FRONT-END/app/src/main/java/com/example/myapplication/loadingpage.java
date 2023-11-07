package com.example.myapplication;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import org.json.JSONException;
import org.json.JSONObject;

public class loadingpage extends AppCompatActivity {

    TextView resultTextView;
    private boolean initialDownloadCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        resultTextView = findViewById(R.id.resultTextView);

        // Handler를 사용하여 처음 다운로드를 1분 후에 실행
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 처음 다운로드 작업 실행
                new NetworkTask().execute();
            }
        }, 2 * 60 * 1000);
    }

    class NetworkTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                // 네트워크 작업 수행
                AWSCredentials awsCredentials = new BasicAWSCredentials("AKIASN5TEJIHGAWDYCGM", "6S35y1m1SwbxXwqY3d0TlCLRRe+ar8848bhyhedH");
                AmazonS3Client s3Client = new AmazonS3Client(awsCredentials, Region.getRegion(Regions.AP_NORTHEAST_2));

                String bucketName = "s3-final-20231028";
                ObjectListing objectListing = s3Client.listObjects(bucketName);

                // Initialize variables to track the latest object
                S3ObjectSummary latestObject = null;
                long latestTimestamp = 0;

                // Iterate through the objects to find the latest one
                for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
                    if (summary.getLastModified().getTime() > latestTimestamp) {
                        latestObject = summary;
                        latestTimestamp = summary.getLastModified().getTime();
                    }
                }

                if (latestObject != null) {
                    String objectKey = latestObject.getKey();
                    String[] keyParts = objectKey.split("/");
                    String fileName = keyParts[keyParts.length - 1];

                    String localFilePath = getFilesDir() + File.separator + "downloaded_file.txt";
                    // 파일이 이미 존재하는지 확인
                    File localFile = new File(localFilePath);
                    if (!initialDownloadCompleted && !localFile.exists()) {
                        // 처음 다운로드를 시작
                        TransferUtility transferUtility = TransferUtility.builder()
                                .s3Client(s3Client)
                                .context(loadingpage.this)
                                .build();

                        transferUtility.download(bucketName, objectKey, new File(localFilePath));
                        initialDownloadCompleted = true;
                    }

                    // 다운로드된 파일 읽기
                    String fileContent = readTextFromFile(localFile);
                    return fileContent;

                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 예외 처리 코드
                // 예외 발생 시 로그에 기록하거나 다른 작업을 수행할 수 있습니다.
                return null; // 또는 예외 처리 후 리턴 값 설정
            }
        }

        @Override
        protected void onPostExecute(String fileContent) {
            if (fileContent != null) {
                // 파일 내용을 처리하거나 표시하는 등의 작업을 수행합니다.
                resultTextView.setText(fileContent.trim());

                Intent nextActivityIntent;

                if (fileContent.trim().equals("0")) {
                    nextActivityIntent = new Intent(loadingpage.this, ResultFalse.class);
                } else if (fileContent.trim().equals("1")) {
                    nextActivityIntent = new Intent(loadingpage.this, Result.class);
                } else {
                    Toast.makeText(loadingpage.this, "파일의 내용이 예상과 다릅니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                startActivity(nextActivityIntent);
                finish(); // 현재 화면을 닫음
            } else {
                // 파일을 다운로드하지 못한 경우 처리
                Toast.makeText(loadingpage.this, "파일을 다운로드하지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String readTextFromFile(File file) {
        StringBuilder text = new StringBuilder();

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));

            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text.toString();
    }
}