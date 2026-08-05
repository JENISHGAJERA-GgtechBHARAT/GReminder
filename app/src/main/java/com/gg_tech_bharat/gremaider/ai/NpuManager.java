package com.gg_tech_bharat.gremaider.ai;

import android.content.Context;
import android.util.Log;

public class NpuManager {
    private static final String TAG = "NpuManager";

    public NpuManager(Context context) {
        Log.i(TAG, "NpuManager initialized in CPU fallback heuristic mode.");
    }

    public boolean isNpuAccelerated() {
        return false;
    }

    public Object getInterpreter() {
        return null;
    }

    public void close() {
        // No-op
    }
}
