package com.sanstudio.musicaod;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * The full edition declares MediaListener (song title, art, progress). The Lite build leaves it out of the
 * manifest, because Play Protect blocks sideloaded apps that ask for notification access.
 */
final class Edition {
    private Edition() {
    }

    static boolean hasSongInfo(Context context) {
        try {
            context.getPackageManager().getServiceInfo(new ComponentName(context, MediaListener.class), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
