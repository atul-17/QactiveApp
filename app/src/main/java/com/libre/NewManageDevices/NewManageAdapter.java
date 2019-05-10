/*
 * ********************************************************************************************
 *  *
 *  * Copyright (C) 2014 Libre Wireless Technology
 *  *
 *  * "Libre Sync" Project
 *  *
 *  * Libre Sync Android App
 *  * Author: Android App Team
 *  *
 *  **********************************************************************************************
 */

package com.libre.NewManageDevices;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.LibreApplication;
import com.libre.ManageDevice.ManageDeviceHandler;
import com.libre.Network.LSSDPDeviceNetworkSettings;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.constants.CommandType;
import com.libre.constants.DeviceMasterSlaveFreeConstants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.util.LibreLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by praveena on 10/19/15.
 */
public class NewManageAdapter extends BaseAdapter {

    /* Moving ConcurrentHAshmap to LinkedHashmap is For ,
    * Always Need master to be Display First , but in ConcurrentHashmap there is no order
    * */
    public LinkedHashMap<String, DeviceData> mMasterSpecificSlaveAndFreeDeviceMap = new LinkedHashMap<>();
    Context context;
    Handler mManageDevicesHandler;
    String mMasterIp;
    String mActivityName;
    HashMap<String, Boolean> checkIfProgressBarActiveHashMap = new HashMap<>();

    public final int STEREO = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    public ScanningHandler m_scanHandler = ScanningHandler.getInstance();
    public ManageDeviceHandler m_ManageDeviceHandler = ManageDeviceHandler.getInstance();
    public int mWifiMode = -1;

    public NewManageAdapter(Context context, int textViewResourceId,
                            Handler mHandler, String m_MasterIp
            , String m_ActivityName) {
        this.context = context;
        this.mManageDevicesHandler = mHandler;
        this.mMasterIp = m_MasterIp;
        this.mActivityName = m_ActivityName;
        mWifiMode = m_scanHandler.getconnectedSSIDname(context);
    }

