package com.libre.qactive.luci;
/*********************************************************************************************
 * Copyright (C) 2014 Libre Wireless Technology
 * <p/>
 * "Junk Yard Lab" Project
 * <p/>
 * Libre Sync Android App
 * Author: Subhajeet Roy
 ***********************************************************************************************/

import android.util.Log;

import com.cumulations.libreV2.tcp_tunneling.TunnelingControl;
import com.cumulations.libreV2.tcp_tunneling.enums.ModelId;
import com.libre.qactive.Scanning.ScanningHandler;
import com.libre.qactive.util.LibreLogger;

import java.util.ArrayList;

public class LSSDPNodeDB {

    private static LSSDPNodeDB LSSDPDB = new LSSDPNodeDB();
    private static ArrayList<LSSDPNodes> nodelist = new ArrayList<>();

    /*Filtered ones for Riva*/
    private static ArrayList<LSSDPNodes> filteredNodesList = new ArrayList<>();

    /* A private Constructor prevents any other
     * class from instantiating.
     */
    private LSSDPNodeDB() {
    }

    /* Static 'instance' method */
    public static LSSDPNodeDB getInstance() {

        return LSSDPDB;

    }

    public ArrayList<LSSDPNodes> GetDB() {
        return (ArrayList<LSSDPNodes>) nodelist.clone();
    }

    public ArrayList<LSSDPNodes> GetFilteredDB() {
        return (ArrayList<LSSDPNodes>) filteredNodesList.clone();
    }

