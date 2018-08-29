package my.home.slauncher.view.effect;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * Created by ShinSung on 2017-09-11.
 * 종류 : 자식뷰들을 하단에서 상단으로 스크롤 효과를 내주는 뷰
 */

public class GlowView extends LinearLayout{
    private Scroller mScroller;
    private Runnable mCloseRunnable = null;

    public GlowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(getContext());
    }

    public void setAnimationVisiblity(int visibility) {
        final int height = getResources().getDisplayMetrics().heightPixels;
        if(visibility == View.VISIBLE){
            setVisibility(visibility);
            mScroller.startScroll(0, -height, 0, height, 500);
            invalidate();
        }
        else if(mCloseRunnable == null && visibility == View.GONE){
            mScroller.startScroll(0, 0, 0, -height, 500);
            invalidate();
            mCloseRunnable = new Runnable() {
                @Override
                public void run() {
                    setVisibility(View.GONE);
                    mCloseRunnable = null;
                }
            };
            postDelayed(mCloseRunnable, 500);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
