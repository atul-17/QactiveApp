package com.libre.qactive.Ls9Sac;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.libre.qactive.LibreApplication;
import com.libre.qactive.R;
import com.libre.qactive.util.LibreLogger;

/**
 * Created by karunakaran on 7/17/2016.
 */
public class GcastUpdateAdapter extends BaseAdapter{

    private Context mContext;
    public GcastUpdateAdapter(Context mContext){
        this.mContext = mContext;
    }
    @Override
    public int getCount() {
        try {
            return LibreApplication.FW_UPDATE_AVAILABLE_LIST.keySet().toArray().length;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public class GcastHolder {
        LinearLayout mRelative;
        TextView DeviceName, mPercentageToDisplay,msgCastUpdate;
        ProgressBar proloader;
        String mIPAddress;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GcastHolder mGcastHolder;
        final String ipAddress = (String) LibreApplication.FW_UPDATE_AVAILABLE_LIST.keySet().toArray()[position];
        LibreLogger.d(this, "Whole Key" + LibreApplication.FW_UPDATE_AVAILABLE_LIST.keySet().toString());
        LibreLogger.d(this, "IP " + ipAddress);


        if(convertView==null)
        {
            mGcastHolder = new GcastHolder();
            LayoutInflater inflater = (LayoutInflater) (mContext).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.gcast_list_item, null);
            mGcastHolder.DeviceName = (TextView)convertView.findViewById(R.id.gCastDeviceName);
            mGcastHolder.mPercentageToDisplay = (TextView)convertView.findViewById(R.id.gcast_perecentage_update);
            mGcastHolder.proloader = (ProgressBar)convertView.findViewById(R.id.gCastProgressBar);
            mGcastHolder.mIPAddress = ipAddress;
            mGcastHolder.msgCastUpdate = (TextView) convertView.findViewById(R.id.tvMsgCastUpdate);
            convertView.setTag(mGcastHolder);
        }
        mGcastHolder = (GcastHolder)convertView.getTag();

        FwUpgradeData mGcastData = LibreApplication.FW_UPDATE_AVAILABLE_LIST.get(ipAddress);
        if(mGcastData!=null){
            mGcastHolder.DeviceName.setText(mGcastData.getmDeviceName());
            mGcastHolder.msgCastUpdate.setText(mGcastData.getUpdateMsg());
            if(mGcastData.getmProgressValue()==255){
                mGcastHolder.mPercentageToDisplay.setText(mContext.getString(R.string.fwUpdateFailed));
                mGcastHolder.proloader.setEnabled(false);
            }else {
                mGcastHolder.mPercentageToDisplay.setText(mGcastData.getmProgressValue() + " %");
                mGcastHolder.proloader.setProgress(mGcastData.getmProgressValue());
            }
        }
        return convertView;
    }
}
