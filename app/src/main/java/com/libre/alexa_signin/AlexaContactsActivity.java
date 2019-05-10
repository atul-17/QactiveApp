package com.libre.alexa_signin;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

public class AlexaContactsActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner, View.OnClickListener {
//    private static final int PICK_CONTACT_REQUEST = 1001;
//    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1002;
    private EditText homeContact,workContact, voiceMailContact;
    private Button save,cancel;
//    private ImageView workContactsBookIv,homeContactsBookIv,xyzContactsBookIv;
//    private int contactRequestType = 0;
//    private static final int CONTACT_REQUEST_WORK = 11;
//    private static final int CONTACT_REQUEST_HOME = 12;
//    private static final int CONTACT_REQUEST_XYZ = 13;
    private ProgressDialog loader;
    private String currentDeviceIp;
    private final int NETWORK_TIMEOUT = 8001;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NETWORK_TIMEOUT:
                    LibreLogger.d(this, "handler message recieved");
                    closeLoader();
                    /*showing error*/
                    LibreError error = new LibreError(currentDeviceIp, getString(R.string.requestTimeout));
                    showErrorMessage(error);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alexa_contacts);

        homeContact = (EditText) findViewById(R.id.homeContactEt);
        workContact = (EditText) findViewById(R.id.workContactEt);
        voiceMailContact = (EditText) findViewById(R.id.xyzContactEt);
        save = (Button) findViewById(R.id.save);
        cancel = (Button) findViewById(R.id.cancel);

//        workContactsBookIv = (ImageView) findViewById(R.id.workContactSelect);
//        homeContactsBookIv = (ImageView) findViewById(R.id.homeContactSelect);
//        xyzContactsBookIv = (ImageView) findViewById(R.id.xyzContactSelect);

        save.setOnClickListener(this);
        cancel.setOnClickListener(this);
