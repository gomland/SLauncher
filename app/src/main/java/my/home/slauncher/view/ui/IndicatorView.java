package my.home.slauncher.view.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import my.home.slauncher.R;


/**
 * Created by ShinSung on 2017-10-11.
 * 종류 : 인디케이터를 표시하는 클래스
 * 설명 : 워크스페이스 영역의 상태가 MAIN일 경우에만 동작한다.
 * 워크스페이스와 하단 아이콘 영역 사이에 2dp크기로 존재하며 페이지 스크롤 발생 후 600ms후에 사라진다.
 */

public class IndicatorView extends View implements AnimationListener{
    private int mIdx = 0, mCnt = 0;
    private Animation mAnimation;
    private Paint mPaint;

    public IndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);

        mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        mAnimation.setDuration(600);
        mAnimation.setAnimationListener(this);
    }

    public void setPosition(int idx, int cnt, boolean isUpdate){
        mIdx = idx;
        mCnt = cnt;

        clearAnimation();

        if(isUpdate)
            startAnimation(mAnimation);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mCnt > 1) {
            final int width = getWidth()/mCnt;
            canvas.drawRect(mIdx * width, 0, (mIdx + 1) * width, getHeight(), mPaint);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}
