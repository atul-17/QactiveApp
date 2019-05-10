package com.libre.Ls9Sac;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.libre.LibreApplication;
import com.libre.R;
import com.libre.util.LibreLogger;

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
            return LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.keySet().toArray().length;
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
        final String ipAddress = (String) LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.keySet().toArray()[position];
        LibreLogger.d(this, "Whole Key" + LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.keySet().toString());
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

        GcastUpdateData mGcastData = LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.get(ipAddress);
        if(mGcastData!=null){
            mGcastHolder.DeviceName.setText(mGcastData.getmDeviceName());
            mGcastHolder.msgCastUpdate.setText(mGcastData.getmGcastUpdate());
            if(mGcastData.getmProgressValue()==255){
                mGcastHolder.mPercentageToDisplay.setText(mContext.getString(R.string.gcastFailed));
                mGcastHolder.proloader.setEnabled(false);
            }else {
                mGcastHolder.mPercentageToDisplay.setText(mGcastData.getmProgressValue() + " %");
                mGcastHolder.proloader.setProgress(mGcastData.getmProgressValue());
            }
        }
        return convertView;
    }
}
