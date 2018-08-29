package my.home.slauncher.data;

import android.graphics.drawable.Drawable;

/**
 * Created by ShinSung on 2017-09-08.
 */

public class ApplicationData {
    private String mComponentName, mPackageName;
    private String mName;
    private Drawable mIconDrawable;

    public ApplicationData(String packageName, String componentName, String name, Drawable iconDrawable){
        mName = name;
        mIconDrawable = iconDrawable;
        mComponentName = componentName;
        mPackageName = packageName;
    }

    public String getComponentName() {
        return mComponentName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public Drawable getIconDrawable(){
        return mIconDrawable;
    }

    public String getName(){
        return mName;
    }
}