package com.wonit.controleremotopc;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PcDiscoveryPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
