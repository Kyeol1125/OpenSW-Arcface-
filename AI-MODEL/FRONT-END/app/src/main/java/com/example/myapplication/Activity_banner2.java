package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;

import androidx.viewpager.widget.ViewPager;

public class Activity_banner2 {
    private final ViewPager viewPager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentPage = 0;
    private final int delay = 5000; // 이미지 전환 간격 (5초)

    public Activity_banner2(ViewPager viewPager) {
        this.viewPager = viewPager;
    }

    public void startAutoSlide() {
        handler.postDelayed(slideRunnable, delay);
    }

    private final Runnable slideRunnable = new Runnable() {
        public void run() {
            if (currentPage >= viewPager.getAdapter().getCount()) {
                currentPage = 0;
            }
            viewPager.setCurrentItem(currentPage, true);
            currentPage++;
            handler.postDelayed(slideRunnable, delay);
        }
    };
}