    @Override
    public int getCount() {
        try {
            return mMasterSpecificSlaveAndFreeDeviceMap.keySet().toArray().length;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getCountSlaves(){

         return ScanningHandler.getInstance().getNumberOfSlavesForMasterIp(mMasterIp,
                0);

    }
    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public void add(DeviceData object) {
        if (object != null)
            mMasterSpecificSlaveAndFreeDeviceMap.put(object.getIpAddress(), object);
    }

    public void addAll(Collection<? extends DeviceData> collection) {
        Iterator<? extends DeviceData> iterator = collection.iterator();
        while (iterator.hasNext()) {
            DeviceData deviceData = iterator.next();
            mMasterSpecificSlaveAndFreeDeviceMap.put(deviceData.getIpAddress(), deviceData);
        }
    }


    public void clear() {
        mMasterSpecificSlaveAndFreeDeviceMap.clear();
    }

    public void remove(DeviceData data) {
        if (data != null)
            mMasterSpecificSlaveAndFreeDeviceMap.remove(data.getIpAddress());
    }


    public class Holder {
        LinearLayout mRelative;
        TextView DeviceName, NoOfDevices, DeviceStatus, mSpeakerType,mWifiBand;
        ImageButton mute;
        SeekBar volumebar;
        ImageButton mArrowForLSSDPSettings;
        ImageView mMute;
        ImageView device_state;
        ProgressBar proloader;
    }
    private void mShowAlertToastForFailingGrouping(){
        /* Commented For LS9 Change*//*
        new android.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.alertTitle))
                .setMessage(" " +
                        context.getString(R.string.alertMessageForCantGroup))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setIcon(android.R.drawable.ic_dialog_alert)
                .show();*/
    }
    private String getSlaveInfoAfterFreeingASlave(LSSDPNodes mNodeToBeFree) {
        StringBuilder stringTobeSentToDUT = new StringBuilder();
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        stringTobeSentToDUT.append("SLAVEINFO_");


        try {
            LSSDPNodes mMasterNode = m_scanHandler.getLSSDPNodeFromCentralDB(mMasterIp);
            for (LSSDPNodes mSlaveNode : mNodeDB.GetDB()) {

                LibreLogger.d(this, "Name Of the Speaker in the Loop :" + mSlaveNode.getFriendlyname() + "Device State " +
                        mSlaveNode.getDeviceState()
                        + " Zone ID " + mSlaveNode.getZoneID());

                if ((mMasterNode != null) && (mSlaveNode != null) &&
                        mSlaveNode.getcSSID().equals(mMasterNode.getcSSID()) &&
                        mSlaveNode.getDeviceState().equals("S")) {
                    LibreLogger.d(this, "HN Mode Slave Speakers Found For MasterIp :" + mSlaveNode.getFriendlyname() + "mMaster" +
                            mMasterNode.getFriendlyname());
                    if (!mSlaveNode.getIP().equals(mNodeToBeFree.getIP()))
                        stringTobeSentToDUT.append(mSlaveNode.getFriendlyname()).append("+");

                }
            }
        } catch (Exception e) {
            LibreLogger.d(this, e.getMessage() + "Exception happened while interating in getSlaveListForMasterIp");
        }
        /*to remove end + sign*/
        if (stringTobeSentToDUT.length() > 10)
            stringTobeSentToDUT.setLength(stringTobeSentToDUT.length() - 1);

        return stringTobeSentToDUT.toString();
    }

    /*this is for slave info to DUT in HN mode*/
    private String getSlavesInfo(LSSDPNodes mNodeOfDetailToSend) {
        StringBuilder stringTobeSentToDUT = new StringBuilder();
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        stringTobeSentToDUT.append("SLAVEINFO_");
        String mFriendlyNameToAdd = mNodeOfDetailToSend.getFriendlyname();
        try {
            LSSDPNodes mMasterNode = m_scanHandler.getLSSDPNodeFromCentralDB(mMasterIp);
            for (LSSDPNodes mSlaveNode : mNodeDB.GetDB()) {

                LibreLogger.d(this, "Name Of the Speaker in the Loop :" + mSlaveNode.getFriendlyname() + "Device State " +
                        mSlaveNode.getDeviceState()
                        + " Zone ID " + mSlaveNode.getZoneID());

                if ((mMasterNode != null) && (mSlaveNode != null) &&
                        mSlaveNode.getcSSID().equals(mMasterNode.getcSSID()) &&
                        mSlaveNode.getDeviceState().equals("S")) {
                    LibreLogger.d(this, "HN Mode Slave Speakers Found For MasterIp :" + mSlaveNode.getFriendlyname() + "mMaster" +
                            mMasterNode.getFriendlyname());
                    stringTobeSentToDUT.append(mSlaveNode.getFriendlyname()).append("+");

                }
            }
            stringTobeSentToDUT.append(mFriendlyNameToAdd);
        } catch (Exception e) {
            LibreLogger.d(this, e.getMessage() + "Exception happened while interating in getSlaveListForMasterIp");
        }
        /*to remove end + sign*/
        if (stringTobeSentToDUT.length() > 10)
            stringTobeSentToDUT.setLength(stringTobeSentToDUT.length() - 1);

        return stringTobeSentToDUT.toString();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;

        final String currentIpAddress = (String) mMasterSpecificSlaveAndFreeDeviceMap.keySet().toArray()[position];
        LibreLogger.d(this, "Whole Key" + mMasterSpecificSlaveAndFreeDeviceMap.keySet().toString());
        LibreLogger.d(this, "IP " + currentIpAddress);

        LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(currentIpAddress);

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) (context).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.new_manage_devices_list_item, null);
            holder.DeviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            holder.DeviceName.setSelected(true);//for marquee to work
            holder.DeviceStatus = (TextView) convertView.findViewById(R.id.tvMasterSlave);
            holder.volumebar = (SeekBar) convertView.findViewById(R.id.seekBar);
            holder.mMute = (ImageView) convertView.findViewById(R.id.mute_button);
            holder.mRelative = (LinearLayout) convertView.findViewById(R.id.managedeviceLayout);
            holder.mSpeakerType = (TextView) convertView.findViewById(R.id.tvSpeakerType);
            holder.mArrowForLSSDPSettings = (ImageButton) convertView.findViewById(R.id.imgView_arrow);
            holder.proloader = (ProgressBar) convertView.findViewById(R.id.progress_loader);
            holder.device_state = (ImageView) convertView.findViewById(R.id.state_result);
            holder.mWifiBand = (TextView)convertView.findViewById(R.id.tvWifiBand);
            convertView.setTag(holder);

        }
        holder = (Holder) convertView.getTag();

