package com.libre;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.constants.LSSDPCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

public class BassEffectActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner{

    SeekBar mHpgain ,mBassGain,mDrive,mKnee;
    public Switch mByPass ;
    TextView tvHpGain, tvBassGain,tvDrive,tvKnee;
    final int HPGain = 250;
    final int BassGain = 251;
    final int DriveGain = 252;
    final int KneeGain = 253;
    final int Bypass = 254;


    String DeviceIP = "";
    String mStrHpGain = "HP Gain";
    String mStrBassGain = "Bass Gain";
    String mStrDrive = "Drive Gain";
    String mStrKnee = "Knee Gain";

    @Override
    protected void onResume() {
        sendLuci();
        super.onResume();
    }

    public void sendLuci(){
        LUCIControl mluci = new LUCIControl(DeviceIP);
        mluci.SendCommand(HPGain,"", LSSDPCONST.LUCI_GET);
        mluci.SendCommand(BassGain,"", LSSDPCONST.LUCI_GET);
        mluci.SendCommand(DriveGain,"", LSSDPCONST.LUCI_GET);
        mluci.SendCommand(KneeGain,"", LSSDPCONST.LUCI_GET);
        mluci.SendCommand(Bypass,"", LSSDPCONST.LUCI_GET);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bass_effect_layout);

        registerForDeviceEvents(this);

        Intent myIntent = getIntent();
        DeviceIP = myIntent.getStringExtra("DeviceIP");


        mHpgain = (SeekBar)findViewById(R.id.seekHpGain);
        mBassGain = (SeekBar)findViewById(R.id.seekbassGain);
        mDrive = (SeekBar) findViewById(R.id.seekDrive);
        mKnee = (SeekBar) findViewById(R.id.seekKnee);

        tvHpGain = (TextView) findViewById(R.id.tvResultHpGain);
        tvBassGain = (TextView) findViewById(R.id.tvResultBassGain);
        tvDrive = (TextView) findViewById(R.id.tvResultDrive);
        tvKnee = (TextView) findViewById(R.id.tvResultKnee);

