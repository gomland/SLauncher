package my.home.slauncher.model;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.WidgetListUpData;
import my.home.slauncher.tools.Utility;
import my.home.slauncher.view.activity.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ShinSung on 2017-09-08.
 * 종류 : 앱,위젯 정보를 미리 로드 하여둔다
 */

public class LauncherModel {
    public static final String BACKGROUND_FILE_NAME = "launcher_bg.png";

    private Context mContext;
    private Handler mMainHandler = null;

    private ArrayList<ApplicationData> mAppIconList = null;
    private ArrayList<WidgetListUpData> mWidgetItemList = null;

    private HashMap<ApplicationData, Integer> mAppData;

    private Thread mIconLoadThread = null;

    //Background bitmap 저장
    private static BitmapDrawable mBackgroundDrawable = null;
    public static BitmapDrawable getBackgroundDrawable(){
        return mBackgroundDrawable;
    }

    //최초 진입시 파일로부터 배경이미지 로드
    public static BitmapDrawable loadBackgroundFile(Context context){
        File file = new File(context.getFilesDir(), BACKGROUND_FILE_NAME);

        if(file.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if(bitmap != null){
                mBackgroundDrawable =  new BitmapDrawable(context.getResources(), bitmap);
                return mBackgroundDrawable;
            }
        }

        return null;
    }