        if (node != null) {
            Log.e("ManageDevice", node.getDeviceState() + "::" + node.getFriendlyname());
            holder.DeviceName.setSelected(true);
            holder.mWifiBand.setSelected(true);
            holder.mWifiBand.setVisibility(View.GONE) ;     //setText(node.getmWifiBand());
            if (node.getDeviceState().equals("M")) {
                holder.DeviceStatus.setText(context.getResources().getString(R.string.master));
                holder.volumebar.setVisibility(View.VISIBLE);
                holder.mMute.setVisibility(View.VISIBLE);
                holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
            } else if (node.getDeviceState().equals("S")) {
                holder.DeviceStatus.setText(context.getResources().getString(R.string.slave));
                holder.volumebar.setVisibility(View.VISIBLE);
                holder.mMute.setVisibility(View.VISIBLE);
                holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
            } else if (node.getDeviceState().equals("F")) {
                holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item));
                holder.DeviceStatus.setText(context.getResources().getString(R.string.free));
                holder.volumebar.setVisibility(View.INVISIBLE);
                holder.mMute.setVisibility(View.INVISIBLE);
            }

            if (Integer.valueOf(node.getSpeakerType()) == STEREO) {
                holder.mSpeakerType.setText(context.getResources().getString(R.string.stereo));
            } else if (Integer.valueOf(node.getSpeakerType()) == LEFT) {
                holder.mSpeakerType.setText(context.getResources().getString(R.string.left));
            } else if (Integer.valueOf(node.getSpeakerType()) == RIGHT) {
                holder.mSpeakerType.setText(context.getResources().getString(R.string.right));
            }


            /* get the device data and set the device name and volume bar*/
            DeviceData deviceData = mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
            holder.DeviceName.setText(deviceData.getDeviceName());

            /*updating volume from hashmap which is receiving volume in background*/
            if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(currentIpAddress)) {
                holder.volumebar.setProgress(LibreApplication.INDIVIDUAL_VOLUME_MAP.get(currentIpAddress));
                Log.d("VolumeMapNotifying", "" + LibreApplication.INDIVIDUAL_VOLUME_MAP.get(currentIpAddress));
            } else {
                /*which means we don't have volume so sending volume request*/
                LUCIControl muteLuciControl = new LUCIControl(currentIpAddress);
                muteLuciControl.SendCommand(MIDCONST.VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET);
                holder.volumebar.setProgress(deviceData.getVolumeValueInPercentage());
            }


            holder.proloader.getIndeterminateDrawable().setColorFilter(0xFFE15D29, android.graphics.PorterDuff.Mode.MULTIPLY);

            handleDeviceStateChnageStatus(holder, node);
        }

            /* Initialize the click listners */
