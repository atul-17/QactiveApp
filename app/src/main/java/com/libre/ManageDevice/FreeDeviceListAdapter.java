package com.libre.ManageDevice;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.libre.R;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.util.LibreLogger;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by karunakaran on 8/8/2015.
 */
public class FreeDeviceListAdapter extends ArrayAdapter<LSSDPNodes> {

    private Context mContext;
    private int id;
    public boolean isLS9Available;
    private boolean isFreeDeviceAvailable;

    public static ArrayList <LSSDPNodes>FreeDeviceLSSDPNodeMap = new ArrayList<LSSDPNodes>();
    ScanningHandler mScanHandler = ScanningHandler.getInstance();
    public FreeDeviceListAdapter(Context context, int textViewResourceId)
    {

        super(context, textViewResourceId);
        mContext = context;
        id = textViewResourceId;


    }

    public boolean FreedeviceListAvailable(LSSDPNodes mNewNode){            
        for(LSSDPNodes mOldNode : FreeDeviceLSSDPNodeMap){
            if(mOldNode!=null && mNewNode != null && mOldNode.getUSN().equalsIgnoreCase(mNewNode.getUSN())){
                return true;
            }
        }
        return false;
    }

    public boolean isFreeDeviceAvailable(){
        return isFreeDeviceAvailable;
    }
    @Override
    public void add(LSSDPNodes object) {
        if(!FreedeviceListAvailable(object)) {
            isFreeDeviceAvailable=true;
            FreeDeviceLSSDPNodeMap.add(object);
        }
        super.add(object);
    }

    @Override
    public void addAll(Collection collection) {
        super.addAll(collection);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public void clear() {
        FreeDeviceLSSDPNodeMap.clear();
        super.clear();
    }

    @Override
    public int getCount() {

        isLS9Available= LSSDPNodeDB.isLS9ExistsInTheNetwork();
        isFreeDeviceAvailable =false;
        if(isLS9Available){
            if(LSSDPNodeDB.getInstance().GetDB().get(0).getNetworkMode().contains("P2P")){
                isLS9Available=false;
            }
        }
        if(isLS9Available)
            return FreeDeviceLSSDPNodeMap.size()+1;
        else
        {
            if(FreeDeviceLSSDPNodeMap.size()>0)
                return FreeDeviceLSSDPNodeMap.size();
            else{
                // returning 1 because getView will be called only when getCount returns 0.
                // variable isFreeDeviceAvailable is used to later filter in getView method.
                isFreeDeviceAvailable =true;
                return 1;
            }

        }
    }

    @Override
    public void remove(LSSDPNodes object) {
        FreeDeviceLSSDPNodeMap.remove(object);
        super.remove(object);
    }

    @Override
    public View getView(int position, final View v, ViewGroup parent)
    {
        View mView = v ;
        if(mView == null){
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = vi.inflate(id, null);
        }
        ManageDeviceHandler mManage = ManageDeviceHandler.getInstance();
        String ipaddress = "";
        RelativeLayout mRelative = (RelativeLayout) mView.findViewById(R.id.list_item_layout);
        TextView text = (TextView) mView.findViewById(R.id.list_item_textView);
        TextView mCastPlayingTv = (TextView) mView.findViewById(R.id.tvCastIsPlaying);
        ImageView image = (ImageView) mView.findViewById(R.id.manage_device_speaker);

        if(!isFreeDeviceAvailable) {
            LibreLogger.d(this, "device count is " + getCount());
            image.setVisibility(View.VISIBLE);
            if (isLS9Available && position == FreeDeviceLSSDPNodeMap.size()) {
                text.setText(R.string.createCastGrp);
                image.setImageResource(R.mipmap.ic_cast_white_24dp_2x);
            } else {
                if( FreeDeviceLSSDPNodeMap.get(position).getCurrentSource() == MIDCONST.GCAST_SOURCE
                        && FreeDeviceLSSDPNodeMap.get(position).getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                    mView.setBackground(mContext.getResources().getDrawable(R.drawable.app_rounded_cast_playing));
                    mCastPlayingTv.setVisibility(View.VISIBLE);
                    mCastPlayingTv.setText(R.string.CastPlaying);
                }else{
                    mRelative.setBackgroundResource(0);
                    mCastPlayingTv.setVisibility(View.INVISIBLE);
                    if(position == mManage.selectedPosition) {
                        mView.setSelected(true);
                        mView.setBackground(mContext.getResources().getDrawable(R.drawable.app_rounded_corners_list_item_selected));

                    }
                }
                text.setText(FreeDeviceLSSDPNodeMap.get(position).getFriendlyname());
                image.setImageResource(R.mipmap.speaker_icon);
            }
        }else{
            text.setText(R.string.noDeviceFound);
            image.setVisibility(View.GONE);
        }
        return mView;
    }

}