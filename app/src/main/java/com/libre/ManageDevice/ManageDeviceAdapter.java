package com.libre.ManageDevice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.util.LibreLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by karunakaran on 8/5/2015.
 */
public class ManageDeviceAdapter  extends ArrayAdapter<SceneObject>  {

    public static ConcurrentHashMap<String, SceneObject> mMasterSpecificSlaveAndFreeDeviceMap = new ConcurrentHashMap<>();
    Context context;
    Handler mManageDevicesHandler;
    String mMasterIp;
    String mActivityName;

    public final int STEREO =0;
    public final int LEFT = 1;
    public final int RIGHT =2;

    public ScanningHandler m_scanHandler = ScanningHandler.getInstance();
    public ManageDeviceHandler m_ManageDeviceHandler = ManageDeviceHandler.getInstance();
    public int mWifiMode = -1;

    public ManageDeviceAdapter(Context context, int textViewResourceId,
                               Handler mHandler,String m_MasterIp
    ,String m_ActivityName) {
        super(context, textViewResourceId);
        this.context=context;
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
    @Override
    public void add(SceneObject object) {
        mMasterSpecificSlaveAndFreeDeviceMap.put(object.getIpAddress(), object);
        super.add(object);
    }

    @Override
    public void addAll(Collection<? extends SceneObject> collection) {
        super.addAll(collection);

        Iterator<? extends SceneObject> iterator = collection.iterator();
        while (iterator.hasNext()) {
            SceneObject sceneObject = iterator.next();
            mMasterSpecificSlaveAndFreeDeviceMap.put(sceneObject.getIpAddress(), sceneObject);
        }
    }


    @Override
    public void insert(SceneObject object, int index) {
        super.insert(object, index);
    }

    @Override
    public void sort(Comparator<? super SceneObject> comparator) {
        super.sort(comparator);
    }

    @Override
    public void setNotifyOnChange(boolean notifyOnChange) {
        super.setNotifyOnChange(notifyOnChange);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public SceneObject getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getPosition(SceneObject item) {
        return super.getPosition(item);
    }

    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
    }

    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @Override
    public void clear() {
        super.clear();
        mMasterSpecificSlaveAndFreeDeviceMap.clear();
        mMasterSpecificSlaveAndFreeDeviceMap = new ConcurrentHashMap<>();
    }

    @Override
    public void remove(SceneObject object) {
        super.remove(object);
        if(object!=null)
            mMasterSpecificSlaveAndFreeDeviceMap.remove(object.getIpAddress());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class Holder {
        RelativeLayout mRelative;
        TextView DeviceName,NoOfDevices,DeviceStatus,mSpeakerType;
        ImageButton mute;
        SeekBar volumebar;
        String ipaddress;
        ImageButton mArrowForLSSDPSettings;
        ImageView mMute;
    }
    private String getSlaves() {
        StringBuilder ss = new StringBuilder();
        ss.append("SLAVEINFO_");
        /*for (HashMap node : data) {
            if (node.get(GroupListActivity.KEY_STATE).equals(GroupListActivity.STATION)) {
                ss.append(node.get(GroupListActivity.KEY_NAME)).append("+");
            }
        }*/
        final LSSDPNodes mMasterNode = m_scanHandler.getLSSDPNodeFromCentralDB(mMasterIp);
        String[] slaveIpaddress = mMasterSpecificSlaveAndFreeDeviceMap.keySet().toArray(new String[0]);
        for (int i = 0; i < slaveIpaddress.length&mMasterNode!=null; i++) {
            try {
            LSSDPNodes mSlaveNode = m_scanHandler.getLSSDPNodeFromCentralDB(slaveIpaddress[i]);

            String mSlaveCssid = mSlaveNode.getcSSID();
            String mMasterCssid = mMasterNode.getcSSID();


                if (mSlaveCssid!=null && mMasterCssid!=null && mSlaveCssid.contains(mMasterCssid)) {
                    ss.append(mSlaveNode.getFriendlyname()).append("+");
                }
            }catch(Exception e){

            }
        }
        /*to remove end + sign*/
        if (ss.length() > 10)
            ss.setLength(ss.length() - 1);

        return ss.toString();
    }

    @Override
    public View getView(final int position,  View convertView, ViewGroup parent) {
        final Holder holder;

        LibreLogger.d(this, "" + position);

        String ipaddress = (String) mMasterSpecificSlaveAndFreeDeviceMap.keySet().toArray()[position];
        LibreLogger.d(this, "Whole Key" + mMasterSpecificSlaveAndFreeDeviceMap.keySet().toString());
        LibreLogger.d(this, "IP " + ipaddress);

        LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(ipaddress);

     //   m_scanHandler.updateSlaveToMasterHashMap();

        if (convertView==null) {

            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.manage_devices_list_item, null);
            holder.DeviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            holder.DeviceStatus = (TextView) convertView.findViewById(R.id.tvMasterSlave);
            holder.volumebar = (SeekBar) convertView.findViewById(R.id.seekBar);
            holder.mMute = (ImageView) convertView.findViewById(R.id.mute_button);
            holder.mRelative=(RelativeLayout) convertView.findViewById(R.id.managedeviceLayout);
            holder.mSpeakerType = (TextView)convertView.findViewById(R.id.tvSpeakerType);
            holder.mArrowForLSSDPSettings = (ImageButton) convertView.findViewById(R.id.imgView_arrow);
            /* First View Creation*/
            if(node != null) {
                Log.e("ManageDevice",node.getDeviceState()+"::"+node.getFriendlyname());
                if (node.getDeviceState().equals("M")) {
                    holder.DeviceStatus.setText("Master");
                    holder.volumebar.setVisibility(View.VISIBLE);
                    holder.mMute.setVisibility(View.VISIBLE);
                    holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
                } else if (node.getDeviceState().equals("S")) {
                    holder.DeviceStatus.setText("Slave");
                    holder.volumebar.setVisibility(View.VISIBLE);
                    holder.mMute.setVisibility(View.VISIBLE);
                    holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
                } else if (node.getDeviceState().equals("F")) {
                    holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item));
                    holder.DeviceStatus.setText("Free");
                    holder.volumebar.setVisibility(View.INVISIBLE);
                    holder.mMute.setVisibility(View.INVISIBLE);
                }
            }
            convertView.setTag(holder);
            //ipaddress = (String) mMasterSpecificSlaveAndFreeDeviceMap.keySet().toArray()[position];
            holder.ipaddress=ipaddress;
        }
        else
            holder=(Holder)convertView.getTag();

