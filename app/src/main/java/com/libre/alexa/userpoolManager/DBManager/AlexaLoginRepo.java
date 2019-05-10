package com.libre.alexa.userpoolManager.DBManager;

import android.support.v7.app.AlertDialog;

import java.util.HashMap;

/**
 * Created by bhargav on 21/6/17.
 */
public class AlexaLoginRepo {
    String username;
    HashMap<String , String> linkedDeviceDetails = new HashMap<>();
    static AlexaLoginRepo alexaLoginRepo;
    private AlexaLoginRepo(){
    }
    public static AlexaLoginRepo getRepo(){
        if (alexaLoginRepo == null){
            alexaLoginRepo = new AlexaLoginRepo();
        }
        return alexaLoginRepo;
    }

    public boolean isLoggedIn(){
        if (AlexaLoginRepo.getRepo().getUsername()!=null && !AlexaLoginRepo.getRepo().getUsername().isEmpty()){
            return true;
        }
        return false;
    }
    public void setUsername(String username){
        this.username = username;
    }
    public String getUsername(){
        return this.username;
    }
    public void signOut(){
        this.setUsername(null);
    }
    public void putLinkedDeviceDetails(String ipAddress,String username){
        linkedDeviceDetails.put(ipAddress,username);
    }
    public HashMap<String,String> getLinkedDeviceDetails(){
        return linkedDeviceDetails;
    }
    public boolean isLinked(String ipAddress){
        if (getLinkedUsername(ipAddress)!=null && !getLinkedUsername(ipAddress).isEmpty()){
            return true;
        }
        return false;
    }
    public String getLinkedUsername(String ipAddress){
        return AlexaLoginRepo.getRepo().getLinkedDeviceDetails().get(ipAddress);
    }

}
