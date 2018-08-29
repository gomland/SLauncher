package my.home.slauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import my.home.slauncher.view.activity.MainActivity;

import java.lang.ref.WeakReference;

/**
 * Created by OWNER on 2017-09-25.
 */

public class PackageReceiver extends BroadcastReceiver{
    private WeakReference<MainActivity> mActivityWeakReference = null;

    public PackageReceiver(){}

    public void init(MainActivity act){
        if(mActivityWeakReference == null)
            mActivityWeakReference = new WeakReference<>(act);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null || mActivityWeakReference == null)
            return;

        MainActivity act = mActivityWeakReference.get();
        final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        final String action = intent.getAction();

        if(action.equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED) && !replacing){
            String packageName = intent.getData().getSchemeSpecificPart();
            if(packageName != null)
                act.addApp(packageName);
        }
        else if(action.equalsIgnoreCase(Intent.ACTION_PACKAGE_REPLACED) && replacing){
            act.updateWidget();
        }
        else if(action.equalsIgnoreCase(Intent.ACTION_PACKAGE_REMOVED) && !replacing){
            String packageName = intent.getData().getSchemeSpecificPart();
            if(packageName != null)
                act.deletePackageToModel(packageName);
        }
    }
}