//        final String currentIpAddress = holder.ipaddress;

        holder.mArrowForLSSDPSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ssid = new Intent(context, LSSDPDeviceNetworkSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                final LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(currentIpAddress);
                if (node != null) {
                    ssid.putExtra("DeviceName", node.getFriendlyname());
                    ssid.putExtra(Constants.CURRENT_DEVICE_IP, node.getIP());
                    ssid.putExtra("activityName", "NewManageDevices.java");
                    context.startActivity(ssid);
                }
            }
        });


        holder.mRelative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final LSSDPNodes clickedNode = m_scanHandler.getLSSDPNodeFromCentralDB(currentIpAddress);
                final LSSDPNodes mMasterNode = m_scanHandler.getLSSDPNodeFromCentralDB(mMasterIp);
                if (clickedNode == null || mMasterNode == null)
                    return;

                /*to prevent user's multiple action on device once change has been initiated*/
                if (clickedNode.getCurrentState() == LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED)
                    return;

                switch (clickedNode.getDeviceState()) {
                    case "F":
                        clickingOnFreeDeviceOld(clickedNode, mMasterNode);
//                        clickingOnFreeDevice(clickedNode, mMasterNode);
                        break;
//                    case "M":
//                        clickingOnMasterDevice(clickedNode);
//                        break;
//                    case "S" :
//                        clickingOnSlaveDevice(clickedNode);
//                        break;
                    default:
                        clickingOnSlaveOrMasterOld(clickedNode, mMasterNode);
                        break;
                }
            }
        });

        holder.mMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LUCIControl muteLuciControl = new LUCIControl(currentIpAddress);
                muteLuciControl.sendAsynchronousCommand();
                muteLuciControl.SendCommand(MIDCONST.MID_MUTE, LUCIMESSAGES.MUTE, LSSDPCONST.LUCI_SET);
            }
        });

        holder.volumebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("onProgresChanged" + progress, "" + position);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("onStartTracking", "" + position);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                LibreLogger.d("onStopTracking", "" + position);

                DeviceData data = mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
                LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.getProgress(), CommandType.SET, currentIpAddress);

                /* This is to make sure that the central repository is also getting for the master device */
                SceneObject sceneObjectFromCentralRepo = m_scanHandler.getSceneObjectFromCentralRepo(currentIpAddress);
                if (sceneObjectFromCentralRepo != null) {
                    sceneObjectFromCentralRepo.setVolumeValueInPercentage(seekBar.getProgress());
                    m_scanHandler.putSceneObjectToCentralRepo(currentIpAddress, sceneObjectFromCentralRepo);
                }


                if (data != null) {
                    data.setVolumeValueInPercentage(seekBar.getProgress());
                    mMasterSpecificSlaveAndFreeDeviceMap.put(currentIpAddress, data);
                }

            }
        });

        if(node!=null &&node.getCurrentSource()== Constants.GCAST_SOURCE
                && node.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
            disableForGcast(holder,node);
        }

        return convertView;
    }

    private void disableForGcast(Holder holder,LSSDPNodes node) {
            holder.volumebar.setClickable(false);
            holder.mRelative.setClickable(false);
            holder.mMute.setClickable(false);
            holder.volumebar.setEnabled(false);
            holder.mArrowForLSSDPSettings.setClickable(false);

        if (node.getDeviceState().equals("M")) {

            holder.DeviceStatus.setText(context.getResources().getString(R.string.master));
            holder.volumebar.setVisibility(View.VISIBLE);
            holder.volumebar.setProgress(0);
            holder.mMute.setVisibility(View.VISIBLE);
            holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_cast_playing));
        } else if (node.getDeviceState().equals("S")) {
            holder.DeviceStatus.setText(context.getResources().getString(R.string.slave));
            holder.volumebar.setVisibility(View.VISIBLE);
            holder.volumebar.setProgress(0);
            holder.mMute.setVisibility(View.VISIBLE);
            holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_cast_playing));
        } else if (node.getDeviceState().equals("F")) {
            holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_cast_playing));
            holder.DeviceStatus.setText(context.getResources().getString(R.string.free));
            holder.volumebar.setVisibility(View.INVISIBLE);
            holder.mMute.setVisibility(View.INVISIBLE);
        }
    }

    private void handleDeviceStateChnageStatus(Holder holder, LSSDPNodes node) {

        holder.device_state.setVisibility(View.INVISIBLE);
        holder.proloader.setVisibility(View.INVISIBLE);


        if (node.getCurrentState() == LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_SUCCESS) {
            checkIfProgressBarActiveHashMap.remove(node.getIP());
//            holder.device_state.setImageResource(R.drawable.device_state_success);
//            holder.device_state.setVisibility(View.VISIBLE);
        } else if (node.getCurrentState() == LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_FAIL) {
            checkIfProgressBarActiveHashMap.remove(node.getIP());
            holder.device_state.setImageResource(R.drawable.device_state_failed);
            holder.device_state.setVisibility(View.VISIBLE);

        } else if (node.getCurrentState() == LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED) {
            holder.proloader.setVisibility(View.VISIBLE);
            checkIfProgressBarActiveHashMap.put(node.getIP(), false);
        }


    }

    public Boolean isAnyDeviceInChangingState() {

        if (checkIfProgressBarActiveHashMap != null && checkIfProgressBarActiveHashMap.size() > 0) {

            return true;
        }
        return false;
    }

    public void clickingOnSlaveDevice(LSSDPNodes mNode) {

        final LUCIControl mLuciCommandToSend = new LUCIControl(mNode.getIP());
        final Dialog dialog = new Dialog(context);

        //setting custom layout to dialog
        dialog.setContentView(R.layout.slave_ddms_change_dialog);
        dialog.setTitle("ReGrouping");

        //adding text dynamically
        TextView txt = (TextView) dialog.findViewById(R.id.tvTitleQues);
        txt.setText("To Regroup this Speaker Select Any of these Options ? ");

        //adding button click event
        Button btnJoinAll = (Button) dialog.findViewById(R.id.btnJoinAll);
        btnJoinAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });
        Button btnJoinNext = (Button) dialog.findViewById(R.id.btnSetSlave);
        btnJoinNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });

        Button btnDropMe = (Button) dialog.findViewById(R.id.btnDropMe);
        btnDropMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.DROP, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });

        Button btnDropAll = (Button) dialog.findViewById(R.id.btnDropAll);
        btnDropAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP,LUCIMESSAGES.DROP_ALL,LSSDPCONST.LUCI_SET);
                dropAllDevices();
                dialog.dismiss();
            }
        });

        dialog.show();
       /* final CharSequence[] items = {"Join All", "Join Next", "Drop Me", "Drop All"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("DDMS STATE");
        builder.setMessage("Are you Sure want to change the DDDMS State????");
        builder.setSingleChoiceItems(items, 1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
                Toast.makeText(context, items[item], Toast.LENGTH_SHORT).show();
            }

        });

        builder.create().show();*/
    }
        /*This function gets Called When Drop All Pressed*/

    private void dropAllDevices() {
        LibreLogger.d(this, "drop all");
        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectFromCentralRepo();

            /*which means no master present hence all devices are free so need to do anything*/
        if (centralSceneObjectRepo == null || centralSceneObjectRepo.size() == 0) {
            LibreLogger.d(this, "No master present");
            return;
        }

        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        LSSDPNodes mNodeToSendDropAll = mNodeDB.GetDB().get(0);

        LUCIControl mLuciControlToSendDropAll = new LUCIControl(mNodeToSendDropAll.getIP());
        mLuciControlToSendDropAll.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.DROP_ALL, LSSDPCONST.LUCI_SET);

        /*clearing central repository*/
        mScanningHandler.clearSceneObjectsFromCentralRepo();

    }

    public void clickingOnMasterDevice(LSSDPNodes mNode) {

        final LUCIControl mLuciCommandToSend = new LUCIControl(mNode.getIP());
        final Dialog dialog = new Dialog(context);

        //setting custom layout to dialog
        dialog.setContentView(R.layout.master_ddms_change_dialog);
        dialog.setTitle("ReGrouping");

        //adding text dynamically
        TextView txt = (TextView) dialog.findViewById(R.id.tvTitleQues);
        txt.setText(" To Regroup this Speaker Select Any of these Options ? ");

        //adding button click event
        Button btnJoinAll = (Button) dialog.findViewById(R.id.btnJoinAll);
        btnJoinAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });

        Button btnDropMe = (Button) dialog.findViewById(R.id.btnDropMe);
        btnDropMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.DROP, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });

        Button btnDropAll = (Button) dialog.findViewById(R.id.btnDropAll);
        btnDropAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP,LUCIMESSAGES.DROP_ALL,LSSDPCONST.LUCI_SET);
                dropAllDevices();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void clickingOnFreeDevice(final LSSDPNodes mNode, final LSSDPNodes mMasterNode) {
        final LUCIControl mLuciCommandToSend = new LUCIControl(mNode.getIP());
        final Dialog dialog = new Dialog(context);

        //setting custom layout to dialog
        dialog.setContentView(R.layout.free_ddms_change_dialog);
        dialog.setTitle("Grouping");

        //adding text dynamically
        TextView txt = (TextView) dialog.findViewById(R.id.tvTitleQues);
        txt.setText("To Group this Speaker Select Any of these Options ? ");

        //adding button click event
        Button btnJoinAll = (Button) dialog.findViewById(R.id.btnJoinAll);
        btnJoinAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });
        Button btnJoinNext = (Button) dialog.findViewById(R.id.btnSetSlave);
        btnJoinNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandleclickingOnFreeDevice(mNode, mMasterNode);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    public void clickingOnFreeDeviceOld(LSSDPNodes clickedNode, LSSDPNodes mMasterNode) {
        if(clickedNode.getmWifiBand()==null
                || mMasterNode.getmWifiBand() == null){
            mShowAlertToastForFailingGrouping();
            return;
        }
        if(clickedNode.getmWifiBand()!=null &&
                mMasterNode.getmWifiBand()!=null) {
            if (!mMasterNode.getmWifiBand().equalsIgnoreCase(clickedNode.getmWifiBand())){
                mShowAlertToastForFailingGrouping();
                return;
            }
        }
        //User is trying to Make this  Slave
        Message msg = new Message();
        msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_CREATE_SLAVE_INITIATED;
        /* Adding the Bundle data to make sure to identify the device in NewManageDevices screen*/
        Bundle data = new Bundle();
        data.putString("ipAddress", clickedNode.getIP());
        msg.setData(data);
        mManageDevicesHandler.sendMessage(msg);


        if (mMasterNode != null) {
            String mCSSID = mMasterNode.getcSSID();
            String mZoneID = mMasterNode.getZoneID();
                         /* Sending Luci Command For Slave*/
            LUCIControl luciControl = new LUCIControl(clickedNode.getIP());
                         /* Sending Asynchronous Registration*/
            luciControl.sendAsynchronousCommand();

            /*To Check Whether A Master is Avaliable or not , So we ae trying to Read Play State */
            LUCIControl luciControlForMaster = new LUCIControl(mMasterNode.getIP());
            ArrayList<LUCIPacket> luciPackets = new ArrayList<LUCIPacket>();
            LUCIPacket packet7 = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_CURRENT_PLAY_STATE, (byte) LSSDPCONST.LUCI_GET);
            luciPackets.add(packet7);
            luciControlForMaster.SendCommand(luciPackets);

            ArrayList<LUCIPacket> setOfCommandsWhileMakingSlave = new ArrayList<LUCIPacket>();
            if (mCSSID != null) {
                LUCIPacket concurrentSsid105Packet = new LUCIPacket(mCSSID.getBytes(), (short) mCSSID.length(), (short) MIDCONST.MID_SSID, (byte) LSSDPCONST.LUCI_SET);
                setOfCommandsWhileMakingSlave.add(concurrentSsid105Packet);
            }
            LUCIPacket zoneid104Packet = new LUCIPacket(mZoneID.getBytes(), (short) mZoneID.length(), (short) MIDCONST.MID_DDMS_ZONE_ID, (byte) LSSDPCONST.LUCI_SET);
            LibreLogger.d(this, "Slave ZoneID" + mZoneID);
            setOfCommandsWhileMakingSlave.add(zoneid104Packet);

            LUCIPacket setSlave100Packet = new LUCIPacket(LUCIMESSAGES.SETSLAVE.getBytes(), (short) LUCIMESSAGES.SETSLAVE.length(), (short) MIDCONST.MID_DDMS, (byte) LSSDPCONST.LUCI_SET);
            setOfCommandsWhileMakingSlave.add(setSlave100Packet);


            luciControl.SendCommand(setOfCommandsWhileMakingSlave);


            /*setting flag to LSSDP Node*/
            clickedNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED);

            LibreLogger.d(this, "We are trying to make " + clickedNode.getIP() + "as Slave for master device=" + mMasterNode.getIP() + " Where we have  master cssid= " + mCSSID + " zoneid=" + mZoneID);
            LibreLogger.d(this, "Second_We are trying to make " + clickedNode.getFriendlyname() + "as Slave for master device=" + mMasterNode.getFriendlyname() + " Where we have  master cssid= " + mCSSID + " zoneid=" + mZoneID);


        }

    }


    public DeviceData getSceneObjectFromAdapter(String ip) {
        return mMasterSpecificSlaveAndFreeDeviceMap.get(ip);
    }


    public void mHandleclickingOnFreeDevice(LSSDPNodes clickedNode, LSSDPNodes mMasterNode) {

        //User is trying to Make this  Slave
        Message msg = new Message();
        msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_CREATE_SLAVE_INITIATED;
        /* Adding the Bundle data to make sure to identify the device in NewManageDevices screen*/
        Bundle data = new Bundle();
        data.putString("ipAddress", clickedNode.getIP());
        msg.setData(data);
        mManageDevicesHandler.sendMessage(msg);


        if (mMasterNode != null) {
            String mCSSID = mMasterNode.getcSSID();
            String mZoneID = mMasterNode.getZoneID();
                         /* Sending Luci Command For Slave*/
            LUCIControl luciControl = new LUCIControl(clickedNode.getIP());
                         /* Sending Asynchronous Registration*/
            luciControl.sendAsynchronousCommand();


            ArrayList<LUCIPacket> setOfCommandsWhileMakingSlave = new ArrayList<LUCIPacket>();
            if (mCSSID != null) {
                LUCIPacket concurrentSsid105Packet = new LUCIPacket(mCSSID.getBytes(), (short) mCSSID.length(), (short) MIDCONST.MID_SSID, (byte) LSSDPCONST.LUCI_SET);
                setOfCommandsWhileMakingSlave.add(concurrentSsid105Packet);
            }
            LUCIPacket zoneid104Packet = new LUCIPacket(mZoneID.getBytes(), (short) mZoneID.length(), (short) MIDCONST.MID_DDMS_ZONE_ID, (byte) LSSDPCONST.LUCI_SET);
            LibreLogger.d(this, "Slave ZoneID" + mZoneID);
            setOfCommandsWhileMakingSlave.add(zoneid104Packet);

            LUCIPacket setSlave100Packet = new LUCIPacket(LUCIMESSAGES.SETSLAVE.getBytes(), (short) LUCIMESSAGES.SETSLAVE.length(), (short) MIDCONST.MID_DDMS, (byte) LSSDPCONST.LUCI_SET);
            setOfCommandsWhileMakingSlave.add(setSlave100Packet);


            luciControl.SendCommand(setOfCommandsWhileMakingSlave);


            /*setting flag to LSSDP Node*/
            clickedNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED);

            LibreLogger.d(this, "We are trying to make " + clickedNode.getIP() + "as Slave for master device=" + mMasterNode.getIP() + " Where we have  master cssid= " + mCSSID + " zoneid=" + mZoneID);
            LibreLogger.d(this, "Second_We are trying to make " + clickedNode.getFriendlyname() + "as Slave for master device=" + mMasterNode.getFriendlyname() + " Where we have  master cssid= " + mCSSID + " zoneid=" + mZoneID);


        }

    }

    /*temporary change*/
    public void clickingOnSlaveOrMasterOld(final LSSDPNodes clickedNode, LSSDPNodes masteNode) {

        {
            if(clickedNode.getmWifiBand()==null
                    || masteNode.getmWifiBand() == null){
                mShowAlertToastForFailingGrouping();
                return;
            }
            if(clickedNode.getmWifiBand()!=null &&
                    masteNode.getmWifiBand()!=null) {
                if (!masteNode.getmWifiBand().equalsIgnoreCase(clickedNode.getmWifiBand())){
                    mShowAlertToastForFailingGrouping();
                    return;
                }
            }
          /*showing dialog only for Masters*/
            if (clickedNode.getDeviceState().equals("M")) {/*Changing -For Master : Freeing All the Slave Devices */
//Show a Dialog . if its YES , then Free it and respective Slave Devices otherwise Discard it
                new AlertDialog.Builder(context)
                        .setTitle(R.string.deviceStateChangingTitle)
                        .setMessage(R.string.freeingDeviceMsg)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Bundle b = new Bundle();


                                LibreLogger.d(this, "We are trying to make free the Master");
                                LibreLogger.d(this, "Freeing individual slaves begin now");

                                             /**/

                                ArrayList<LSSDPNodes> mSlaveListForMyMaster = m_scanHandler.getSlaveListForMasterIp(clickedNode.getIP(), mWifiMode);

                                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
                                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                                              /* Sending Asynchronous Registration*/
                                    luciControl.sendAsynchronousCommand();
                                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

                                    LibreLogger.d(this, "Sent command to be free for " + mSlaveNode);
                                    b.putString("ipAddress", mSlaveNode.getIP());

                                    Message msg = new Message();
                                    msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
                                    msg.setData(b);
                                    mManageDevicesHandler.sendMessage(msg);

                                }
                                LibreLogger.d(this, "After freeing all slaves we are freeing master");


                                LibreLogger.d(this, "Begining of Freeing of device " + clickedNode.getFriendlyname() + " With ip=" + clickedNode.getIP());


                                LUCIControl luciControl = new LUCIControl(clickedNode.getIP());
                                         /* Sending Asynchronous Registration*/
                                luciControl.sendAsynchronousCommand();
                                luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                                b.putString("ipAddress", clickedNode.getIP());
                                Message msg = new Message();
                                msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
                                msg.setData(b);
                                mManageDevicesHandler.sendMessage(msg);

                                LibreLogger.d(this, "SET FREE sent successfully for " + clickedNode.getFriendlyname() + " With ip=" + clickedNode.getIP());


                                String ipaddress = clickedNode.getIP();
                                DeviceData deviceData = mMasterSpecificSlaveAndFreeDeviceMap.get(ipaddress);
                                if (clickedNode.getDeviceState().equals("M")) {
                                    m_scanHandler.removeSceneMapFromCentralRepo(clickedNode.getIP());
                                }

                            /*setting flag to LSSDP Node*/
                                clickedNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(context, context.getResources().getString(R.string.deviceStateNotChanged), Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
//            if (clickedNode.getDeviceState().equals("M")) {/*Changing -For Master : Freeing All the Slave Devices */
//
//                LibreLogger.d(this, "We are trying to make free the Master");
//                LibreLogger.d(this, "Freeing individual slaves begin now");
//
//                                             /**/
//
//                ArrayList<LSSDPNodes> mSlaveListForMyMaster = m_scanHandler.getSlaveListForMasterIp(clickedNode.getIP(), mWifiMode);
//
//                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
//                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
//                                              /* Sending Asynchronous Registration*/
//                    luciControl.sendAsynchronousCommand();
//                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
//
//                    LibreLogger.d(this, "Sent command to be free for " + mSlaveNode);
//                    b.putString("ipAddress", mSlaveNode.getIP());
//
//                    Message msg = new Message();
//                    msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
//                    msg.setData(b);
//                    mManageDevicesHandler.sendMessage(msg);
//
//                }
//                LibreLogger.d(this, "After freeing all slaves we are freeing master");
//            }

            else {

                /*which means clicked on slave device*/
                Bundle b = new Bundle();
                LibreLogger.d(this, "Begining of Freeing of device " + clickedNode.getFriendlyname() + " With ip=" + clickedNode.getIP());
                if(masteNode!=null) {
                /*To Check Whether A Master is Avaliable or not , So we ae trying to Read Play State */
                    LUCIControl luciControlForMaster = new LUCIControl(masteNode.getIP());
                    ArrayList<LUCIPacket> luciPackets = new ArrayList<LUCIPacket>();
                    LUCIPacket packet7 = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_CURRENT_PLAY_STATE, (byte) LSSDPCONST.LUCI_GET);
                    luciPackets.add(packet7);
                    luciControlForMaster.SendCommand(luciPackets);
                }

                LUCIControl luciControl = new LUCIControl(clickedNode.getIP());
                                         /* Sending Asynchronous Registration*/
                luciControl.sendAsynchronousCommand();
                luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                b.putString("ipAddress", clickedNode.getIP());
                Message msg = new Message();
                msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
                msg.setData(b);
                mManageDevicesHandler.sendMessage(msg);

                LibreLogger.d(this, "SET FREE sent successfully for " + clickedNode.getFriendlyname() + " With ip=" + clickedNode.getIP());


                String ipaddress = clickedNode.getIP();
                DeviceData deviceData = mMasterSpecificSlaveAndFreeDeviceMap.get(ipaddress);
                if (clickedNode.getDeviceState().equals("M")) {
                    m_scanHandler.removeSceneMapFromCentralRepo(clickedNode.getIP());
                }
                            /*setting flag to LSSDP Node*/
                clickedNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED);
            }


        }
    }
}







