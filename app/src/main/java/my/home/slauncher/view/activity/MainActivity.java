package my.home.slauncher.view.activity;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

import my.home.slauncher.R;
import my.home.slauncher.controller.LauncherController;
import my.home.slauncher.data.IconData;
import my.home.slauncher.database.LauncherDBHelper;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.receiver.PackageReceiver;
import my.home.slauncher.view.widget.CustomAppWidgetHost;

public class MainActivity extends Activity {
    private LauncherController mLauncherController;
    private LauncherModel mLauncherModel;

    private IconData mWaitWidgetIconData = null; //위젯 추가시 백업 데이터

    private static final int APPWIDGET_HOST_ID = 2025;
    private static CustomAppWidgetHost mAppWidgetHost;
    public static CustomAppWidgetHost getAppWidgetHost(){ //Host 객체 공유
        return mAppWidgetHost;
    }

    //위젯 Request status
    private static final int REQUEST_BIND_APPWIDGET = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 2;
    private static final int REQUEST_WALLPAPER = 3;

    //핸들러 이벤트
    public static final int HANDLE_LONG_TOUCH = 0;
    public static final int HANDLE_LOAD_FINISH = 1;
    public static final int HANDLE_PICK_APPS_ITEM = 2;
    public static final int HANDLE_PICK_WIDGET_ITEM = 3;
    public static final int HANDLE_PICK_WIDGET_CREATE = 4;
    public static final int HANDLE_PAGE_EFFECT_ZOOM_OUT = 5;
    public static final int HANDLE_PAGE_EFFECT_ZOOM_IN= 6;
    public static final int HANDLE_CACHE_PICK_ITEM = 7;
    public static final int HANDLE_RUN_ACTIVITY = 8;
    public static final int HANDLE_RUN_SETTING_ACTIVITY = 9;
    public static final int HANDLE_RUN_WALLPAPER_ACTIVITY = 10;
    public static final int HANDLE_DELETE_APP = 11;
    public static final int HANDLE_SET_SCREEN_MAIN = 12;
    private MainHandler mMainHandler;


    private PackageReceiver mPackageReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager mgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        mgr.getDefaultDisplay().getMetrics(metrics);

