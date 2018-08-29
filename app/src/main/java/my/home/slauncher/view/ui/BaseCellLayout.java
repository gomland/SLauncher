package my.home.slauncher.view.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.ViewGroup;

import my.home.slauncher.R;


/**
 * Created by ShinSung on 2017-09-21.
 * 종류 : 페이지에 대한 기본 레이아웃
 * 설명 : 페이지 모드 활성화시 백그라운드 표시, scale 기능을 기본적으로 가짐
 */

public class BaseCellLayout extends ViewGroup{
    protected float mScale = 1.0f; //화면 zoom 값
    protected float mRatio = 1.0f; //화면 가로 대 세로 비율
    protected Paint mPaint;

    protected boolean isPageMode = false; //페이지 편집 모드 활성화 여부
    private boolean isAdderScreen = false; //스크린 추가용 페이지

    public static final int FOCUS_NON = 0;
    public static final int FOCUS_PRESS = 1;
    public static final int FOCUS_CHANGED = 2;
    protected int mFoucsType = FOCUS_NON; //포커스 효과

    private static BitmapDrawable mBgDrawable = null;

    public BaseCellLayout(Context context) {
        this(context, null);
    }

    public BaseCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mPaint = new Paint();

        if(mBgDrawable == null)
            mBgDrawable = (BitmapDrawable) AppCompatResources.getDrawable(getContext(), R.drawable.cell_bg_add);
    }

    public BaseCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if(isPageMode){ //페이지 모드가 활성화 되어 있다면 zoom out 시킴
            widthSpecSize *= mScale;
            heightSpecSize = (int) (widthSpecSize * mRatio);//일반 모드일때의 비율로 계산
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
    }

    //화면 scale 비율 설정
    protected void setScaleValue(float scale, float ratio){
        mScale = scale;
        if(!isPageMode) //페이지 모드가 아닐때의 설정값만 알아냄
            mRatio = ratio;
    }

    //페이지 모드 설정
    //true : 80%로 scale되며, 좌우 페이지가 걸쳐서 보임
    //false : 100%로 scale됨
    protected void setPageEditingMode(boolean set){
        isPageMode = set;
        if(set)
            setBackgroundResource(R.drawable.cell_bg);
        else
            setBackground(null);
    }

    public void setAdderScreen(boolean set){
        isAdderScreen = set;
    }

    public void setFocus(int type){
        mFoucsType = type;
        if(isPageMode){
            if(mFoucsType == FOCUS_PRESS)
                setAlpha(0.5f);
            else if(mFoucsType == FOCUS_CHANGED)
                setAlpha(0.9f);
            else
                setAlpha(1);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(isAdderScreen) {
            if(mBgDrawable != null && mBgDrawable.getBitmap() != null && !mBgDrawable.getBitmap().isRecycled()) {
                final Bitmap b = mBgDrawable.getBitmap();
                canvas.drawBitmap(b, getWidth() / 2 - b.getWidth() / 2, getHeight() / 2 - b.getHeight() / 2, mPaint);
            }
        }
    }
}
