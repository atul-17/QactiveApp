package com.libre;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.Scanning.Constants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.serviceinterface.LSDeviceClient;
import com.libre.util.GoogleTOSTimeZone;
import com.libre.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Gcast extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    Button learn_cast, how_cast, gcast_group,gcast_learn_privacy;
    TextView legalDocText;
    Spinner mSpinner;
    ArrayAdapter idAdapter;
    String ipAddress;
    TextView viewById ;
    private CheckBox checkBox;
    private final String GCAST_PRIVACY_POLICY = "https://support.google.com/chromecast/answer/6076570";
    private final  String gcast_group_link =  "https://support.google.com/googlecast/answer/6372909?hl=en&ref_topic=6372841";
    private final String private_google_link = "https://support.google.com/googlecast/answer/6371336?hl=en&ref_topic=6369825";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcast);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        how_cast = (Button) findViewById(R.id.how_cast);
        how_cast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Gcast.this, LibreWebViewActivity.class);
                intent.putExtra("url", "https://www.google.com/cast/audio/learn");
                startActivity(intent);
            }
        });


        gcast_group = (Button) findViewById(R.id.gcast_group);
        gcast_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Gcast.this, LibreWebViewActivity.class);
                intent.putExtra("url", gcast_group_link);
                startActivity(intent);
            }
        });


        legalDocText = (TextView) findViewById(R.id.legalDocText);
        SpannableString content = new SpannableString(getString(R.string.legal_documents1));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        legalDocText.setText(content);
        legalDocText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://support.google.com/googlecast/answer/6121012"));
                startActivity(intent);
            }
        });

        gcast_learn_privacy = (Button) findViewById(R.id.learn_gcast_privacy);
        gcast_learn_privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse(GCAST_PRIVACY_POLICY));
                startActivity(intent);

            }
        });


        checkBox = (CheckBox) findViewById(R.id.usage);

        ipAddress = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);

        GoogleTOSTimeZone googleTOSTimeZone = LibreApplication.GOOGLE_TIMEZONE_MAP.get(ipAddress);

       /* commented if (googleTOSTimeZone==null)
        {
            Toast.makeText(this,getString(R.string.tosNotAvailable),Toast.LENGTH_LONG).show();
            finish();
        }*/
       // setTheTOSANDTimeZone(googleTOSTimeZone);


        LSSDPNodeDB db = LSSDPNodeDB.getInstance();
        LSSDPNodes theNodeBasedOnTheIpAddress = db.getTheNodeBasedOnTheIpAddress(ipAddress);

        viewById = (TextView) findViewById(R.id.version_number);
