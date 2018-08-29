package my.home.slauncher.view.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import my.home.slauncher.data.IconData;

/**
 * Created by ShinSung on 2017-09-18.
 */

public class WidgetPreviewParent extends LinearLayout{
    private IconData mIconData;

    public WidgetPreviewParent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setIconData(IconData iconData){
        mIconData = iconData;
    }

    public IconData getIcondata(){
        return mIconData;
    }
}
