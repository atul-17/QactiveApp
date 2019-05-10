package com.libre.ManageDevice;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.libre.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SelectSourceDeviceFragment extends Fragment {

    public SelectSourceDeviceFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_source_device, container, false);
    }
}