//        TextView ipAddressText = (TextView) findViewById(R.id.ipaddress);

        if (theNodeBasedOnTheIpAddress != null) {
            /*if(theNodeBasedOnTheIpAddress.getgCastVersionFromEureka() == null){
                getEurekaBuildVersion();
                mHandlerForShowingLoader.sendEmptyMessage(Constants.INITIATED_TO_FETCH_GCASTVERSION);
            }else {
                viewById.setText(theNodeBasedOnTheIpAddress.getgCastVersionFromEureka());
            }*/ getEurekaBuildVersion();
            mHandlerForShowingLoader.sendEmptyMessage(Constants.INITIATED_TO_FETCH_GCASTVERSION);
            mHandlerForShowingLoader.sendEmptyMessageDelayed(Constants.FAIL_TO_FETCH_GCASTVERSION,60000);
//            ipAddressText.setText(theNodeBasedOnTheIpAddress.getIP());

        }


    }
    private void parseJson(String mJson) {

        try {

            boolean mTosAccepted=false;
            boolean mShareAccepted=false;

            JSONObject jsonRootObject = new JSONObject(mJson);
            //Get the instance of JSONArray that contains JSONObjects
            /*Cast Build Vversion */
            String mCastBuild = jsonRootObject.getString("cast_build_revision");
            LSSDPNodeDB.getInstance()
                    .getTheNodeBasedOnTheIpAddress(ipAddress)
                    .setgCastVersionFromEureka(mCastBuild);
            viewById.setText(mCastBuild);


            mTosAccepted = jsonRootObject.optBoolean("tos_accepted");
            JSONObject mJsonObject = jsonRootObject.optJSONObject("opt_in");
            mShareAccepted = mJsonObject.optBoolean("stats");


            GoogleTOSTimeZone googleTOSTimeZone = LibreApplication.GOOGLE_TIMEZONE_MAP.get(ipAddress);
            String mHostName=  ipAddress;
            try {
                String mTimeZone = jsonRootObject.getString("timezone");
                if (mTimeZone != null) {
                    googleTOSTimeZone.setTimezone(mTimeZone);
                }else{
                    LSSDPNodes mNodeData = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(ipAddress);
                    if(mNodeData!= null && ( mNodeData.getmTimeZone()!=null &&
                            !mNodeData.getmTimeZone().equalsIgnoreCase(""))){
                        googleTOSTimeZone.setTimezone(mNodeData.getmTimeZone());
                    }else
                        googleTOSTimeZone.setTimezone(getResources().getString(R.string.notimezoneselected));
                }
            }catch(Exception e){
                LSSDPNodes mNodeData = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(ipAddress);
                if(mNodeData!= null && ( mNodeData.getmTimeZone()!=null &&
                        !mNodeData.getmTimeZone().isEmpty())){
                    googleTOSTimeZone.setTimezone(mNodeData.getmTimeZone());
                }else
                    googleTOSTimeZone.setTimezone(getResources().getString(R.string.notimezoneselected));
            }
            String msg = "";
            if(!mTosAccepted && mShareAccepted){
                googleTOSTimeZone.setTosANDShare("3");
            }else if(!mTosAccepted && !mShareAccepted){
                googleTOSTimeZone.setTosANDShare("1");
            }else if(mTosAccepted && !mShareAccepted){
                googleTOSTimeZone.setTosANDShare("1");
            }else if(mTosAccepted && mShareAccepted){
                googleTOSTimeZone.setTosANDShare("3");
            }
            LibreApplication.GOOGLE_TIMEZONE_MAP.put(mHostName, googleTOSTimeZone);
            setTheTOSANDTimeZone(googleTOSTimeZone);
            LibreLogger.d(this,"Message Recieved for Gcast 226 is " + msg);
            /* Modified by Praveen to send the request for listening to 226 in the background */

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    private void getEurekaBuildVersion(){
        final String BASE_URL = "http://" + ipAddress+":8008";
        LSDeviceClient lsDeviceClient = new LSDeviceClient(BASE_URL);
        LSDeviceClient.DeviceNameService GcastBuildVersion = lsDeviceClient.getDeviceNameService();

        GcastBuildVersion.getTOSData(new Callback<Object>() {
            @Override
            public void success(Object mScanResult, Response response) {

                /* retry if the ScanResult is null*/
                if (mScanResult == null) {
                    closeLoader();
                    return;
                }

                //JSONObject mJsonObject = new JSONObject(mScanResult.toString());
                parseJson(mScanResult.toString());

                mHandlerForShowingLoader.sendEmptyMessage(Constants.SUCCESS_FOR_FETCH_GCASTVERSION);

            }

            @Override
            public void failure(RetrofitError error) {
                LibreLogger.d(this, "Eurkea Got FAilure");
                if(mHandlerForShowingLoader.hasMessages(Constants.FAIL_TO_FETCH_GCASTVERSION))
                    mHandlerForShowingLoader.removeMessages(Constants.FAIL_TO_FETCH_GCASTVERSION);
               /* *//*When Eureka Got Failed Have to Retry it with the delay of 2s*/
                try {
                    Thread.sleep(2000);
                    getEurekaBuildVersion();
                    mHandlerForShowingLoader.sendEmptyMessageDelayed(Constants.FAIL_TO_FETCH_GCASTVERSION, 60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public CompoundButton.OnCheckedChangeListener checkBoxListner =new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
            LibreLogger.d(this, "onCheckedChanged() returned: ");
            /* hash map should be read  */
            GoogleTOSTimeZone googleTOSTimeZone = LibreApplication.GOOGLE_TIMEZONE_MAP.get(ipAddress);


            LUCIControl luciControl = new LUCIControl(ipAddress);
            if (enabled && googleTOSTimeZone.isTOSAccepted()) {
                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "3:" + googleTOSTimeZone.getTimezone(), LSSDPCONST.LUCI_SET);
            } else if (enabled & googleTOSTimeZone.isTOSAccepted() == false) {
                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "2:" + googleTOSTimeZone.getTimezone(), LSSDPCONST.LUCI_SET);
            } else if (enabled == false & googleTOSTimeZone.isTOSAccepted() == true) {
                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:" + googleTOSTimeZone.getTimezone(), LSSDPCONST.LUCI_SET);
            } else if (enabled == false & googleTOSTimeZone.isTOSAccepted() == false) {
                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "0:" + googleTOSTimeZone.getTimezone(), LSSDPCONST.LUCI_SET);
            }

        }


    };


    private void showAlertWithMessage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(Gcast.this);
        builder.setMessage(getString(R.string.GcastSupport))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setPositiveButton(getString(R.string.launchGcast), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                launchTheApp("com.google.android.apps.chromecast.app");
            }
        });


        AlertDialog googleAler = builder.create();
        googleAler.show();





    }

    public String getVersion(Context context) {
        String Version = getString(R.string.title_activity_welcome);
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);

        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (pInfo != null)
            Version = pInfo.versionName;

        return Version;
    }


    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }


    public void SendLoagcatMail() {

        // save logcat in file
        File outputFile = new File(Environment.getExternalStorageDirectory(),
                "logcat.txt");
        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Uri path = Uri.fromFile(outputFile);

        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_VIEW);
        String to[] = {"libreappsupport@libre.com"};
        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        emailIntent.setType("plain/text");
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        // the mail subject
        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Libre App Feedback");

        LSSDPNodeDB db = LSSDPNodeDB.getInstance();
        LSSDPNodes theNodeBasedOnTheIpAddress = db.getTheNodeBasedOnTheIpAddress(ipAddress);
        if(theNodeBasedOnTheIpAddress!=null)
            emailIntent .putExtra(Intent.EXTRA_TEXT, "Cast version- "+theNodeBasedOnTheIpAddress.getgCastVerision()
                    +"\n"+"App version- "+getVersion(Gcast.this)+"\nPhone details- "+getDeviceName());
        startActivity(emailIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gcast_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.privacy_google_text) {
            Intent intent = new Intent(Gcast.this,LibreWebViewActivity.class);
            intent.putExtra("url",private_google_link);
            startActivity(intent);

        }

        if (id == R.id.feedback) {
            try {
                SendLoagcatMail();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }



        if (id == R.id.open_source_license) {
            Intent intent = new Intent(Gcast.this,LibreWebViewActivity.class);
            intent.putExtra("url","https://support.google.com/googlecast/answer/6121012");
            startActivity(intent);
        }

        if (id == R.id.google_privcay_policy) {
            Intent intent = new Intent(Gcast.this,LibreWebViewActivity.class);
            intent.putExtra("url", "https://www.google.com/intl/en/policies/privacy/");
            startActivity(intent);
        }

        if (id == R.id.google_terms_of_policy) {
            Intent intent = new Intent(Gcast.this,LibreWebViewActivity.class);
            intent.putExtra("url", "https://www.google.com/intl/en/policies/terms/");
            startActivity(intent);
        }


        return super.onOptionsItemSelected(item);

    }
    private ProgressDialog m_progressDlg;
    Handler mHandlerForShowingLoader = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what== Constants.INITIATED_TO_FETCH_GCASTVERSION){
                showLoader();

            }else if(msg.what == Constants.SUCCESS_FOR_FETCH_GCASTVERSION){
                if(mHandlerForShowingLoader.hasMessages(Constants.FAIL_TO_FETCH_GCASTVERSION))
                    mHandlerForShowingLoader.removeMessages(Constants.FAIL_TO_FETCH_GCASTVERSION);
                LSSDPNodeDB db = LSSDPNodeDB.getInstance();
                LSSDPNodes theNodeBasedOnTheIpAddress = db.getTheNodeBasedOnTheIpAddress(ipAddress);
                viewById.setText(theNodeBasedOnTheIpAddress.getgCastVersionFromEureka());
                closeLoader();
            }else if(msg.what == Constants.FAIL_TO_FETCH_GCASTVERSION){
                closeLoader();
                Toast.makeText(getApplicationContext(),"60s Timeout For Fetching Eureka information",Toast.LENGTH_SHORT).show();
            }
        }
    };
    private void showLoader() {

        if (Gcast.this.isFinishing())
            return;

        if (m_progressDlg == null) {
            m_progressDlg = new ProgressDialog(Gcast.this);
            m_progressDlg.setMessage(getString(R.string.loading));
            m_progressDlg.setCancelable(false);
            m_progressDlg.show();
        }

        if (!m_progressDlg.isShowing()) {
            m_progressDlg.show();
        }
    }

    private void closeLoader() {

        if (m_progressDlg != null) {

            if (m_progressDlg.isShowing() == true) {
                m_progressDlg.dismiss();
            }

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if(mHandlerForShowingLoader.hasMessages(Constants.FAIL_TO_FETCH_GCASTVERSION))
                mHandlerForShowingLoader.removeMessages(Constants.FAIL_TO_FETCH_GCASTVERSION);
        }catch(Exception e){

        }
    }

    private static String displayTimeZone(TimeZone tz) {

        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset())
                - TimeUnit.HOURS.toMinutes(hours);
        // avoid -4:-30 issue
        minutes = Math.abs(minutes);

        String result = "";
        if (hours > 0) {
            result = String.format("(GMT+%d:%02d) %s", hours, minutes, tz.getID());
        } else {
            result = String.format("(GMT%d:%02d) %s", hours, minutes, tz.getID());
        }

        return result;

    }

    private void displayIds(){
        String[] ids=TimeZone.getAvailableIDs();

       for(int i=0;i<ids.length;i++)
        {
            System.out.println("Availalbe ids.................."+ids[i]);
            TimeZone d= TimeZone.getTimeZone(ids[i]);
            System.out.println("time zone."+d.getDisplayName());
            System.out.println("savings."+d.getDSTSavings());
            System.out.println("offset."+d.getRawOffset());

            /////////////////////////////////////////////////////
            if (!ids[i].matches(".*//*.*")) {
                continue;
            }

            String region = ids[i].replaceAll(".*//*", "").replaceAll("_", " ");
            int hours = Math.abs(d.getRawOffset()) / 3600000;
            int minutes = Math.abs(d.getRawOffset() / 60000) % 60;
            String sign = d.getRawOffset() >= 0 ? "+" : "-";

            String timeZonePretty = String.format("(UTC %s %02d:%02d) %s", sign, hours, minutes, region);
            System.out.println("KarunaKarana" + timeZonePretty);
            //////////////////////////////////////////////////////////////////
        }
    }
    public static String getCurrentTimezoneOffset(TimeZone tz) {
/*
        TimeZone tz = TimeZone.getDefault();*/
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());
      /*  int offsetInMillis = tz.getRawOffset();*/
        String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = (offsetInMillis >= 0 ? "+" : "-") + offset;

        return offset;
    }
    public void populateAndUpdateTimeZone(String currentTimeZoneString) {
      //  displayIds();
        mSpinner = (Spinner) findViewById(R.id.timezonespinner);

        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<CharSequence>(Gcast.this, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        ArrayList<String> timeZoneAsStringArray = new ArrayList<String>();
        final List<TimeZone> sortedTimeZoneArray = new ArrayList<>();
        List<TimeZone> timeZoneRawArray = new ArrayList<>();
        final String[] AvailabelTimeZoneIds = TimeZone.getAvailableIDs();

        timeZoneAsStringArray.add(getResources().getString(R.string.notimezoneselected));

        for (String id : AvailabelTimeZoneIds) {
//            TZ1.add(displayTimeZone(TimeZone.getTimeZone(id)));
            LibreLogger.d(this, "KAruna ID " + TimeZone.getTimeZone(id).getID() + " TimeZone Id " +
                     getCurrentTimezoneOffset(TimeZone.getTimeZone(id)) );
            timeZoneRawArray.add(TimeZone.getTimeZone(id));
        }


        Collections.sort(timeZoneRawArray,
                new Comparator<TimeZone>() {
                    public int compare(TimeZone s1, TimeZone s2) {
                        Calendar cal1 = GregorianCalendar.getInstance(s1);
                        Calendar cal2 = GregorianCalendar.getInstance(s2);
                        return s1.getOffset(cal1.getTimeInMillis()) - s2.getOffset(cal2.getTimeInMillis());
                    }
                }); // Need to sort the GMT timezone here after getTimeZone() method call
        for (TimeZone instance : timeZoneRawArray) {
            LibreLogger.d(this, getTimeZone(instance) + instance.getID());
            timeZoneAsStringArray.add(getTimeZone(instance));
            sortedTimeZoneArray.add(instance);

        }

        for (int i = 0; i < timeZoneAsStringArray.size(); i++) {
            adapter.add(timeZoneAsStringArray.get(i));
        }

//        final Spinner TZone = (Spinner)findViewById(R.id.TimeZoneEntry);
        mSpinner.setAdapter(adapter);

        // now set the spinner to default timezone from the time zone settings
        for (int i = 0; i < adapter.getCount(); i++) {
            if (timeZoneAsStringArray.get(i).contains(currentTimeZoneString)/*|| sortedTimeZoneArray.get(i).getID().equals(TimeZone.getDefault().getID())*/ ) {
//            if (TZ[i].equals(TimeZone.getDefault().getID())) {
                mSpinner.setSelection(i);
            }
        }




        final AdapterView.OnItemSelectedListener spinerListne=  new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if((position-1)<0){
                    Toast.makeText(getApplicationContext(),"Select the valid Timezone",Toast.LENGTH_SHORT).show();
                    return;
                }
                LibreLogger.d("", "selected postion " + position + " " + sortedTimeZoneArray.get(position-1).getID());
                LUCIControl favLuci = new LUCIControl(ipAddress);
                GoogleTOSTimeZone googleTOSTimeZone = LibreApplication.GOOGLE_TIMEZONE_MAP.get(ipAddress);

                if (googleTOSTimeZone.isTOSAccepted()&&googleTOSTimeZone.isShareAccepted())
                    favLuci.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "3:" + sortedTimeZoneArray.get(position-1).getID(), LSSDPCONST.LUCI_SET);
                else if (googleTOSTimeZone.isTOSAccepted()==true&&googleTOSTimeZone.isShareAccepted()==false)
                    favLuci.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:" + sortedTimeZoneArray.get(position-1).getID(), LSSDPCONST.LUCI_SET);
                else if (googleTOSTimeZone.isTOSAccepted()==false&&googleTOSTimeZone.isShareAccepted()==true)
                    favLuci.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "2:" + sortedTimeZoneArray.get(position-1).getID(), LSSDPCONST.LUCI_SET);
                else if (googleTOSTimeZone.isTOSAccepted()==false&&googleTOSTimeZone.isShareAccepted()==false)
                    favLuci.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "0:" + sortedTimeZoneArray.get(position-1).getID(), LSSDPCONST.LUCI_SET);


            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };


        mSpinner.post(new Runnable() {
            public void run() {
                mSpinner.setOnItemSelectedListener(spinerListne);
            }
        });

    }





    /* private void populateAndUpdateTimeZone() {

         //populate spinner with all timezones
         mSpinner = (Spinner) findViewById(R.id.timezonespinner);
         final String[] idArray = TimeZone.getAvailableIDs();
         idAdapter = new ArrayAdapter<String>(Gcast.this, android.R.layout.simple_spinner_dropdown_item,idArray);
         idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         mSpinner.setAdapter(idAdapter);

         // now set the spinner to default timezone from the time zone settings
         for (int i = 0; i < idAdapter.getCount(); i++) {
             if (idAdapter.getItem(i).equals(TimeZone.getDefault().getID())) {
                 mSpinner.setSelection(i);
             }
         }

         mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
             public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                 // your code here

                 LibreLogger.d(this, "selected postion " + position + " " + idArray[position]);
                 String selected = idArray[position];
                 LUCIControl favLuci = new LUCIControl(ipAddress);
                 favLuci.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "2:" + selected, LSSDPCONST.LUCI_SET);
             }

             @Override
             public void onNothingSelected(AdapterView<?> parentView) {
                 // your code here
             }

         });
     }
 */
    private static String getTimeZone(TimeZone tz) {
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());
        long hours = TimeUnit.MILLISECONDS.toHours(offsetInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(offsetInMillis)
                - TimeUnit.HOURS.toMinutes(hours);
        minutes = Math.abs(minutes);
        String result = "";
        if (hours > 0) {
            result = String.format("(GMT+%d:%02d) %s", hours, minutes, tz.getID());
        } else if (hours < 0) {
            result = String.format("(GMT%d:%02d) %s", hours, minutes, tz.getID());
        } else {
            result = String.format("(GMT) %s", tz.getID());
        }
        return result;

    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData dataRecived) {


        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        if (packet.getCommand() == MIDCONST.GCAST_TOS_SHARE_COMMAND) {

            /**remove from UI */
            LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
            final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(dataRecived.getRemotedeviceIp());

            String msg = new String(packet.getpayload());
            LibreLogger.d(this,"Message Recieved for Gcast 226 is " + msg);
                    /* Modified by Praveen to send the request for listening to 226 in the background */
            if (msg != null) {

                GoogleTOSTimeZone googleTOSTimeZone=new GoogleTOSTimeZone(msg);
                setTheTOSANDTimeZone(googleTOSTimeZone);

            }
        }
    }

    private void setTheTOSANDTimeZone(GoogleTOSTimeZone googleTOSTimeZone) {

        if (googleTOSTimeZone!=null && googleTOSTimeZone.isShareAccepted()) {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(true);
            checkBox.setOnCheckedChangeListener(checkBoxListner);
        }else
        {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(false);
            checkBox.setOnCheckedChangeListener(checkBoxListner);
        }

       if(googleTOSTimeZone!=null)
        /* Seting the Timezone */
        populateAndUpdateTimeZone(googleTOSTimeZone.getTimezone());

    }

    public void launchTheApp(String appPackageName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } else {

            redirectingToPlayStore(intent, appPackageName);

        }

    }


    public void redirectingToPlayStore(Intent intent, String appPackageName) {

        try {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + appPackageName));
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException anfe) {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
            startActivity(intent);

        }

    }



}
