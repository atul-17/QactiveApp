package com.libre.Network;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.R;
import com.libre.SacSettingsOption;

/**
 * Created by karunakaran on 4/8/2016.
 */
public class SacInstructionsActivity extends CTDeviceDiscoveryActivity implements View.OnClickListener {
    Button btnNextForSac,pairShareConfigure;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sacinstruction);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnNextForSac = (Button)findViewById(R.id.btnNextButtonForSac);
        btnNextForSac.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnNextButtonForSac:
                startActivity(new Intent(SacInstructionsActivity.this,SacActivityScreenForLaunchWifiSettings.class));
                finish();
                break;
         /*   case R.id.sacConfigure:
                startActivity(new Intent(SacSettingsOption.this,NewSacDevicesActivity.class));
                break;*/
            default:
                break;
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SacInstructionsActivity.this, SacSettingsOption.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}