    public boolean checkDataIsAvailable(String mNodeUSN) {
        for (LSSDPNodes mNode : nodelist) {
            if (mNode.getUSN().equals(mNodeUSN)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkFilteredNodeIsAvailable(String mNodeUSN) {
        for (LSSDPNodes mNode : filteredNodesList) {
            if (mNode.getUSN().equals(mNodeUSN)) {
                return true;
            }
        }
        return false;
    }

    public boolean addToNodeDb(LSSDPNodes node) {/* USN may be Empty When Device is transiting From SA State to Another State*/
        if(node.getUSN().isEmpty()){
            return false;
        }
        if (!checkDataIsAvailable(node.getUSN())) {
            nodelist.add(node);
            return true;
        }
        return false;/*
        if(!nodelist.contains(node)){
            LibreLogger.d(this,"Node Adding to List" +node.getFriendlyname());

            nodelist.remove(node);
            nodelist.add(node);
            return true;
        }
        return  false;// nodelist.add(node); */
    }

    public void addToFilteredNodeDb(LSSDPNodes node) {
        if(node.getUSN().isEmpty()){
            return;
        }

        if (!checkDataIsAvailable(node.getUSN())) {
            if (node.getModelId() == ModelId.Concert || node.getModelId() == ModelId.Stadium){
                filteredNodesList.add(node);
            }
        }
    }

    public void removeFromDB(LSSDPNodes node) {
        nodelist.remove(node);
        filteredNodesList.remove(node);
    }

    public void clearDB() {
        nodelist.clear();
        filteredNodesList.clear();
        deviceNameList.clear();
        ScanningHandler mHandler = ScanningHandler.getInstance();
    }


    public ArrayList<String> deviceNameList = new ArrayList<>();

    public LSSDPNodes getLSSDPNodeFromPosition(int position) {
        return nodelist.get(position);
    }

    public ArrayList<String> getLSSDPDeviceNameList() {
        return deviceNameList;
    }

    public void renewLSSDPNodeDataWithNewNode(LSSDPNodes mToBeChange) {
        boolean found = false;
        int size = GetDB().size();
        for (int i = 0; i < size; i++) {
            if (GetDB().get(i).getIP().equals(mToBeChange.getIP())) {

                if (!GetDB().get(i).getFriendlyname().equals(mToBeChange.getFriendlyname())) {
                    GetDB().get(i).setFriendlyname(mToBeChange.getFriendlyname());
                }

                if (!GetDB().get(i).getDeviceState().equals(mToBeChange.getDeviceState())) {
                    GetDB().get(i).setDeviceState(mToBeChange.getDeviceState());
                }

                if (!GetDB().get(i).getNetworkMode().equals(mToBeChange.getNetworkMode())) {
                    GetDB().get(i).setNetworkMode(mToBeChange.getNetworkMode());
                }

                if (!GetDB().get(i).getSpeakerType().equals(mToBeChange.getSpeakerType())) {
                    GetDB().get(i).setSpeakerType(mToBeChange.getSpeakerType());
                }

                if (!GetDB().get(i).getZoneID().equals(mToBeChange.getZoneID())) {
                    if (!mToBeChange.getZoneID().equals(""))
                        GetDB().get(i).setZoneID(mToBeChange.getZoneID());
                }

                if (!GetDB().get(i).getcSSID().equals(mToBeChange.getcSSID())) {
                    if (!mToBeChange.getcSSID().equals(""))
                        GetDB().get(i).setcSSID(mToBeChange.getcSSID());
                }

                if (!GetDB().get(i).getNodeAddress().equals(mToBeChange.getNodeAddress())) {
                    GetDB().get(i).setNodeAddress(mToBeChange.getNodeAddress());
                }

                if(mToBeChange.getgCastVerision()==null && GetDB().get(i).getgCastVerision()!=null){
                    GetDB().get(i).setgCastVerision(null);
                }else if(GetDB().get(i).getgCastVerision()==null && mToBeChange.getgCastVerision()!=null){
                    GetDB().get(i).setgCastVerision(mToBeChange.getgCastVerision());
                }else if(mToBeChange.getgCastVerision()==null && GetDB().get(i).getgCastVerision() ==null ) {
                }else if(!GetDB().get(i).getgCastVerision().equals(mToBeChange.getgCastVerision())){
                    GetDB().get(i).setgCastVerision(mToBeChange.getgCastVerision());
                }
                if(mToBeChange.getmDeviceCap()!=null){
                    GetDB().get(i).setmDeviceCap(mToBeChange.getmDeviceCap());
                }
                LibreLogger.d(this, GetDB().get(i).getFriendlyname() + ":" + GetDB().get(i).getDeviceState() + ":"
                        + GetDB().get(i).getZoneID() + " : " + GetDB().get(i).getcSSID());

            }
        }
    }

    public void clearNode(String mNodeAddress) {
        LibreLogger.d(this,"Clearing the Node "+ mNodeAddress);
        for (int i = 0; i < nodelist.size(); i++) {
            LSSDPNodes mNode = nodelist.get(i);
            if (mNode.getIP().equals(mNodeAddress)) {
                removeFromDB(mNode);
            }
        }

        TunnelingControl.removeTunnelingClient(mNodeAddress);
    }


    public LSSDPNodes getTheNodeBasedOnTheIpAddress(String ipaddress) {

        try {
            for (int i = 0; i < nodelist.size(); i++) {
                LSSDPNodes mNode = nodelist.get(i);
                if (mNode.getIP().equals(ipaddress)) {
                    return mNode;
                }
            }
        } catch (Exception e) {
            Log.d("EXCEPTION", "Error while returning the node from ip address");
        }

        return null;

    }
    public boolean isAnyMasterIsAvailableInNetwork(){

        for (int i = 0; i < nodelist.size(); i++) {
            LSSDPNodes mNode = nodelist.get(i);

            if(  mNode!= null && mNode.getDeviceState()!=null && mNode.getDeviceState().equalsIgnoreCase("M"))
                return true;
        }
        return false;
    }

    public boolean isDeviceInNodeRepo(){

        for (int i = 0; i < nodelist.size(); i++) {
            LSSDPNodes mNode = nodelist.get(i);

            if (mNode != null
                    && mNode.getDeviceState() != null
                    && (mNode.getDeviceState().equalsIgnoreCase("M")
                    || mNode.getDeviceState().equalsIgnoreCase("F")
                    /*|| mNode.getDeviceState().equalsIgnoreCase("S")*/))
                return true;
        }
        return false;
    }

    public static boolean isLS9ExistsInTheNetwork(){

        for (int i = 0; i < nodelist.size(); i++) {
            LSSDPNodes mNode = nodelist.get(i);

          if(  mNode.getgCastVerision()!=null)
              return true;
        }
        return false;

    }

    public ArrayList<LSSDPNodes> getAllMasterNodes(){
        ArrayList<LSSDPNodes> masterNodesList = new ArrayList<>();
        ArrayList<LSSDPNodes> nodesArrayList = GetDB();
        for (LSSDPNodes lssdpNodes : nodesArrayList){
            if (lssdpNodes.getDeviceState()!=null && !lssdpNodes.getDeviceState().isEmpty() &&
                    lssdpNodes.getDeviceState().equalsIgnoreCase("M")){
                masterNodesList.add(lssdpNodes);
            }
        }

        return masterNodesList;
    }

}

