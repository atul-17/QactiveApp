package com.libre.qactive.util;


import android.util.Log;

public class LibreLogger {
    public static void d(Object object, String sb) {
        String TAG = object.getClass().getSimpleName();
        /* Change done by Praveen for accoumdadting logs which are exceeding the length */
        if (sb.length() > 4000) {
            Log.d(TAG, "Logger Message.length = " + sb.length());
            int chunkCount = sb.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= sb.length()) {
                    Log.e(TAG, "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i));
                } else {
                    Log.e(TAG, "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max));
                }
            }
        } else {
            Log.e(TAG, sb);
        }
    }
}