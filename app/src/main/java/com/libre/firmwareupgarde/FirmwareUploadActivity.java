package com.libre.firmwareupgarde;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.R;
import com.libre.util.LibreLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirmwareUploadActivity extends Activity {

    final int DEVICES_FOUND = 412;
    final int TIME_EXPIRED = 413;
    final int MSEARCH_REQUEST = 414;
    final int MEDIA_PROCESS_INIT = 415;
    final int MEDIA_PROCESS_DONE = 416;
    final int MEDIA_PROCESS_FAIL = 417;
    final int DEVICE_FLASH_INITIATED_AFTER_CONFIRMATION = 418;
    final int DEVICE_FLASH_STARTED = 419;
    final int DEVICE_FLASH_FAILED = 420;

    final int STARTING_BSL_INITIATED = 421;
    final int STARTING_BSL_FAILED = 422;


    public String device_ip;
    private HandlerThread mThread;
    public String device_name;


    private ServiceHandler mServiceHandler;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_layout);

        if (!checkIfInAssets("lsimage")) {
            Toast.makeText(this, "Image not present", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mThread = new HandlerThread("NEW", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            mThread.start();
            mServiceHandler = new ServiceHandler(mThread.getLooper());

            device_ip = getIntent().getStringExtra("DeviceIP");
            device_name = getIntent().getStringExtra("DeviceSSID");

            LibreLogger.d(this, "Device ip " + device_ip + " device name = " + device_name);
            handler.sendEmptyMessage(STARTING_BSL_INITIATED);

            TextView tView = (TextView) findViewById(R.id.device_name);
            tView.setText("Firmware Upload progress for " + device_name);
        }

    }


    /**
     * Checks if an asset exists.
     *
     * @param assetName
     * @return boolean - true if there is an asset with that name.
     */
    public boolean checkIfInAssets(String assetName) {
        AssetManager am;
        List<String> mapList = new ArrayList<>();
        am = getAssets();
        try {
            mapList = Arrays.asList(am.list(""));
        } catch (IOException e) {
        }

        return mapList.contains(assetName);
    }


    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);


            switch (msg.what) {

                case DEVICES_FOUND:

                {
                    Log.d("", "Devices found");
                     /*loading stopMediaServer with success message*/
                }
                break;

                case STARTING_BSL_INITIATED:
                    showLoader("Step 1/3: Rebooting in BSL mode");
                    Toast.makeText(FirmwareUploadActivity.this, "Rebooting in BSL mode", Toast.LENGTH_SHORT).show();
                    mServiceHandler.sendEmptyMessage(STARTING_BSL_INITIATED);

                    break;

                case STARTING_BSL_FAILED:
                    closeLoader();
                    Toast.makeText(FirmwareUploadActivity.this, "Failed to Start Device in BSL!Upload Failed ", Toast.LENGTH_SHORT).show();
                    break;

                case MEDIA_PROCESS_INIT:
                    closeLoader();
                    showLoader("Step 2/3: Uploading Firmware");
                    Toast.makeText(FirmwareUploadActivity.this, "Firmware uploading Started..", Toast.LENGTH_SHORT).show();
                    mServiceHandler.sendEmptyMessage(MEDIA_PROCESS_INIT);
                    break;

                case MEDIA_PROCESS_DONE:
                    closeLoader();
                    Toast.makeText(FirmwareUploadActivity.this, "Completed", Toast.LENGTH_SHORT).show();
                    showConfirmDialog(FirmwareUploadActivity.this);
                    break;

                case MEDIA_PROCESS_FAIL:
                    closeLoader();
                    Toast.makeText(FirmwareUploadActivity.this, "Firmware Upload Failed", Toast.LENGTH_SHORT).show();
                    break;


                case DEVICE_FLASH_STARTED:
                    closeLoader();
                    Toast.makeText(FirmwareUploadActivity.this, "Success! Please wait device is rebooting!", Toast.LENGTH_LONG).show();
                    finish();
                    break;

                case DEVICE_FLASH_FAILED:
                    closeLoader();
                    Toast.makeText(FirmwareUploadActivity.this, "Device flash failed", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

    class ServiceHandler extends Handler {

        URLclient uploadClient;

        public ServiceHandler(Looper looper) {
            super(looper);

        }

        @Override
        public void handleMessage(Message msg) {

            try {
                switch (msg.what) {

                    case STARTING_BSL_INITIATED: {
                        uploadClient = new URLclient(device_ip);
                        uploadClient.restartTheDUTInBSL(handler);
                        break;
                    }


                    case MEDIA_PROCESS_INIT: {
                        copyFilesToSdCard();
                        File file = new File(TARGET_BASE_PATH + "lsimage");
                        uploadClient = new URLclient(device_ip);
                        uploadClient.sendTheFileToDevice(file, handler);
                        break;
                    }


                    case DEVICE_FLASH_INITIATED_AFTER_CONFIRMATION:
                        uploadClient.startDeviceFlash(handler);
                        break;


                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.sendEmptyMessage(MEDIA_PROCESS_FAIL);
            }
        }
    }


    final static String TARGET_BASE_PATH = "/sdcard/Libre/";

    public void createAlert() {
        final EditText input = new EditText(this);
        input.setHint("enter the ip address of the device");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        new AlertDialog.Builder(this).setView(input)
                .setTitle("Enter device IP adddress")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete

                        device_ip = input.getText().toString().trim();
                        handler.sendEmptyMessage(MEDIA_PROCESS_INIT);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void copyFilesToSdCard() {

        copyFileOrDir(""); // copy all files in assets folder in my project

    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = this.getAssets();
        String assets[] = null;
        try {
            Log.i("tag", "copyFileOrDir() " + path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath = TARGET_BASE_PATH + path;
                Log.i("tag", "path=" + fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir " + fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                        copyFileOrDir(p + assets[i]);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String filename) {
        AssetManager assetManager = this.getAssets();

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() " + filename);
            in = assetManager.open(filename);
            if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = TARGET_BASE_PATH + filename.substring(0, filename.length() - 4);
            else
                newFileName = TARGET_BASE_PATH + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of " + newFileName);
            Log.e("tag", "Exception in copyFile() " + e.toString());
        }

    }


    public void showLoader(String title) {
        mProgressDialog = ProgressDialog.show(FirmwareUploadActivity.this, null, title);
        mProgressDialog.show();
    }

    public void closeLoader() {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.cancel();
    }


    void showConfirmDialog(Context context) {

        new AlertDialog.Builder(context)
                .setTitle("Firmware loaded to device")
                .setMessage("Are you sure you want to update the device with this Firmware?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete

                        showLoader("Requesting Firmware Flash ");
                        mServiceHandler.sendEmptyMessage(DEVICE_FLASH_INITIATED_AFTER_CONFIRMATION);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        Toast.makeText(FirmwareUploadActivity.this, "You  cancelled the upload", Toast.LENGTH_SHORT).show();

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

}

