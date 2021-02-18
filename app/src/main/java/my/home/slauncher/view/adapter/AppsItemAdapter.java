package my.home.slauncher.view.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import my.home.slauncher.R;
import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.IconData;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.tools.Utility;
import my.home.slauncher.view.activity.MainActivity;
import my.home.slauncher.view.drawable.FastBitmapDrawable;

/**
 * Created by ShinSung on 2017-09-11.
 * 종류 : 앱 아이콘에 대한 어뎁터
 */

public class AppsItemAdapter extends BaseAdapter implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener{
    private Context mContext;
    private LayoutInflater mInflater;
    private LauncherModel mLauncherModel;
    private Handler mMainHandler = null;

    public AppsItemAdapter(Context context, LauncherModel launcherModel, Handler mainHandler){
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mLauncherModel = launcherModel;
        mMainHandler = mainHandler;
    }

    @Override
    public int getCount() {
        return mLauncherModel.getAppsCnt();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null)
            view = mInflater.inflate(R.layout.apps_icon_layout, viewGroup, false);

        final TextView appsView = (TextView) view;
        appsView.setId(i);

        final ApplicationData data = mLauncherModel.getAppsData(i);

        final int height = mContext.getResources().getDimensionPixelSize(R.dimen.apps_icon_grid_height);
        ViewGroup.LayoutParams params = appsView.getLayoutParams();
        params.height = height;
        appsView.setLayoutParams(params);

        appsView.setOnClickListener(this);
        appsView.setOnLongClickListener(this);

        appsView.setOnTouchListener(this);

        appsView.setText(data.getName());
        final int iconSize = mContext.getResources().getDimensionPixelSize(R.dimen.apps_icon_size);
        FastBitmapDrawable d = (FastBitmapDrawable) Utility.createResizeDrawable(data.getIconDrawable(), iconSize, iconSize);
        appsView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);


        return appsView;
    }

    @Override
    public void onClick(View view) {
        int idx = view.getId();

        ApplicationData data = mLauncherModel.getAppsData(idx);
        if(data != null){
            IconData iconData = new IconData();
            iconData.setPackageName(data.getPackageName());
            iconData.setComponentName(data.getComponentName());

            Message msg = new Message();
            msg.what = MainActivity.HANDLE_RUN_ACTIVITY;
            msg.obj = iconData;
            mMainHandler.sendMessage(msg);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int idx = view.getId();
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        Message msg = new Message();
        msg.what = MainActivity.HANDLE_PICK_APPS_ITEM;
        msg.arg1 = location[0];
        msg.arg2 = location[1];
        msg.obj = idx;
        mMainHandler.sendMessage(msg);

        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            view.setAlpha(0.4f);
        else
            view.setAlpha(1);
        return false;
    }
}
