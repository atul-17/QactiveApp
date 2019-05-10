package com.libre.alexa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.ImageView;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.MIDCONST;
import com.libre.luci.LUCIControl;
import com.libre.luci.Utils;

public class AudioRecordDemoActivity extends CTDeviceDiscoveryActivity implements AudioRecordCallback, MicExceptionListener {
    AppCompatTextView infoTv;
    ImageView micIv;
    boolean micOn = false;
    private static final int REQUEST_RECORD_AUDIO_AND_WRITE_PERMISSION = 200;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionGranted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO
            , Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private AudioRecordUtil audioRecordUtil;
    private MicTcpServer micTcpServer;
    private String ip;
    private LUCIControl luciControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_demo);

        ip = getIntent().getStringExtra("ip");
        luciControl = new LUCIControl(ip);

        infoTv = (AppCompatTextView) findViewById(R.id.info);
        micIv = (ImageView) findViewById(R.id.mic);
        micTcpServer = MicTcpServer.getMicTcpServer();

        final String phoneIp = new Utils().getIPAddress(true);

        micIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!micOn) {
                    micOn = true;
                    micIv.setImageResource(R.drawable.mic_on);
                    luciControl.SendCommand(MIDCONST.MID_MIC, Constants.START_MIC +phoneIp+","+MicTcpServer.MIC_TCP_SERVER_PORT, LSSDPCONST.LUCI_SET);
                    audioRecordUtil.startRecording(AudioRecordDemoActivity.this);
                    infoTv.setText("Recording started..");
                } else {
                    micOn = false;
                    micIv.setImageResource(R.drawable.mic_of);
//                    audioRecordUtil.stopRecording();
                    infoTv.setText("Recording stopped..");
                }
            }
        });

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_AND_WRITE_PERMISSION);

        audioRecordUtil = AudioRecordUtil.getAudioRecordUtil();
        LibreApplication libreApplication = (LibreApplication) getApplication();
        libreApplication.registerForMicException(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_AND_WRITE_PERMISSION:
                if (grantResults.length > 1) {
                    boolean audioRecordPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStoragePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    permissionGranted = audioRecordPermission && writeStoragePermission;
                }
                break;
        }
        if (!permissionGranted)
            infoTv.setText("Record audio and Storage permission not granted!!");

    }

    @Override
    public void recordError(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoTv.setText(error);
            }
        });
    }

    @Override
    public void recordStopped() {

    }

    @Override
    public void recordProgress(byte[] byteBuffer) {

    }

    @Override
    public void sendBufferAudio(byte[] audioBufferBytes) {
        micTcpServer.sendDataToClient(audioBufferBytes);
    }

    @Override
    public void micExceptionCaught(Exception e) {
        /*socket got closed from client side*/
        audioRecordUtil.stopRecording();
        if (e.getMessage().contains("Broken pipe")) {
            micTcpServer.createVerificationFile();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    infoTv.setText("Recording stopped...");
                    micOn = false;
                    micIv.setImageResource(R.drawable.mic_of);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new LibreApplication().unregisterMicException();
    }
}
