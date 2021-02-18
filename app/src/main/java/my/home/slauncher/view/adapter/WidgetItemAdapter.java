package my.home.slauncher.view.adapter;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.content.res.AppCompatResources;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import my.home.slauncher.R;
import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.IconData;
import my.home.slauncher.data.WidgetListUpData;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.tools.Utility;
import my.home.slauncher.view.activity.MainActivity;
import my.home.slauncher.view.drawable.FastBitmapDrawable;
import my.home.slauncher.view.ui.WidgetPreviewParent;

/**
 * Created by ShinSung on 2017-09-11.
 * 종류 : 위젯에 대한 아이템 어텝터
 */

public class WidgetItemAdapter extends BaseAdapter implements View.OnLongClickListener{
    private Context mContext;
    private LayoutInflater mInflater;
    private LauncherModel mLauncherModel;
    private Handler mMainHandler = null;

    private PackageManager mPackageManager;

    public WidgetItemAdapter(Context context, LauncherModel launcherModel, Handler mainHandler){
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mLauncherModel = launcherModel;
        mMainHandler = mainHandler;

        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public int getCount() {
        return mLauncherModel.getWidgetCnt();
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
        if(view == null) {
            view = mInflater.inflate(R.layout.widget_icon_layout, viewGroup, false);
        }
        final WidgetListUpData widgetData = mLauncherModel.getWidgetData(i);
        final LinearLayout widgetLayout = (LinearLayout) view;

        final TextView titleView = view.findViewById(R.id.widget_title);
        setTitle(titleView, widgetData.getPackageName(), widgetData.getComponentName());


        final LinearLayout widgetContainer = view.findViewById(R.id.widget_container);

        if (widgetContainer != null) {
            widgetContainer.removeAllViews();
            setWidgets(widgetContainer, widgetData);
        }

        widgetLayout.invalidate();

        return view;
    }

    private void setTitle(TextView titleView, String packageName, String componetName){
        final int appIdx = mLauncherModel.getAppIndexByPackageName(packageName, componetName);

        if(appIdx != -1) {
            ApplicationData appData = mLauncherModel.getAppsData(appIdx);
            titleView.setText(appData.getName());

            final int widgetSize = mContext.getResources().getDimensionPixelSize(R.dimen.widget_title_icon_size);
            FastBitmapDrawable d = (FastBitmapDrawable) Utility.createResizeDrawable(appData.getIconDrawable(), widgetSize, widgetSize);
            titleView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
        }
        else{
            final PackageManager packageManager = mContext.getApplicationContext().getPackageManager();
            String name = null;
            try {
                ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                name = (String) info.loadLabel(packageManager);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if(name == null)
                titleView.setText(R.string.no_name);
            else
                titleView.setText(name);

            Drawable d = AppCompatResources.getDrawable(mContext, R.drawable.no_icon);
            titleView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
        }
    }

    private void setWidgets(LinearLayout widgetContainer, WidgetListUpData widgetData){
        ArrayList<AppWidgetProviderInfo> widgetList = widgetData.getWidgetList();
        for (AppWidgetProviderInfo info : widgetList){
            LinearLayout widgetView = createWidgetPreview(widgetContainer, info);
            if(widgetView != null) {
                widgetContainer.addView(widgetView);
                widgetView.setOnLongClickListener(this);
            }
        }
    }

    private LinearLayout createWidgetPreview(LinearLayout parentView, AppWidgetProviderInfo info){
        final WidgetPreviewParent widgetView = (WidgetPreviewParent) mInflater.inflate(R.layout.widget_item, parentView, false);
        final TextView widgetName = widgetView.findViewById(R.id.widget_name);
        final ImageView widgetPreview = widgetView.findViewById(R.id.widget_preview);

        String name;
        Drawable drawable = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = info.loadPreviewImage(mContext, DisplayMetrics.DENSITY_HIGH);
            if (drawable == null)
                drawable = info.loadIcon(mContext, DisplayMetrics.DENSITY_XXXHIGH);
            name = info.loadLabel(mPackageManager);
        } else {
            name = info.label;
            Resources resources = null;
            try {
                resources = mPackageManager.getResourcesForApplication(info.provider.getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if(resources != null && info.previewImage != 0x0)
                drawable = resources.getDrawableForDensity(info.previewImage, resources.getDisplayMetrics().densityDpi);
        }

        final int[] widgetCellSize = Utility.rectToCell(mContext, info.minWidth, info.minHeight);

        final IconData iconData = new IconData();
        iconData.setPackageName(info.provider.getPackageName());
        iconData.setComponentName(info.provider.getClassName());
        iconData.setCellWidth(widgetCellSize[0]);
        iconData.setCellHeight(widgetCellSize[1]);

        if(drawable != null){
            iconData.setDrawable(drawable);
            widgetPreview.setImageDrawable(drawable);
        }

        widgetName.setText(widgetCellSize[0] + "x" + widgetCellSize[1] + " " + name);

        widgetView.setIconData(iconData);

        return widgetView;
    }

    @Override
    public boolean onLongClick(View view) {
        if(view instanceof  WidgetPreviewParent) {
            final WidgetPreviewParent widgetPreviewParent = (WidgetPreviewParent)view;
            final IconData data = widgetPreviewParent.getIcondata();
            if(data != null) {
                data.setCacheView(true); //preview모드를 설정

                int[] location = new int[2];
                view.getLocationOnScreen(location);
                Message msg = new Message();
                msg.what = MainActivity.HANDLE_PICK_WIDGET_ITEM;
                msg.arg1 = location[0];
                msg.arg2 = location[1];
                msg.obj = data;
                mMainHandler.sendMessage(msg);
            }
        }
        return false;
    }
}
