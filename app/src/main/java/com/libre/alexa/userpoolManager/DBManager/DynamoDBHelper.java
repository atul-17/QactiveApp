package com.libre.alexa.userpoolManager.DBManager;

import android.os.Bundle;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.libre.alexa.userpoolManager.AlexaListeners.DynamoDBAdditionListener;
import com.libre.alexa.userpoolManager.AlexaListeners.DynamoDBDeletionListener;
import com.libre.alexa.userpoolManager.AlexaListeners.DynamoDBUpdationListener;
import com.libre.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.libre.util.LibreLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by bhargav on 19/6/17.
 */
public class DynamoDBHelper {

    static DynamoDBHelper dynamoDBHelper;
    BasicAWSCredentials credentials;
    DynamoDBMapper mapper;

    private DynamoDBHelper(){
        credentials = new BasicAWSCredentials("AKIAI5GKKKKY7RBJ5IRA", "jwe1wwjfFttBpfxFOX+6hHTOUEPZrB4xosljbl6t");
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        ddbClient.setRegion(usWest2);
        mapper = new DynamoDBMapper(ddbClient);
    }
    public static DynamoDBHelper getInstance(){
        if (dynamoDBHelper == null){
            dynamoDBHelper = new DynamoDBHelper();
        }
        return dynamoDBHelper;
    }