    //배경이미지 파릴로 저장
    public static void setBackgroundDrawable(Context context, BitmapDrawable d){
        File file = new File(context.getFilesDir(), BACKGROUND_FILE_NAME);

        if(file.exists())
            file.delete();

        if(d != null) {
            FileOutputStream outputStream;
            byte[] bytes = Utility.getByteArrayFromDrawable(d);

            if(bytes != null) {
                try {
                    outputStream = context.openFileOutput(BACKGROUND_FILE_NAME, Context.MODE_PRIVATE);
                    outputStream.write(bytes);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mBackgroundDrawable = d;
        }
        else
            mBackgroundDrawable = null;

    }

    public LauncherModel(Context context, Handler handler){
        mContext = context;
        mMainHandler = handler;
        mAppIconList = new ArrayList<>();
        mWidgetItemList = new ArrayList<>();
        mAppData = new HashMap<>();
    }

    public void destory(){
        mAppIconList = null;
        mAppData = null;
        mIconLoadThread = null;
        mWidgetItemList = null;
    }

    //앱, 위젯 정보 로드
    public void load(){
        if(mAppIconList != null)
            mAppIconList.clear();

        if(mAppData != null)
            mAppData.clear();

        if(mWidgetItemList != null)
            mWidgetItemList.clear();

        ItemLoader iconLoader = new ItemLoader(mContext);
        mIconLoadThread = new Thread(iconLoader);
        mIconLoadThread.start();
    }

    public class ItemLoader implements Runnable{
        private WeakReference<Context> mContextWeakRef;

        public ItemLoader(Context context){
            mContextWeakRef = new WeakReference<>(context);
        }

        @Override
        public void run() {
            final Context context = mContextWeakRef.get();
            final PackageManager packageManager = context.getApplicationContext().getPackageManager();

            //아이콘 Drawable 확보
            final Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            final List<ResolveInfo> groupApps = context.getPackageManager().queryIntentActivities(intent, 0);

            for (int i = 0; i < groupApps.size() && mAppIconList != null; i++) {
                final ResolveInfo item = groupApps.get(i);

                final String packageName = item.activityInfo.packageName;
                final String componentName = item.activityInfo.name;
                final CharSequence iconName = item.loadLabel(packageManager);
                final Drawable iconDrawable = item.loadIcon(packageManager);

                ApplicationData data = new ApplicationData(packageName, componentName, iconName.toString(), iconDrawable);
                mAppData.put(data, i);
                mAppIconList.add(data);
            }


            //위젯을 보유한 패키지 항목 생성
            AppWidgetManager manager = AppWidgetManager.getInstance(mContext);
            ArrayList<AppWidgetProviderInfo> widgetList = (ArrayList)manager.getInstalledProviders();
            String prevPackageName = "";
            WidgetListUpData widgetData = null;
            for (int i = 0; i < widgetList.size(); i++) {
                final AppWidgetProviderInfo item = widgetList.get(i);
                final String packageName = item.provider.getPackageName();

                if(!prevPackageName.equalsIgnoreCase(packageName)){
                    widgetData = new WidgetListUpData(packageName, "widget");
                    mWidgetItemList.add(widgetData);
                    prevPackageName = packageName;
                }

                widgetData.put(item);
            }


            mMainHandler.sendEmptyMessageDelayed(MainActivity.HANDLE_LOAD_FINISH, 500);
        }
    }

    public ApplicationData getAppsData(int idx){
        return mAppIconList.get(idx);
    }

    public int getAppsCnt(){
        return mAppIconList.size();
    }

    public WidgetListUpData getWidgetData(int i){
        return mWidgetItemList.get(i);
    }

    public int getWidgetCnt(){
        return mWidgetItemList.size();
    }

    public int getAppIndexByPackageName(String packageName, String componentName){
        for(int i=0; i<mAppIconList.size(); i++){
            ApplicationData data = mAppIconList.get(i);
            if(data.getPackageName().equalsIgnoreCase(packageName) &&
                    (data.getComponentName().equalsIgnoreCase(componentName) || componentName.equalsIgnoreCase("widget")))
                return i;
        }

        return -1;
    }

    //패키지 이름을 기준으로 정보 추가
    public void addAppByPackageName(String packageName){
        final PackageManager packageManager = mContext.getApplicationContext().getPackageManager();

        //아이콘 Drawable 확보
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> groupApps = mContext.getPackageManager().queryIntentActivities(intent, 0);

        for (int i = 0; i < groupApps.size() && mAppIconList != null; i++) {
            final ResolveInfo item = groupApps.get(i);

            if(packageName.equalsIgnoreCase(item.activityInfo.packageName)) {
                final String componentName = item.activityInfo.name;
                final CharSequence iconName = item.loadLabel(packageManager);
                final Drawable iconDrawable = item.loadIcon(packageManager);

                ApplicationData data = new ApplicationData(packageName, componentName, iconName.toString(), iconDrawable);
                mAppData.put(data, i);
                mAppIconList.add(data);
            }
        }


        //위젯을 보유한 패키지 항목 생성
        AppWidgetManager manager = AppWidgetManager.getInstance(mContext);
        ArrayList<AppWidgetProviderInfo> widgetList = (ArrayList)manager.getInstalledProviders();
        String prevPackageName = "";
        WidgetListUpData widgetData = null;
        for (int i = 0; i < widgetList.size(); i++) {
            final AppWidgetProviderInfo item = widgetList.get(i);

            if(item.provider.getPackageName().equals(packageName)) {
                if (!prevPackageName.equalsIgnoreCase(packageName)) {
                    widgetData = new WidgetListUpData(packageName, "widget");
                    mWidgetItemList.add(widgetData);
                    prevPackageName = packageName;
                }

                widgetData.put(item);
            }
        }
    }

    //패키지 이름을 기준으로 정보 삭제
    public void deleteAppByPackageName(String packageName){
        for(int i=mAppIconList.size()-1; i>=0; i--){
            ApplicationData data = mAppIconList.get(i);
            if(packageName.equalsIgnoreCase(data.getPackageName()))
                mAppIconList.remove(data);

        }

        mAppData.clear();
        for(int i=0; i<mAppIconList.size(); i++){
            ApplicationData data = mAppIconList.get(i);
            mAppData.put(data, i);
        }

        for(int i=mWidgetItemList.size()-1; i>=0; i--){
            WidgetListUpData data = mWidgetItemList.get(i);
            if(data.getPackageName().equalsIgnoreCase(packageName))
                mWidgetItemList.remove(data);
        }

    }
}