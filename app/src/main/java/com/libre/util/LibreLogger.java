package com.libre.util;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.libre.R;

/**
 * Created by karunakaran on 7/13/2015.
 */
public class LibreLogger {


    public static void d(Object _object, String sb) {

         String TAG=_object.getClass().getName();

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
            Log.e(_object.getClass().getName(), sb.toString());
        }


    }

   /* public static void displayMessageAlert(String Message, Context c)
    {
        new AlertDialog.Builder(c).setMessage(Message).setPositiveButton(c.getResources().getString(R.string.OK), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface arg0, int arg1)
            {

            }
        }).show();
    }*/
}