        final LUCIControl mluci = new LUCIControl(DeviceIP);
        /* HP Gain SeekBar */
        mHpgain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LibreLogger.d(this,"HpGain "+ seekBar.getProgress());
                if(seekBar.getProgress()>0) {

                    float mNumberToSend = (float)seekBar.getProgress() / 100;
                    LibreLogger.d(this,"Hp Gain "+ mNumberToSend);
                    tvHpGain.setText(mStrHpGain + ": " + mNumberToSend);
                    mluci.SendCommand(HPGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }else{
                    tvHpGain.setText(mStrHpGain + ": " + (seekBar.getProgress()));
                    float mNumberToSend = seekBar.getProgress();
                    mluci.SendCommand(HPGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }
            }
        });

        /* Bass Gain SeekBar */
        mBassGain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LibreLogger.d(this,"Bass Gain "+ seekBar.getProgress());
                if(seekBar.getProgress()>0) {

                    float mNumberToSend = (float)seekBar.getProgress() / 100;
                    LibreLogger.d(this,"Bass Gain "+ mNumberToSend);
                    tvBassGain.setText(mStrBassGain + ": " + mNumberToSend);
                    mluci.SendCommand(BassGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }else{
                    tvBassGain.setText(mStrBassGain + ": " + (seekBar.getProgress()));
                    float mNumberToSend = seekBar.getProgress();
                    mluci.SendCommand(BassGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }

            }
        });

        /* Drive Gain SeekBar */
        mDrive.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LibreLogger.d(this,"Drive Gain "+ seekBar.getProgress());
                if(seekBar.getProgress()>0) {


                    float mNumberToSend = (float)seekBar.getProgress() / 100;
                    LibreLogger.d(this,"Drive Gain "+ mNumberToSend);
                    tvDrive.setText(mStrDrive + ": " + mNumberToSend);
                    mluci.SendCommand(DriveGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }else{
                    tvDrive.setText(mStrDrive + ": " + (seekBar.getProgress()));
                    float mNumberToSend = seekBar.getProgress();
                    mluci.SendCommand(DriveGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }

            }
        });

        mKnee.setProgress(1);
        /* Knee Gain SeekBar */
        mKnee.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LibreLogger.d(this,"Knee Gain "+ seekBar.getProgress());
                if(seekBar.getProgress()>0) {

                    float mNumberToSend =(float) seekBar.getProgress() / 100;
                    LibreLogger.d(this,"Knee Gain "+ mNumberToSend);
                    tvKnee.setText(mStrKnee + ": " + mNumberToSend);
                    mluci.SendCommand(KneeGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }else{
                    tvKnee.setText(mStrKnee + ": " + (seekBar.getProgress()));
                    float mNumberToSend = seekBar.getProgress();
                    mluci.SendCommand(KneeGain, String.valueOf(mNumberToSend), LSSDPCONST.LUCI_SET);
                }
            }
        });

        mByPass = (Switch) findViewById(R.id.byPassSwitc);
        //set the switch to ON
        mByPass.setChecked(false);
        //attach a listener to check for changes in state
        mByPass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                    mluci.SendCommand(Bypass, String.valueOf(1), LSSDPCONST.LUCI_SET);
                  //  switchStatus.setText("Switch is currently ON");
                }else{
                    mluci.SendCommand(Bypass, String.valueOf(0), LSSDPCONST.LUCI_SET);
                    //switchStatus.setText("Switch is currently OFF");
                }

            }
        });



       /* mByPass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Toast.makeText(BassEffectActivity.this, "Boolean Value" + b, Toast.LENGTH_SHORT).show();
            }
        });*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bass_effect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
    public void messageRecieved(NettyData dataRecived) {
        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        LibreLogger.d(this,"Command REceived From the Command "+ packet.getCommand());
        switch(packet.getCommand()) {
            case HPGain:

                try{
                    String message = new String(packet.getpayload());
                    float duration = Float.parseFloat(message);
                    int progressToSet = (int)(duration);
                    float mNumberToSend =(float) progressToSet / 100;
                    tvHpGain.setText(mStrHpGain + ": " + mNumberToSend);
                    mHpgain.setProgress(progressToSet);
                }catch(Exception e){

                }    break;
            case BassGain:

                try{
                    String message = new String(packet.getpayload());
                    float duration = Float.parseFloat(message);
                    int progressToSet = (int)(duration);
                    float mNumberToSend =(float) progressToSet / 100;
                    tvBassGain.setText(mStrBassGain + ": " + mNumberToSend);
                    mBassGain.setProgress(progressToSet);
                }catch(Exception e){

                }
                break;
            case KneeGain:
                try{
                    String message = new String(packet.getpayload());
                    float duration = Float.parseFloat(message);
                    int progressToSet = (int)(duration );
                    float mNumberToSend =(float) progressToSet / 100;
                    tvKnee.setText(mStrKnee + ": " + mNumberToSend);
                    mKnee.setProgress(progressToSet);
                }catch(Exception e){

                }
                break;
            case DriveGain:

                try{
                    String message = new String(packet.getpayload());
                    float duration = Float.parseFloat(message);
                    int progressToSet = (int)(duration);
                    float mNumberToSend =(float) progressToSet / 100;
                    tvDrive.setText(mStrDrive + ": " + mNumberToSend);
                    mDrive.setProgress(progressToSet);
                }catch(Exception e){

                }
                break;
            case Bypass:

                try{
                    String message = new String(packet.getpayload());
                    int value = Integer.valueOf(message);
                    if(value != 0){
                        mByPass.setChecked(true);
                    }else{
                        mByPass.setChecked(false);
                    }
                }catch(Exception e){

                }
                break;
        }


    }
}