    public void deleteItem(final Bundle deleteBundle, final DynamoDBDeletionListener dynamoDBDeletionListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isMaxHitReached(deleteBundle)){
                    dynamoDBDeletionListener.itemDeletionFailed(deleteBundle);
                    return;
                }
                Endpoints endpoints = new Endpoints();
                String username = deleteBundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
                endpoints.setUsername(username);
                String usn = deleteBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
                String friendlyName = deleteBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
                String description = deleteBundle.getString(AlexaConstants.BUNDLE_DESCRIPTION);
                HashSet<String> endpointDetailsArray = null;
                try {
                    endpoints = getItem(username);
                    endpointDetailsArray = getRemovedEndpointDetailsArray(usn,friendlyName,endpoints);
                    endpoints.setEndpointDetailsArray(endpointDetailsArray);
                  //  endpoints.setRefresh_token(AlexaConstants.ALEXA_REFRESH_TOKEN);
                    /*if (endpointDetailsArray == null || endpointDetailsArray.size()<=0){
                        mapper.delete(endpoints);
                    }else*/ {
                        mapper.save(endpoints);
                    }
                    dynamoDBDeletionListener.itemDeleted(deleteBundle);
                } catch (JSONException e) {
                    dynamoDBDeletionListener.itemDeletionFailed(deleteBundle);
                    e.printStackTrace();
                }catch (ConditionalCheckFailedException e) {
                    int hitCount = deleteBundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
                    hitCount++;
                    deleteBundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT,hitCount);
                    // Another process updated this item after we loaded it, so try again with the newest data
                    deleteItem(deleteBundle,dynamoDBDeletionListener);
                }


            }
        }).start();
    }
    public void updateItem(final Bundle updateBundle, final DynamoDBAdditionListener dynamoDBAccessListener){
        putItem(updateBundle,dynamoDBAccessListener);
    }
    public boolean isMaxHitReached(Bundle bundle){
        if (bundle!=null){
            int hitCount = bundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
            LibreLogger.d(this,"dynamo db hit count is "+hitCount);
            if (hitCount > AlexaConstants.HIT_COUNT_LIMIT){
                return true;
            }
        }else {
            return true;
        }
        return false;
    }
    public void putItem(final Bundle putBundle, final DynamoDBAdditionListener dynamoDBAccessListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isMaxHitReached(putBundle)){
                    dynamoDBAccessListener.onItemAdditionFailed(putBundle);
                    return;
                }
                String username = putBundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
                Endpoints endpoints = getItem(username);
                endpoints.setUsername(username);
                String usn = putBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
                String friendlyName = putBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
                String description = putBundle.getString(AlexaConstants.BUNDLE_DESCRIPTION);
                HashSet<String> endpointDetailsArray = getUpdatedEndpointDetailsArrayAfterAddition(usn,friendlyName,description,endpoints);
                endpoints.setEndpointDetailsArray(endpointDetailsArray);
                endpoints.setRefresh_token(AlexaConstants.ALEXA_REFRESH_TOKEN);
                try {
                    mapper.save(endpoints);
                    dynamoDBAccessListener.onItemAdded(putBundle);
                } catch (ConditionalCheckFailedException e) {
                    int hitCount = putBundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
                    hitCount++;
                    putBundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT,hitCount);
                    // Another process updated this item after we loaded it, so try again with the newest data
                    putItem(putBundle,dynamoDBAccessListener);
                }

            }
        }).start();

    }
    public void updateItem(final Bundle updateBundle,final DynamoDBUpdationListener dynamoDBUpdationListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isMaxHitReached(updateBundle)){
                    dynamoDBUpdationListener.itemUpdationFailed(updateBundle);
                    return;
                }
                Endpoints endpoints = new Endpoints();
                String username = updateBundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
                String endPointId = updateBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
                String friendlyName = updateBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
                endpoints = getItem(username);
                HashSet<String> endpointDetailsArray = getUpdatedEndpointDetailsArray(endPointId,friendlyName,endpoints);
                endpoints.setEndpointDetailsArray(endpointDetailsArray);
                //endpoints.setUsername(username);
               // endpoints.setRefresh_token(AlexaConstants.ALEXA_REFRESH_TOKEN);
                try {
                    mapper.save(endpoints);
                    dynamoDBUpdationListener.itemUpdated(updateBundle);
                } catch (ConditionalCheckFailedException e) {
                    int hitCount = updateBundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
                    hitCount++;
                    updateBundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT,hitCount);
                    // Another process updated this item after we loaded it, so try again with the newest data
                    updateItem(updateBundle,dynamoDBUpdationListener);
                }

            }
        }).start();
    }
    private Endpoints getItem(String username){
        Endpoints endpoints = mapper.load(Endpoints.class,username);
        if (endpoints==null){
            endpoints = new Endpoints();
        }
        return endpoints;
    }
    public boolean removeEndpointDetailsArray(String usn,String friendlyName,HashSet<String> endPointArray) throws JSONException {
        Iterator iter = endPointArray.iterator();
        while (iter.hasNext()){
            String jsonString = (String) iter.next();
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getString("friendlyName").equalsIgnoreCase(friendlyName) && jsonObject.getString("endpointId").equals(usn)){
                //remove this item
                endPointArray.remove(jsonString);
                return true;
            }
        }
        return false;
    }

    public HashSet<String> getRemovedEndpointDetailsArray(String usn,String friendlyName,Endpoints endpoints) throws JSONException {
        HashSet<String> endPointArray = new HashSet<String>();
        if (endpoints == null){
            return null;
        }
        endPointArray = endpoints.getEndpointDetailsArray();
        Iterator iter = endPointArray.iterator();
        while (iter.hasNext()){
            String jsonString = (String) iter.next();
            if (jsonString==null && jsonString.isEmpty()){
                break;
            }
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getString("friendlyName").equalsIgnoreCase(friendlyName) && jsonObject.getString("endpointId").equals(usn)){
                //remove this item
                endPointArray.remove(jsonString);
                break;
            }
        }
        if (endPointArray == null || endPointArray.size() == 0){
            return null;
        }
        return endPointArray;
    }

     public HashSet<String> getUpdatedEndpointDetailsArrayAfterAddition(String usn, String friendlyName, String description, Endpoints endpoints) {
        HashSet<String> endPointArray = new HashSet<String>();
        try {
            if (endpoints != null) {
               endPointArray = endpoints.getEndpointDetailsArray();
            }
            JSONObject endPointJsonObject = getEndpointJsonObject(usn,friendlyName,description);
                removeEndpointFromArrayIfExist(endPointJsonObject,endPointArray,endpoints);
                endPointArray.add(endPointJsonObject.toString());
            LibreLogger.d(this,"json to put to DB is "+endPointJsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return endPointArray;
    }
    public HashSet<String> getUpdatedEndpointDetailsArray(String usn, String friendlyName, Endpoints endpoints) {
        HashSet<String> endPointArray = new HashSet<String>();
        try {
            if (endpoints != null) {
                endPointArray = endpoints.getEndpointDetailsArray();
            }
            //update after getting all array
            Iterator iter = endPointArray.iterator();
            while (iter.hasNext()){
                String jsonString = (String) iter.next();
                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.getString("endpointId").equals(usn)){
                    endPointArray.remove(jsonString);
                //update this item
                    jsonObject.put("friendlyName",friendlyName);
                    endPointArray.add(jsonObject.toString());
                    break;
                }
            }

          //  LibreLogger.d(this,"json to put to DB is "+endPointJsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return endPointArray;
    }

    private void removeEndpointFromArrayIfExist(JSONObject endPointJsonObject, HashSet<String> endPointArray,Endpoints endpoints){
        try {
            String friendlyName = endPointJsonObject.getString("friendlyName");
            String usn = endPointJsonObject.getString("endpointId");
            removeEndpointDetailsArray(usn,friendlyName,endPointArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isExist(JSONObject endPointJsonObject, HashSet<String> endPointArray) {
        Iterator iter = endPointArray.iterator();
        while (iter.hasNext()) {
            String jsonString = (String) iter.next();
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(jsonString);
                String friendlyName = endPointJsonObject.getString("friendlyName");
                String usn = endPointJsonObject.getString("endpointId");
                if (jsonObject.getString("friendlyName").equals(friendlyName) && jsonObject.getString("endpointId").equals(usn)){
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private ArrayList<String> getSpeakerTypeList(){
        ArrayList<String> typeList = new ArrayList<>();
        typeList.add("Alexa.AudioPlayer");
        typeList.add("Alexa.Speaker");
        typeList.add("Alexa.PlaybackController");
        return typeList;
    }

    private JSONObject getEndpointJsonObject(String usn,String friendlyName,String description) throws JSONException{
        ArrayList<String> typeList = getSpeakerTypeList();
        JSONObject endPointJsonObject = new JSONObject();
        endPointJsonObject.put("endpointId",usn);
        endPointJsonObject.put("endpointTypeId","Alexa_Libre");
        endPointJsonObject.put("manufacturerName","3PDA");
        endPointJsonObject.put("friendlyName",friendlyName);
        endPointJsonObject.put("description",description);
        JSONObject cookiesObj = new JSONObject();
        cookiesObj.put("abc","abc");
        cookiesObj.put("def","def");
        endPointJsonObject.put("cookie",cookiesObj);
        JSONArray speakerTypeArray = new JSONArray();
        JSONObject capabilitiesObj;
        for (String interfaceType : typeList){
            capabilitiesObj = new JSONObject();
            capabilitiesObj.put("type","AlexaInterface");
            capabilitiesObj.put("interface",interfaceType);
            capabilitiesObj.put("version","1.0");
            speakerTypeArray.put(capabilitiesObj);
        }
        endPointJsonObject.put("capabilities",speakerTypeArray);

        return endPointJsonObject;

    }
}
