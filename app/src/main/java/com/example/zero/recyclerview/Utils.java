package com.example.zero.recyclerview;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by Zero on 2016/7/25.
 */
public class Utils {

    @NonNull
    public static ArrayList<String> getDatas(String info, int startNum) {
        ArrayList<String> listData = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            listData.add("item " + info + (startNum + 1 + i));
        }
        return listData;
    }
}
