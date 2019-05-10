package com.libre.led;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class LedActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    private static final String TAG = "MainActivity";
    View viewHue;
    AmbilWarnaKotak viewSatVal;
    ImageView viewCursor;
    //  TextView viewOldColor;
    View viewNewColor;
    int hsvcolor;

    ImageView viewTarget;
    ViewGroup viewContainer;
    RelativeLayout relativeLayout;

    LUCIControl luciControl;
    public Socket newTcpSocketForHostMsging = null;
    float[] currentColorHsv = new float[3];
    String SERVER_IP;
    String NAME;
    SwitchCompat device;
    Switch mGestureSwitch;
    SwitchCompat flashing;
    TextView flashingButton;
    SeekBar seekBar, ambarbar, whitebar;
    String DEVICE_ON = "LED_DEVICE:01";
    String DEVICE_OFF = "LED_DEVICE:00";
    String FLASING_ON = "LED_FLASHING:ON";
    String FLASHING_OFF = "LED_FLASHING:OFF";

    String RGB = "LED_RGB:";
    String BRIGHTNESSCONSTANT = "555555D5040302AA";
    String FLASHINGCONSTANT = "555555D5040303AA";
    String BRIGTHNESSSWITCHCONSTANT = "555555D5040101AA";
    TextView seekbartext, ambartext, whitetext;
    int MID = 207;
    int SET = 2;
    int GET = 1;
    boolean mbrunning=true;

    @Override
    protected void onStop() {

        try {
            mbrunning= false;
            newTcpSocketForHostMsging.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.led_layout);
        Log.d(TAG, "OnCreate");

        try {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

      /*  StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);*/
        SERVER_IP = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);
        NAME = getIntent().getStringExtra("DeviceName");
        luciControl = new LUCIControl(SERVER_IP);

        int color = Color.RED;

        Color.colorToHSV(color, currentColorHsv);


        viewHue = findViewById(R.id.ambilwarna_viewHue);
        device = (SwitchCompat) findViewById(R.id.device1);
//        flashing=(SwitchCompat)findViewById(R.id.switch2);
        flashingButton = (TextView) findViewById(R.id.switch_button);
        viewSatVal = (AmbilWarnaKotak) findViewById(R.id.ambilwarna_viewSatBri);
        viewCursor = (ImageView) findViewById(R.id.ambilwarna_cursor);

        // viewOldColor = (TextView)findViewById(R.id.ambilwarna_warnaLama);
        viewNewColor = findViewById(R.id.ambilwarna_warnaBaru);
        viewTarget = (ImageView) findViewById(R.id.ambilwarna_target);
        viewContainer = (ViewGroup) findViewById(R.id.ambilwarna_viewContainer);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativelayout);

        seekbartext = (TextView) findViewById(R.id.seekBartext);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax(255);

        mGestureSwitch = (Switch) findViewById(R.id.gestureLockSwitch);
        mGestureSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    byte[] setLockStatus = {0x02,0x02,0x21,0x00,0x01,0x01};
                    try {
                        sendGestureLockStatus(setLockStatus);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    byte[] setLockStatus = {0x02,0x02,0x21,0x00,0x01,0x00}; // OFF STATAUS
                    try {
                        sendGestureLockStatus(setLockStatus);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        ambartext = (TextView) findViewById(R.id.seekBarambartext);
        whitetext = (TextView) findViewById(R.id.seekBarwhitetext);
        ambarbar = (SeekBar) findViewById(R.id.ambar_control);
        whitebar = (SeekBar) findViewById(R.id.white_control);
        ambarbar.setMax(255);
        whitebar.setMax(255);
        whitebar.setProgress(125);
        viewSatVal.setHue(getHue());
        //viewOldColor.setText(gethexColor());
        //viewOldColor.setTextColor(getColor());

        viewNewColor.setBackgroundColor(color);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int m_progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_progress = progress;
                String hexformat = String.format("%02X", 0xFF & m_progress);
                Log.d(TAG, "SEEK VALUE" + hexformat);
                seekbartext.setText(hexformat + "");
                luciControl.SendCommand(MID, "LED_INTENSITY:" + hexformat, SET);


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        whitebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int m_progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                whitebar.setClickable(true);
                m_progress = progress;
                String hexformat = String.format("%02X", 0xFF & m_progress);
                Log.d(TAG, "SEEK VALUE" + hexformat);

                whitetext.setText(hexformat + "");

                luciControl.SendCommand(MID, "LED_WHITE:" + hexformat, SET);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });

        ambarbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int m_progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_progress = progress;
                String hexformat = String.format("%02X", 0xFF & m_progress);
                Log.d(TAG, "SEEK VALUE" + hexformat);

                ambartext.setText(hexformat + "");
                luciControl.SendCommand(MID, "LED_AMBER:" + hexformat, SET);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });

        viewHue.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                viewCursor.setVisibility(View.VISIBLE);
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP) {

                    float y = event.getY();
                    if (y < 0.f) y = 0.f;
                    if (y > viewHue.getMeasuredHeight())
                        y = viewHue.getMeasuredHeight() - 0.001f; // to avoid looping from end to start.
                    float hue = 360.f - 360.f / viewHue.getMeasuredHeight() * y;
                    if (hue == 360.f) hue = 0.f;
                    setHue(hue);

                    viewSatVal.setHue(getHue());
                 /*   viewOldColor.setText(gethexColor());
                    viewOldColor.setTextColor(getColor());*/
                    moveCursor();
                    viewNewColor.setBackgroundColor(getColor());

                    luciControl.SendCommand(MID, RGB + gethexColor(), SET);

                    return true;
                }
                return false;
            }
        });
        viewSatVal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP) {

                    float x = event.getX(); // touch event are in dp units.
                    float y = event.getY();

                    if (x < 0.f) x = 0.f;
                    if (x > viewSatVal.getMeasuredWidth()) x = viewSatVal.getMeasuredWidth();
                    if (y < 0.f) y = 0.f;
                    if (y > viewSatVal.getMeasuredHeight()) y = viewSatVal.getMeasuredHeight();

                    setSat(1.f / viewSatVal.getMeasuredWidth() * x);
                    setVal(1.f - (1.f / viewSatVal.getMeasuredHeight() * y));

                    // update view
                    moveTarget();
                   /* viewOldColor.setText(gethexColor());
                    viewOldColor.setTextColor(getColor());*/
                    viewNewColor.setBackgroundColor(getColor());
                    // if (event.getAction() == MotionEvent.ACTION_UP)
                    luciControl.SendCommand(MID, RGB + gethexColor(), SET);
                    Log.d(TAG,"HEX COLOR" + gethexColor()) ;
                    return true;
                }
                return false;
            }
        });


        device.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    luciControl.SendCommand(MID, DEVICE_ON, SET);
                    flashingButton.setClickable(true);
                    seekBar.setEnabled(true);
                    ambarbar.setEnabled(true);
                    whitebar.setEnabled(true);
                    viewSatVal.setEnabled(true);

                } else {
                    luciControl.SendCommand(MID, DEVICE_OFF, SET);
                    flashingButton.setClickable(false);
                    seekBar.setEnabled(false);
                    ambarbar.setEnabled(false);
                    whitebar.setEnabled(false);
                    viewSatVal.setEnabled(false);
                }


            }
        });
