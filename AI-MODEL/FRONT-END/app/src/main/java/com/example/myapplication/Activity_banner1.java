package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;


public class Activity_banner1 extends AppCompatActivity {

    private ViewPager viewPager;
    Button button_und;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner);

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        button_und = (Button)findViewById(R.id.button_und);

        button_und.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Activity_banner1.this, home.class);
                startActivity(intent);
            }
        });


        // 이미지 리소스 배열 설정 (여기에서는 이미지 리소스 ID를 사용)
        int[] imageResources = {R.drawable.img1, R.drawable.img2, R.drawable.img3};

        // PagerAdapter를 생성하고 ViewPager에 설정
        PagerAdapter pagerAdapter = new Activity_banner3(this, imageResources);
        viewPager.setAdapter(pagerAdapter);

        // 자동 슬라이딩을 위한 타이머 시작
        Activity_banner2 activitybanner2 = new Activity_banner2(viewPager);
        activitybanner2.startAutoSlide();



    }



}