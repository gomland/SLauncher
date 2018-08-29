package my.home.slauncher.view.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import my.home.slauncher.R;


/**
 * Created by ShinSung on 2017-09-21.
 */

public class MenuButton extends android.support.v7.widget.AppCompatButton implements View.OnTouchListener{
    private Paint mPaint = null;
    public MenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(6);

        mPaint.setColor(Color.argb(150, 255, 255, 255));
        setTextColor(Color.argb(200, 255, 255, 255));
        setOnTouchListener(this);
    }

    public void setDrawable(BitmapDrawable d){
        final int size = getResources().getDimensionPixelSize(R.dimen.menu_btn_size);
        d.setBounds(0, 0, size, size);
        setCompoundDrawables(null, d, null, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            view.setAlpha(0.4f);
        else if(motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL)
            view.setAlpha(1);
        return false;
    }
}
