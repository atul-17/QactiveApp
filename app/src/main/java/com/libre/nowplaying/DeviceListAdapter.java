package com.libre.nowplaying;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.ActiveScenesListActivity;
import com.libre.ManageDevice.ManageDeviceHandler;
import com.libre.R;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.constants.CommandType;
import com.libre.constants.DeviceMasterSlaveFreeConstants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.util.LibreLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Created by karunakaran on 8/5/2015.
 */
public class DeviceListAdapter extends ArrayAdapter<SceneObject> {

    public static LinkedHashMap<String, SceneObject>   sceneMap = new LinkedHashMap<>();
    Context context;
    String masterIP;
    Handler mDeviceListHandler;
    public int NoOfDevicesSelected = 0;
    public ScanningHandler m_scanHandler = ScanningHandler.getInstance();
    public ManageDeviceHandler m_ManageDeviceHandler = ManageDeviceHandler.getInstance();

    public DeviceListAdapter(Context context, int textViewResourceId,String mMasterIP,Handler m_DeviceListHandler) {
        super(context, textViewResourceId);
        this.context=context;
        this.masterIP = mMasterIP;
        this.mDeviceListHandler = m_DeviceListHandler;
    }

    @Override
    public int getCount() {

        try {
            return sceneMap.keySet().toArray().length;
        } catch (Exception e) {
            return 0;
        }
    }
    @Override
    public void add(SceneObject object) {
        sceneMap.put(object.getIpAddress(), object);
        super.add(object);
    }