        int screenSize = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE)
            Log.d(getClass().toString(), "screen info size : SCREENLAYOUT_SIZE_LARGE, dpi : " + metrics.densityDpi);
        else if(screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL)
            Log.d(getClass().toString(), "screen info size : SCREENLAYOUT_SIZE_NORMAL, dpi : "  + metrics.densityDpi);
        else if(screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL)
            Log.d(getClass().toString(), "screen info size : SCREENLAYOUT_SIZE_SMALL, dpi : " + metrics.densityDpi);
        else if(screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            Log.d(getClass().toString(), "screen info size : SCREENLAYOUT_SIZE_XLARGE, dpi : " + metrics.densityDpi);
        else if(screenSize == Configuration.SCREENLAYOUT_SIZE_UNDEFINED)
            Log.d(getClass().toString(), "screen info size : SCREENLAYOUT_SIZE_UNDEFINED, dpi : " + metrics.densityDpi);


        LauncherDBHelper.create(getApplicationContext());
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mPackageReceiver != null)
            unregisterReceiver(mPackageReceiver);

        if(mAppWidgetHost != null)
            mAppWidgetHost.stopListening();

        LauncherDBHelper.inst().close();
        mLauncherModel.destory();
        mLauncherModel = null;
        mMainHandler = null;
        mAppWidgetHost = null;
        mPackageReceiver = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWidget();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        mLauncherController.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CREATE_APPWIDGET) {//위젯이 생성이 허락되었다면
            if (resultCode == RESULT_OK)
                createWidget(data);
        }
        else if (requestCode == REQUEST_BIND_APPWIDGET){//위젯 생성 요청
            if (resultCode == RESULT_OK)
                configureWidget(data);
            else { //허용을 하지 않았다면 지운다.
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1)
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                mWaitWidgetIconData = null;
            }
        }
        else if(requestCode == REQUEST_WALLPAPER){
            BitmapDrawable bitmapDrawable = LauncherModel.getBackgroundDrawable();

            if(bitmapDrawable != null)
                mLauncherController.setBackgroundImage(bitmapDrawable);
            else
                mLauncherController.clearBackgroundImage();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    private void init(){
        initData();
        createBR();
    }

    //초기화
    private void initData() {
        mLauncherController = findViewById(R.id.layout_touch_controller);
        mMainHandler = new MainHandler(this, mLauncherController);
        mLauncherModel = new LauncherModel(getApplicationContext(), mMainHandler);

        mLauncherController.init(mLauncherModel, mMainHandler);
        mLauncherModel.load();

        mAppWidgetHost = new CustomAppWidgetHost(getApplicationContext(), APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();
    }

    private void createBR(){
        mPackageReceiver = new PackageReceiver();
        mPackageReceiver.init(this);

        IntentFilter pFilter = new IntentFilter();
        pFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pFilter.addDataScheme("package");
        registerReceiver(mPackageReceiver, pFilter);
    }

    //위젯 생성
    private void getAppWidget(IconData iconData){
        mWaitWidgetIconData = iconData; //데이터 백업 (권한을 물을 수도 있기 때문)

        final ComponentName componentName = new ComponentName(iconData.getPackageName(), iconData.getComponentName());
        final int appWidgetId = mAppWidgetHost.allocateAppWidgetId(); //아이디 발급

        final AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
        if (!manager.bindAppWidgetIdIfAllowed(appWidgetId, componentName)) { //권한이 없다면
            Intent intent = new Intent(
                    AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, componentName);
            startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
        }else //있다면
            configureWidgetById(appWidgetId);
    }

    private void createWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        createWidget(appWidgetId);
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        configureWidgetById(appWidgetId);
    }

    private void configureWidgetById(int appWidgetId){
        AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(getApplicationContext()).getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else
            createWidget(appWidgetId);
    }

    //실제로 위젯을 생성하는 부분
    private void createWidget(int appWidgetId) {
        if(mWaitWidgetIconData != null){
            mWaitWidgetIconData.setWidgetId(appWidgetId);
            mLauncherController.addWidgetByRequest(mWaitWidgetIconData);
        }

        mWaitWidgetIconData = null;
    }

    //액티비티 실행
    private void runActivity(IconData data) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(data.getPackageName(), data.getComponentName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //Setting호출
    private void runSettingActivity(){
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //월페이퍼 호출
    private void runWallpaperActivity(){
        Intent intent = new Intent(this, WallpaperActivity.class);
        startActivityForResult(intent, REQUEST_WALLPAPER);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    //앱 삭제 요청
    private void deleteApp(String packageName) {
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(uninstallIntent);
    }

    //앱 추가됨
    public void addApp(String packageName){
        mLauncherModel.addAppByPackageName(packageName);
    }

    //업데이트
    public void updateWidget(){
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        sendBroadcast(intent);
    }

    //모델에서 패키지 삭제
    public void deletePackageToModel(String packageName){
        mLauncherModel.deleteAppByPackageName(packageName);
        LauncherDBHelper.inst().deleteData(packageName);
        mLauncherController.deleteIconByPackageName(packageName);
    }

    //이벤트 핸들링
    private static class MainHandler extends Handler {
        private WeakReference<MainActivity> mActWeakRef;
        private WeakReference<LauncherController> mContextWeakRef;

        public MainHandler(MainActivity act, LauncherController launcherController){
            mActWeakRef = new WeakReference<>(act);
            mContextWeakRef = new WeakReference<>(launcherController);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity act = mActWeakRef.get();
            LauncherController launcherController = mContextWeakRef.get();

            switch(msg.what){
                case HANDLE_LONG_TOUCH://롱터치 발생 처리
                    launcherController.longTouchAction(msg.arg1, msg.arg2);
                    break;
                case HANDLE_LOAD_FINISH: //AppData 로드가 완료 된 경우
                    launcherController.loadData();
                    break;
                case HANDLE_PICK_APPS_ITEM : //앱 아이콘을 픽 한 경우
                    launcherController.longTouchActionApps(msg.arg1, msg.arg2, (Integer) msg.obj);
                    break;
                case HANDLE_PICK_WIDGET_ITEM: //위젯 아이템을 픽 한 경우
                    launcherController.longTouchActionWidget(msg.arg1, msg.arg2, (IconData) msg.obj);
                    break;
                case HANDLE_PICK_WIDGET_CREATE: //위젯 생성을 요청 한 경우
                    act.getAppWidget((IconData) msg.obj);
                    break;
                case HANDLE_PAGE_EFFECT_ZOOM_OUT:
                    launcherController.startZoomEffect(true, msg.arg1, msg.arg2);
                    break;
                case HANDLE_PAGE_EFFECT_ZOOM_IN:
                    launcherController.startZoomEffect(false, msg.arg1, msg.arg2);
                    break;
                case HANDLE_CACHE_PICK_ITEM:
                    launcherController.getLongTouchCache(null, false);
                    break;
                case HANDLE_RUN_ACTIVITY:
                    act.runActivity((IconData) msg.obj);
                    break;
                case HANDLE_RUN_SETTING_ACTIVITY:
                    act.runSettingActivity();
                    break;
                case HANDLE_RUN_WALLPAPER_ACTIVITY:
                    act.runWallpaperActivity();
                    break;
                case HANDLE_DELETE_APP:
                    act.deleteApp((String) msg.obj);
                    break;
                case HANDLE_SET_SCREEN_MAIN:
                    launcherController.setMainScreen();
                    break;
            }
        }
    }
}
