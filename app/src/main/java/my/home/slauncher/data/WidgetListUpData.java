package my.home.slauncher.data;

import android.appwidget.AppWidgetProviderInfo;

import java.util.ArrayList;

/**
 * Created by ShinSung on 2017-09-12.
 */

public class WidgetListUpData {
    private String mPackageName, mComponentName;
    private ArrayList<AppWidgetProviderInfo> mWidgetList;

    public WidgetListUpData(String packageName, String componentName){
        mPackageName = packageName;
        mComponentName = componentName;
        mWidgetList = new ArrayList<>();
    }

    public void put(AppWidgetProviderInfo item){
        mWidgetList.add(item);
    }

    public ArrayList<AppWidgetProviderInfo> getWidgetList(){
        return mWidgetList;
    }

    public String getPackageName(){
        return mPackageName;
    }
    public String getComponentName(){
        return mComponentName;
    }
}