//        homeContactsBookIv.setOnClickListener(this);
//        workContactsBookIv.setOnClickListener(this);
//        xyzContactsBookIv.setOnClickListener(this);

        setSupportActionBar((android.support.v7.widget.Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        currentDeviceIp = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);

        /*calling this here since going to contacts book of Android OS sends this activity to onstop and
        * hence calls onactivityresult and then onStart*/
        registerForDeviceEvents(this);
        new LUCIControl(currentDeviceIp).SendCommand(MIDCONST.MID_ENV_READ, LUCIMESSAGES.READ_CONTACT_NUMBERS,LSSDPCONST.LUCI_GET);
        showLoader("Loading device contacts..");

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
        byte[] buffer = dataRecived.getMessage();
        String ipaddressRecieved = dataRecived.getRemotedeviceIp();

        LibreLogger.d(this, "Message recieved for ipaddress " + ipaddressRecieved);

        if (currentDeviceIp.equalsIgnoreCase(ipaddressRecieved)) {
            LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
            switch (packet.getCommand()) {

                case MIDCONST.MID_ALEXA_CONTACTS: {
                    closeLoader();
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 235 recieved  " + message);
                    if (message.isEmpty())
                        onBackPressed();
                    break;
                }
                case MIDCONST.MID_ENV_READ: {
                    closeLoader();
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 208 recieved  " + message);
                    parseMsg(message);
                    break;
                }
            }
        }
    }

    private void parseMsg(String message) {
        if (message!=null && !message.isEmpty()){
            /*Format == contact_numbers:1:+918088719996,2:958859,3:,*/
            /*In string split, after split data is empty.. it will not be considered*/
            String[] msgPartsByComma = message.split(",");
            if (msgPartsByComma.length>2){
                String[] firstContactPartsBySingleColon = msgPartsByComma[0].split(":");

                if (firstContactPartsBySingleColon.length>2) {
                    homeContact.setText(firstContactPartsBySingleColon[2]);
                }

                String[] secondContactPartsBySingleColon = msgPartsByComma[1].split(":");
                if (secondContactPartsBySingleColon.length>1) {
                    workContact.setText(secondContactPartsBySingleColon[1]);
                }

                String[] thirdContactPartsBySingleColon = msgPartsByComma[2].split(":");
                if (thirdContactPartsBySingleColon.length>1) {
                    voiceMailContact.setText(thirdContactPartsBySingleColon[1]);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save:

                if (homeContact.getText().toString().isEmpty() &&
                        workContact.getText().toString().isEmpty() &&
                        voiceMailContact.getText().toString().isEmpty()){
                    Toast.makeText(this, "No number added", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isAllNumbersValid()) {
                    new LUCIControl(currentDeviceIp).SendCommand(MIDCONST.MID_ALEXA_CONTACTS, getContactsStrng(), LSSDPCONST.LUCI_SET);
                    showLoader("Updating contacts..");
                }
                break;
            case R.id.cancel:
                finish();
                break;
//            case R.id.homeContactSelect:
//                contactRequestType = CONTACT_REQUEST_HOME;
//                checkContactPermission();
//                break;
//            case R.id.workContactSelect:
//                contactRequestType = CONTACT_REQUEST_WORK;
//                checkContactPermission();
//                break;
//            case R.id.xyzContactSelect:
//                contactRequestType = CONTACT_REQUEST_XYZ;
//                checkContactPermission();
//                break;
        }
    }

    private void showLoader(String loaderMsg) {
        if (!isFinishing()) {
            loader = new ProgressDialog(AlexaContactsActivity.this);
            loader.setMessage(loaderMsg);
            loader.setCancelable(true);
            loader.setCanceledOnTouchOutside(false);
            loader.show();
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT,Constants.INTERNET_PLAY_TIMEOUT);
        }
    }

    private void closeLoader(){
        handler.removeMessages(NETWORK_TIMEOUT);
        if (loader!=null && loader.isShowing()){
            loader.dismiss();
            loader = null;
        }
    }

    private String getContactsStrng() {
        return  "1:"+homeContact.getText().toString()+","+
                "2:"+workContact.getText().toString()+","+
                "3:"+voiceMailContact.getText().toString()+",";
    }

//    private void checkContactPermission(){
//        closeKeyboard();
//
//        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
//            if (isContactsReadPermissionGranted()){
//             launchContactPicker();
//            } else {
//                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
////                    showAlertDialogForContactsPermissionRequired();
//                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
//                            MY_PERMISSIONS_REQUEST_READ_CONTACTS);
//                } else {
//                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
//                            MY_PERMISSIONS_REQUEST_READ_CONTACTS);
//                }
//            }
//            /*else {
//                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
//                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
//            }*/
//        } else {
//            launchContactPicker();
//        }
//    }

//    private void showAlertDialogForContactsPermissionRequired() {
//        AlertDialog.Builder requestPermission = new AlertDialog.Builder(AlexaContactsActivity.this);
//        requestPermission.setTitle(getString(R.string.permitNotAvailable))
//                .setMessage(getString(R.string.enableContactsPermit))
//                .setPositiveButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        //navigate to settings
//                        alertDialog1.dismiss();
//                        Intent intent = new Intent();
//                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                        Uri uri = Uri.fromParts("package", getPackageName(), null);
//                        intent.setData(uri);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                        startActivity(intent);
//                    }
//                })
//                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        alertDialog1.dismiss();
//                    }
//                })
//                .setCancelable(false);
//        if (alertDialog1 == null) {
//            alertDialog1 = requestPermission.create();
//        }
//        if (alertDialog1 != null && !alertDialog1.isShowing())
//            alertDialog1.show();
//    }

//    private void launchContactPicker(){
//        try {
//            Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
//            pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
//            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }



//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_CONTACT_REQUEST) {
//            if (resultCode == RESULT_OK) {
//                Uri contactUri = data.getData();
//                // We only need the NUMBER column, because there will be only one row in the result
//                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
//
//                Cursor cursor = getContentResolver()
//                        .query(contactUri, projection, null, null, null);
//                cursor.moveToFirst();
//
//                // Retrieve the phone number from the NUMBER column
//                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//                String number = cursor.getString(column)/*.replaceAll("[-() ]", "")*/;
//
//                if (isValidPhoneNumber(number)){
//                    switch (contactRequestType){
//                        case CONTACT_REQUEST_HOME:
//                            homeContact.setText(number);
//                            break;
//                        case CONTACT_REQUEST_WORK:
//                            workContact.setText(number);
//                            break;
//                        case CONTACT_REQUEST_XYZ:
//                            voiceMailContact.setText(number);
//                            break;
//                    }
//                } else {
//                    Toast.makeText(AlexaContactsActivity.this, "Invalid Phone number", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//    }

    private boolean isAllNumbersValid(){
        if (!isValidPhoneNumber(homeContact.getText().toString())){
            homeContact.setError("Invalid phone number");
            homeContact.requestFocus();
            return false;
        } else if (!isValidPhoneNumber(workContact.getText().toString())){
            workContact.setError("Invalid phone number");
            workContact.requestFocus();
            return false;
        } else if (!isValidPhoneNumber(voiceMailContact.getText().toString())){
            voiceMailContact.setError("Invalid phone number");
            voiceMailContact.requestFocus();
            return false;
        } else {
            homeContact.setError(null);
            workContact.setError(null);
            voiceMailContact.setError(null);
            return true;
        }
    }

    private boolean isValidPhoneNumber(String number) {
        if (number.isEmpty())
            return true;
        else
            return !(number.length() < 6 || number.length() > 20) && android.util.Patterns.PHONE.matcher(number).matches();
    }

//    private void closeKeyboard(){
//        InputMethodManager inputManager = (InputMethodManager)
//                getSystemService(Context.INPUT_METHOD_SERVICE);
//
//        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(),
//                InputMethodManager.HIDE_NOT_ALWAYS);
//    }

//    @TargetApi(Build.VERSION_CODES.M)
//    private boolean isContactsReadPermissionGranted(){
//        boolean granted = false;
//        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
//            granted = true;
//        }
//        return granted;
//    }

//    @TargetApi(Build.VERSION_CODES.M)
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    launchContactPicker();
//                } else {
//                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
//                        /*requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
//                                MY_PERMISSIONS_REQUEST_READ_CONTACTS);*/
//                    } else {
//                        showAlertDialogForContactsPermissionRequired();
//                    }
//                }
//                return;
//            }
//        }
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        unRegisterForDeviceEvents();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterForDeviceEvents();
        handler.removeMessages(NETWORK_TIMEOUT);
    }

}