        if(node != null) {
            Log.e("ManageDevice",node.getDeviceState()+"::"+node.getFriendlyname());
            if (node.getDeviceState().equals("M")) {
                holder.DeviceStatus.setText("Master");
                holder.volumebar.setVisibility(View.VISIBLE);
                holder.mMute.setVisibility(View.VISIBLE);
                holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
            } else if (node.getDeviceState().equals("S")) {
                holder.DeviceStatus.setText("Slave");
                holder.volumebar.setVisibility(View.VISIBLE);
                holder.mMute.setVisibility(View.VISIBLE);
                holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
            } else if (node.getDeviceState().equals("F")) {
                holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item));
                holder.DeviceStatus.setText("Free");
                holder.volumebar.setVisibility(View.INVISIBLE);
                holder.mMute.setVisibility(View.INVISIBLE);
            }
        }

        holder.mArrowForLSSDPSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ssid = new Intent(context, LSSDPDeviceNetworkSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                final LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);
                if(node!=null){
                ssid.putExtra("DeviceName", node.getFriendlyname());
                ssid.putExtra(Constants.CURRENT_DEVICE_IP, node.getIP());
                ssid.putExtra("activityName", "ManageDevices");
                context.startActivity(ssid);
                }
            }
        });
        holder.mRelative.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 final LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);
                 final LSSDPNodes mMasterNode = m_scanHandler.getLSSDPNodeFromCentralDB(mMasterIp);
                 if(node==null || mMasterNode==null)
                     return ;

                 switch (node.getDeviceState()) {
                     case "F": {
                         //Make it as Slave
                         Message msg = new Message();
                         msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_SLAVE_COMMAND;
                         mManageDevicesHandler.sendEmptyMessage(msg.what);
                        if(mMasterNode!=null) {
                            String mCSSID = mMasterNode.getcSSID();
                            String mZoneID = mMasterNode.getZoneID();
                         /* Sending Luci Command For Slave*/
                            LUCIControl luciControl = new LUCIControl(node.getIP());
                         /* Sending Asynchronous Registration*/
                            luciControl.sendAsynchronousCommand();

                            if (mCSSID != null) {
                                luciControl.SendCommand(MIDCONST.MID_SSID, mCSSID, LSSDPCONST.LUCI_SET);
                                LibreLogger.d(this, "Slave Concurrent SSID" + mCSSID);
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            luciControl.SendCommand(MIDCONST.MID_DDMS_ZONE_ID, mZoneID, LSSDPCONST.LUCI_SET);
                            LibreLogger.d(this, "Slave ZoneID" + mZoneID);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETSLAVE, LSSDPCONST.LUCI_SET);

                            if (mWifiMode != m_scanHandler.SA_MODE) { /* Sending Slave Info Only in HN Mode */
                                LUCIControl mMluciControl = new LUCIControl(mMasterIp);
                                mMluciControl.sendAsynchronousCommand();
                                mMluciControl.SendCommand(MIDCONST.MID_DDMS_SLAVEINFO, getSlaves(), LSSDPCONST.LUCI_SET);
                            }
                        /* UI Updation*/
                            holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
                            holder.DeviceName.setText(node.getFriendlyname());
                            holder.DeviceStatus.setText("Slave");
                            holder.mMute.setVisibility(View.VISIBLE);
                            holder.volumebar.setVisibility(View.VISIBLE);
                        }



                     }
                     break;
                     case "S":
                         //Show a Dialog . if its YES , then Free it otheewise Discard it
                     case "M": {
                         //Show a Dialog . if its YES , then Free it and respective Slave Devices otherwise Discard it
                         new AlertDialog.Builder(context)
                                 .setTitle("Device State Changing")
                                 .setMessage("Are you sure you want to Free this Device ?")
                                 .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                     public void onClick(DialogInterface dialog, int which) {
                                         Bundle b = new Bundle();
                                         if (node.getDeviceState().equals("M")) {/*Changing -For Master : Freeing All the Slave Devices */
                                             /**/

                                             ArrayList<LSSDPNodes> mSlaveListForMyMaster = m_scanHandler.getSlaveListForMasterIp(node.getIP(),mWifiMode);

                                             for(LSSDPNodes mSlaveNode : mSlaveListForMyMaster){
                                                 LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                                              /* Sending Asynchronous Registration*/
                                                 luciControl.sendAsynchronousCommand();
                                                 luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

                                                 b.putString("ipAddress", mSlaveNode.getIP());
                                                 Message msg = new Message();
                                                 msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND;
                                                 msg.setData(b);
                                                 mManageDevicesHandler.sendMessage(msg);

                                                 LUCIControl mMluciControl = new LUCIControl(node.getIP());
                                                 mMluciControl.sendAsynchronousCommand();
                                                 mMluciControl.SendCommand(MIDCONST.MID_DDMS_SLAVEINFO, getSlaves(), LSSDPCONST.LUCI_SET);

                                             }                                         /**/
                                         }
                                         LUCIControl luciControl = new LUCIControl(node.getIP());
                                         /* Sending Asynchronous Registration*/
                                         luciControl.sendAsynchronousCommand();
                                         luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                                         b.putString("ipAddress", node.getIP());
                                         Message msg = new Message();
                                         msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND;
                                         msg.setData(b);
                                         mManageDevicesHandler.sendMessage(msg);

                                         holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item));

                                         String ipaddress = holder.ipaddress;
                                         SceneObject scene = mMasterSpecificSlaveAndFreeDeviceMap.get(ipaddress);
                                         if(node.getDeviceState().equals("M")) {

                                            m_scanHandler.removeSceneMapFromCentralRepo(node.getIP());

                                         }

                                        holder.DeviceName.setText(scene.getSceneName());
                                        holder.DeviceStatus.setText("Free");
                                         holder.mMute.setVisibility(View.INVISIBLE);
                                        holder.volumebar.setVisibility(View.INVISIBLE);                                     }
                                 })
                                 .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                     public void onClick(DialogInterface dialog, int which) {
                                         Toast.makeText(context, "Device state Not Changed", Toast.LENGTH_SHORT).show();
                                         dialog.cancel();
                                     }
                                 })
                                 .setIcon(android.R.drawable.ic_dialog_alert)
                                 .show();
                     }
                     break;
                 }
             }
         });

        holder.mMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LUCIControl muteLuciControl = new LUCIControl(holder.ipaddress);
                muteLuciControl.sendAsynchronousCommand();
                muteLuciControl.SendCommand(MIDCONST.MID_MUTE, LUCIMESSAGES.MUTE, LSSDPCONST.LUCI_SET);
            }
        });
         /*setting views*/

        if (node != null) {
             if (!node.getDeviceState().equals("F")) {



                 holder.volumebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                     @Override
                     public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                         Log.d("onProgresChanged" + progress, "" + position);
                         //holder.mRelative.setClickable(false);
                     }

                     @Override
                     public void onStartTrackingTouch(SeekBar seekBar) {
                         Log.d("onStartTracking", "" + position);
                     }

                     @Override
                     public void onStopTrackingTouch(SeekBar seekBar) {

                         LibreLogger.d("onStopTracking", "" + position);
                         //holder.mRelative.setClickable(true);
                         SceneObject scene = mMasterSpecificSlaveAndFreeDeviceMap.get(holder.ipaddress);
                         LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.getProgress(), CommandType.SET, holder.ipaddress);


                         SceneObject sceneObjectFromCentralRepo = m_scanHandler.getSceneObjectFromCentralRepo(holder.ipaddress);

                         if(sceneObjectFromCentralRepo!=null){
                             sceneObjectFromCentralRepo.setVolumeValueInPercentage(seekBar.getProgress());
                             m_scanHandler.putSceneObjectToCentralRepo(holder.ipaddress,sceneObjectFromCentralRepo);
                         }
                         if(scene!=null) {
                             scene.setVolumeValueInPercentage(seekBar.getProgress());
                             mMasterSpecificSlaveAndFreeDeviceMap.put(holder.ipaddress,scene);
                         }

                     }
                 });
             }
        }
        try {
            holder.ipaddress =  (String) mMasterSpecificSlaveAndFreeDeviceMap.keySet().toArray()[position];
            SceneObject scene = mMasterSpecificSlaveAndFreeDeviceMap.get(holder.ipaddress);
            LSSDPNodes mNode = m_scanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);

            SceneObject sceneObjectFromCentralRepo = m_scanHandler.getSceneObjectFromCentralRepo(holder.ipaddress);

            if(sceneObjectFromCentralRepo!=null){
               holder.volumebar.setProgress(sceneObjectFromCentralRepo.getVolumeValueInPercentage());
            }else {
                if (scene!=null && scene.getVolumeValueInPercentage() >= 0)
                    holder.volumebar.setProgress(scene.getVolumeValueInPercentage());
            }

          /*  if(scene.getVolumeValueInPercentage()==-1){
                SceneObject mMasterScene = getSceneObjectFromAdapter(mMasterIp);
                if(mMasterScene!=null)
                    holder.volumebar.setProgress(mMasterScene.getVolumeValueInPercentage());
            }*/

            if (scene != null) {
                if(node.getDeviceState().equals("F")) {
                    holder.DeviceName.setText(scene.getSceneName());
                }else{
                    holder.DeviceName.setText(scene.getSceneName());
                }
                if(mNode!=null) {
                    if (Integer.valueOf(mNode.getSpeakerType()) == STEREO) {
                        holder.mSpeakerType.setText("Stereo");
                    } else if (Integer.valueOf(mNode.getSpeakerType()) == LEFT) {
                        holder.mSpeakerType.setText("Left");
                    } else if (Integer.valueOf(mNode.getSpeakerType()) == RIGHT) {
                        holder.mSpeakerType.setText("Right");
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return convertView;
    }

    public SceneObject getSceneObjectFromAdapter(String ip){
        return   mMasterSpecificSlaveAndFreeDeviceMap.get(ip);
    }
}