//        flashing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked)
//                    luciControl.SendCommand(MID,FLASING_ON,SET);
//
//
//                else
//                    luciControl.SendCommand(MID, FLASHING_OFF, SET);
//
//
//            }
//        });


        flashingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (flashingButton.getText().toString().equalsIgnoreCase("Flashing")) {
                    luciControl.SendCommand(MID, FLASING_ON, SET);
                    flashingButton.setText(getString(R.string.LEDSmoothness));
                } else {
                    luciControl.SendCommand(MID, FLASHING_OFF, SET);
                    flashingButton.setText(getString(R.string.LEDFlashing));
                }

            }
        });




      /*  ViewTreeObserver vto = this.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                moveCursor();
                moveTarget();
              getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });*/


        // initialColor is the initially-selected color to be shown
        // in the rectangle on the left of the arrow.
        // for example, 0xff000000 is black, 0xff0000ff is blue.
        // Please be aware of the initial 0xff which is the alpha.


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "OnResume");
        luciControl = new LUCIControl(SERVER_IP);

        new Thread(){
            public void run(){
                try {
                    newTcpSocketForHostMsging = new Socket(SERVER_IP,50005);
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getString(R.string.LEDTCPFail), Toast.LENGTH_SHORT).show();
                        }
                    });

                    newTcpSocketForHostMsging = null;
                }
                try {
                    byte[] getLockStatus = {0x02,0x01,0x20,0x00,0x00};
                    sendGestureLockStatus(getLockStatus);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
    public void sendGestureLockStatus(byte[] byteOfDataToSend) throws IOException {
        if(newTcpSocketForHostMsging!=null)
        {
            OutputStream outToServerStream = newTcpSocketForHostMsging.getOutputStream();
            outToServerStream.write(byteOfDataToSend, 0, byteOfDataToSend.length);

            ListeningToReceiveCommandFromSocket mReceivingThread=null;
            if(mReceivingThread==null)
                mReceivingThread = new ListeningToReceiveCommandFromSocket(newTcpSocketForHostMsging);
        }
    }
   /* private void sendGestureLockStatus(LUCIControl luciControl) throws IOException {

        byte[] getLockStatus = {0x02,0x01,0x20,0x00,0x00};
        byte[] setLockStatus = {0x02,0x02,0x21,0x00,0x01,0x01}; //byte[] getLockStatus = {0x02,0x01,0x20,0x00,0x00};
        if(newTcpSocketForHostMsging!=null)
        {
            OutputStream outToServerStream = newTcpSocketForHostMsging.getOutputStream();
            outToServerStream.write(getLockStatus, 0, getLockStatus.length);
            outToServerStream.write(setLockStatus, 0, setLockStatus.length);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outToServerStream.write(getLockStatus, 0, getLockStatus.length);

            ListeningToReceiveCommandFromSocket mReceivingThread=null;
            if(mReceivingThread==null)
                mReceivingThread = new ListeningToReceiveCommandFromSocket(newTcpSocketForHostMsging);
        }

    }*/
    class ListeningToReceiveCommandFromSocket extends Thread{
        Socket connectionSocket;
        ListeningToReceiveCommandFromSocket(Socket _connectionSocket){
            connectionSocket = _connectionSocket;
            this.start();
        }
        public  String byteArrayToHex(byte[] a) {
            StringBuilder sb = new StringBuilder(a.length * 2);
            for(byte b: a)
                sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        }
        public void run(){
            try{

                while(mbrunning) {
                    InputStream in = connectionSocket.getInputStream();
                    byte receivedByteArray[] = new byte[15];

                    int length = in.read(receivedByteArray);

                    if(receivedByteArray[1]==03){
                        if(receivedByteArray[11]==01){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mGestureSwitch.setChecked(true);
                                }
                            });

                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mGestureSwitch.setChecked(false);
                                }
                            });
                        }
                    }
                    System.out.println("Size of ReceivedByteArray : " + length);

                    System.out.println("Message Received:"+ byteArrayToHex(receivedByteArray) + "For Command:") ;
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
//        luciControl.close();
        Log.d(TAG, "OnPause");
    }

    protected void moveCursor() {
        float y = viewHue.getMeasuredHeight() - (getHue() * viewHue.getMeasuredHeight() / 360.f);
        if (y == viewHue.getMeasuredHeight()) y = 0.f;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewCursor.getLayoutParams();
        layoutParams.leftMargin = (int) (viewHue.getLeft() - Math.floor(viewCursor.getMeasuredWidth() / 2) - relativeLayout.getPaddingLeft());
        ;
        layoutParams.topMargin = (int) (viewHue.getTop() + y - Math.floor(viewCursor.getMeasuredHeight() / 2) - relativeLayout.getPaddingTop());
        ;
        viewCursor.setLayoutParams(layoutParams);
    }

    protected void moveTarget() {
        float x = getSat() * viewSatVal.getMeasuredWidth();
        float y = (1.f - getVal()) * viewSatVal.getMeasuredHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewTarget.getLayoutParams();
        layoutParams.leftMargin = (int) (viewSatVal.getLeft() + x - Math.floor(viewTarget.getMeasuredWidth() / 2) - relativeLayout.getPaddingLeft());
        layoutParams.topMargin = (int) (viewSatVal.getTop() + y - Math.floor(viewTarget.getMeasuredHeight() / 2) - relativeLayout.getPaddingTop());
        viewTarget.setLayoutParams(layoutParams);
    }

    private int getColor() {
        hsvcolor = Color.
                HSVToColor(currentColorHsv);
        return hsvcolor;

    }

    long fromByteArray(byte[] bytes) {
        return bytes[0] << 32 | bytes[1] << 24 | (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 8 | (bytes[4] & 0xFF);
    }
    private float getHue() {
        return currentColorHsv[0];
    }

    private float getSat() {
        return currentColorHsv[1];
    }

    private float getVal() {
        return currentColorHsv[2];
    }

    private void setHue(float hue) {
        currentColorHsv[0] = hue;
    }

    private void setSat(float sat) {
        currentColorHsv[1] = sat;
    }

    private void setVal(float val) {
        currentColorHsv[2] = val;
    }

    private String gethexColor() {
        Log.e(TAG,String.valueOf(hsvcolor));
        return String.format("%06X", 0xFFFFFF & hsvcolor);
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        Log.d(TAG, "Ondestroy");
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData packet) {
        Log.d(TAG,"Message Received "+ packet.getMessage());
    }
}
