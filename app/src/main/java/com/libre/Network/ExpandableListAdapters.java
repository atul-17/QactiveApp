package com.libre.Network;

/**
 * Created by karunakaran on 8/1/2015.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.libre.R;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.util.LibreLogger;

import java.util.HashMap;
import java.util.List;

public class ExpandableListAdapters extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader;
    private ExpandableListView mSpeakersExpandableListView;// header titles
    // child data in format of header title, child title
    private HashMap<String, List<String>> _listDataChild;

    private float screenHeight;

    public void clear(int mGroupPosition){
        String mGroupName = _listDataHeader.get(mGroupPosition);
        _listDataChild.get(mGroupName).clear();
    }
    public boolean isChildAvailable(int mGroupPosition,String mChild){
        String mGroupName = _listDataHeader.get(mGroupPosition);
        if( _listDataChild.get(mGroupName)==null)
            return false;
        if( _listDataChild.get(mGroupName).contains(mChild))
            return true;
        return false;
    }
    public void removeChildFromAdapter(int mGroupPosition, String mToBeRemovedChild) {
        String mGroupName = _listDataHeader.get(mGroupPosition);
        if( _listDataChild.get(mGroupName).contains(mToBeRemovedChild))
            _listDataChild.get(mGroupName).remove(mToBeRemovedChild);
    }
    public void addNewChildToAdapter(int mGroupPosition,String mNewDataChild){
        String mGroupName = _listDataHeader.get(mGroupPosition);
        if(! _listDataChild.get(mGroupName).contains(mNewDataChild))
            _listDataChild.get(mGroupName).add(mNewDataChild);
    }

    public ExpandableListAdapters(Context context, List<String> listDataHeader,
                                  HashMap<String, List<String>> listChildData, ExpandableListView mSpeakersExpandableListView) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
        this.mSpeakersExpandableListView = mSpeakersExpandableListView;
        screenHeight = (float)getScreenHeight();

    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {

        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        LayoutInflater infalInflater = (LayoutInflater) this._context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = infalInflater.inflate(R.layout.list_item, null);


        final LinearLayout childLayout = (LinearLayout) convertView
                .findViewById(R.id.device_layout);

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);
        String childText = "";
        LSSDPNodes mNode = null;
        String mGroupName = _listDataHeader.get(groupPosition);
        LibreLogger.d(this, "child count " + _listDataChild.get(mGroupName).size() + " header is " + mGroupName);
     /*   if (_listDataChild.get(mGroupName).size() >= 9) {
            // convertView.
            ViewGroup.LayoutParams params = mSpeakersExpandableListView.getLayoutParams();
            float localScreenHeight =  screenHeight *((float)3/4);
            params.height = (int)localScreenHeight;
            mSpeakersExpandableListView.setLayoutParams(params);
            mSpeakersExpandableListView.requestLayout();
        }*/


        if(groupPosition == 0){
            String mGettingChild = (String)getChild(groupPosition, childPosition);
            if(!mGettingChild.contains(_context.getResources().getString(R.string.title_no_lssdp_device))
            && !mGettingChild.contains(_context.getResources().getString(R.string.title_for_refreshing))) {
               mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress((mGettingChild));
                if (mNode != null) {
                    childText = mNode.getFriendlyname();
                } else {
                    /*hiding row*/
                    LibreLogger.d(this, "Hiding row as node is null");
                    childLayout.setVisibility(View.GONE);
                }
            }else{
                childText = mGettingChild;
            }
        }else {
            childText = (String) getChild(groupPosition, childPosition);
        }
        {
/*

                LayoutInflater infalInflater = (LayoutInflater) this._context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.list_item, null);


                TextView txtListChild = (TextView) convertView
                        .findViewById(R.id.lblListItem);
*/

                txtListChild.setText(childText);

                return convertView;

        }

    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        /*lblListHeader.setVisibility(View.GONE);*/
        lblListHeader.setText(headerTitle);
        lblListHeader.setClickable(false);
        lblListHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LibreLogger.d(this,"click is disabled");
            }
        });
        convertView.setClickable(false);
        ExpandableListView mExpandableListView = (ExpandableListView) parent;
        mExpandableListView.expandGroup(groupPosition);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public int getScreenHeight() {

        int screenHeight = _context.getResources().getDisplayMetrics().heightPixels;

        LibreLogger.d(this, "Height is " + screenHeight);
        return screenHeight;
    }

}