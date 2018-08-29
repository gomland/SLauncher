package my.home.slauncher.view.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.GridView;

import my.home.slauncher.R;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.view.adapter.AppsItemAdapter;
import my.home.slauncher.view.adapter.WidgetItemAdapter;

/**
 * Created by ShinSung on 2017-09-12.
 * 종류 : 앱스 및 위젯 리스트를 표출하기 위한 그리드 뷰
 */

public class AllIconGirdView extends GridView{
    public static final int MODE_APPS = 0;
    public static final int MODE_WIDGETS = 1;

    private AppsItemAdapter mAppsItemAdapter;
    private WidgetItemAdapter mWidgetItemAdapter;

    private int mAppsCol, mWidgetCol;

    public AllIconGirdView(Context context) {
        this(context, null);
    }

    public AllIconGirdView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllIconGirdView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GridInfo, defStyle, 0);
        mAppsCol = typedArray.getInt(R.styleable.GridInfo_appsColumn, 5);
        mWidgetCol = typedArray.getInt(R.styleable.GridInfo_widgetColumn, 2);
    }

    public void init(LauncherModel launcherModel, Handler handler) {
        mAppsItemAdapter = new AppsItemAdapter(getContext(), launcherModel, handler);
        mWidgetItemAdapter = new WidgetItemAdapter(getContext(), launcherModel, handler);
    }

    public void changeMode(int mode) {
        if (mode == MODE_APPS) {
            setNumColumns(mAppsCol);
            setAdapter(mAppsItemAdapter);
            mAppsItemAdapter.notifyDataSetChanged();
        } else if (mode == MODE_WIDGETS) {
            setNumColumns(mWidgetCol);
            setAdapter(mWidgetItemAdapter);
            mWidgetItemAdapter.notifyDataSetChanged();
        }
    }
}