    @Override
    public void addAll(Collection<? extends SceneObject> collection) {
        super.addAll(collection);

        Iterator<? extends SceneObject> iterator = collection.iterator();
        while (iterator.hasNext()) {
            SceneObject sceneObject = iterator.next();
            sceneMap.put(sceneObject.getIpAddress(), sceneObject);
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
        sceneMap = new LinkedHashMap<>();
    }

    @Override
    public void remove(SceneObject object) {
        super.remove(object);
        sceneMap.remove(object.getIpAddress());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public class Holder {
        RelativeLayout mRelative;
        TextView DeviceStatus,NoOfDevices,DeviceName;
        ImageView mMute;
        SeekBar volumebar;
        String ipaddress;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Holder holder;

        LibreLogger.d("getView", "" + position);
        String ipaddress = (String) sceneMap.keySet().toArray()[position];
        LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(ipaddress);

        if (convertView==null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.manage_devices_list_item, null);
            holder.DeviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            holder.DeviceStatus = (TextView) convertView.findViewById(R.id.tvMasterSlave);
            holder.volumebar = (SeekBar) convertView.findViewById(R.id.seekBar);
            holder.mMute = (ImageView) convertView.findViewById(R.id.mute_button);
            holder.mRelative=(RelativeLayout) convertView.findViewById(R.id.managedeviceLayout);
            /* First View Creation*/
            if(node != null) {
                if (node.getDeviceState().equals("M")) {
                    holder.DeviceStatus.setText("Master");
                    holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
                } else if (node.getDeviceState().equals("S")) {
                    holder.DeviceStatus.setText("Slave");
                    holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
                } else if (node.getDeviceState().equals("F")) {
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
        holder.mRelative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);
                final LSSDPNodes mMasterNode = m_scanHandler.getLSSDPNodeFromCentralDB(masterIP);

                if(node==null ||mMasterNode==null)
                    return;


                Message msg = new Message();
                switch (node.getDeviceState()) {
                    case "F": {
                        //Make it as Slave
                        msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_SLAVE_COMMAND;
                        mDeviceListHandler.sendEmptyMessage(msg.what);

                        String mCSSID = mMasterNode.getcSSID();
                        String mZoneID = mMasterNode.getZoneID();


                         /* Sending Luci Command For Slave*/
                        LUCIControl luciControl = new LUCIControl(node.getIP());
                         /* Sending Asynchronous Registration*/
                        luciControl.sendAsynchronousCommand();
                        ArrayList<LUCIPacket> setOfCommandsWhileMakingSlave = new ArrayList<LUCIPacket>();
                        if (mCSSID != null) {
                            LUCIPacket concurrentSsid105Packet = new LUCIPacket(mCSSID.getBytes(),
                                    (short) mCSSID.length(), (short) MIDCONST.MID_SSID,
                                    (byte) LSSDPCONST.LUCI_SET);
                            setOfCommandsWhileMakingSlave.add(concurrentSsid105Packet);
                            LibreLogger.d(this, "Slave Concurrent SSID" + mCSSID);

                        }
                        LUCIPacket zoneid104Packet = new LUCIPacket(mZoneID.getBytes(),
                                (short) mZoneID.length(), (short) MIDCONST.MID_DDMS_ZONE_ID, (byte) LSSDPCONST.LUCI_SET);
                        setOfCommandsWhileMakingSlave.add(zoneid104Packet);

                        LibreLogger.d(this, "Slave ZoneID" + mZoneID);
                        LUCIPacket setSlave100Packet = new LUCIPacket(LUCIMESSAGES.SETSLAVE.getBytes(),
                                (short) LUCIMESSAGES.SETSLAVE.length(), (short) MIDCONST.MID_DDMS, (byte) LSSDPCONST.LUCI_SET);
                        setOfCommandsWhileMakingSlave.add(setSlave100Packet);


                        luciControl.SendCommand(setOfCommandsWhileMakingSlave);
                        /*if (mCSSID != null) {
                            luciControl.SendCommand(MIDCONST.MID_SSID, mCSSID, LSSDPCONST.LUCI_SET);
                        }
                        luciControl.SendCommand(MIDCONST.MID_DDMS_ZONE_ID, mZoneID, LSSDPCONST.LUCI_SET);
                        luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETSLAVE, LSSDPCONST.LUCI_SET);
*/
                         /*Getting Scene Object*/
                        SceneObject scene = sceneMap.get(node.getIP());

                         /* UI Updation*/
                        holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));
                        holder.DeviceName.setText(node.getFriendlyname());
                        holder.DeviceStatus.setText("Slave");
                        holder.mMute.setVisibility(View.VISIBLE);
                        holder.volumebar.setVisibility(View.VISIBLE);
                    }
                    break;
                    case "S":
                        //Show a Dialog . if its YES , then Free it otheewise Discard it
                    case "M": {
                        //Show a Dialog . if its YES , then Free it and respective Slave Devices otherwise Discard it
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.deviceStateChanging)
                                .setMessage(R.string.deviceNameChangingMsg)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mDeviceListHandler.sendEmptyMessage(DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND);
                                        if (node.getDeviceState().equals("M")) {/*Changing For Master*/
                                            String[] ipaddress = sceneMap.keySet().toArray(new String[0]);
                                            for (int i = 0; i < ipaddress.length; i++) {
                                                LSSDPNodes mSlaveNode = m_scanHandler.getLSSDPNodeFromCentralDB(ipaddress[i]);
                                                String mSlaveCssid = mSlaveNode.getcSSID();
                                                String mMasterCssid = mMasterNode.getcSSID();
                                                if (mSlaveNode!=null)
                                                {
                                                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                                                     /* Sending Asynchronous Registration */
                                                    luciControl.sendAsynchronousCommand();
                                                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                                                }
                                            }
                                        }
                                        LUCIControl luciControl = new LUCIControl(holder.ipaddress);
                                         /* Sending Asynchronous Registration*/
                                        luciControl.sendAsynchronousCommand();
                                        luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

                                        if (node.getDeviceState().equals("M")) {
                                            Intent intent = new Intent(context, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            context.startActivity(intent);
                                        }
                                        holder.mRelative.setBackground(context.getResources().getDrawable(R.drawable.app_rounded_corners_list_item));
                                        String ipaddress = holder.ipaddress;
                                        SceneObject scene = sceneMap.get(ipaddress);

                                        holder.DeviceName.setText(scene.getSceneName());
                                        holder.DeviceStatus.setText("Free");
                                        holder.mMute.setVisibility(View.INVISIBLE);
                                        holder.volumebar.setVisibility(View.INVISIBLE);


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
                    break;
                }
            }
        });
        if (node != null) {
            if (!node.getDeviceState().equals("F")) {
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
                        SceneObject scene = sceneMap.get(holder.ipaddress);
                        LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.getProgress(), CommandType.SET, holder.ipaddress);
                    }
                });
            }
        }
        try {
            SceneObject scene = sceneMap.get(holder.ipaddress);
            if (scene != null) {
                if(node.getDeviceState().equals("F")) {
                    holder.DeviceName.setText(scene.getSceneName());
                }else{
                    holder.DeviceName.setText(scene.getSceneName());
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return convertView;
    }


    public SceneObject getSceneObjectFromAdapter(String ip){
        return   sceneMap.get(ip);
    }
}
