package my.home.slauncher.view.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import my.home.slauncher.tools.Utility;
import my.home.slauncher.view.animate.ZoomAnimation;
import my.home.slauncher.view.interfaces.DragEvent;
import my.home.slauncher.view.ui.PageWorkSpace;

/**
 * Created by ShinSung on 2017-09-06.
 * 종류 : 페이지에 대한 캐쉬를 복사하여 드래그 효과를 구현하는 클래스
 * 설명 :
 *    MODE_SLIDE - 페이지 이동시 scale down, fade out 효과가 일어남
 *    MODE_ZOOM - 페이지 롱탭시 확대, 축소 효과
 */

public class ScreenEffector extends FrameLayout implements DragEvent, Animation.AnimationListener {
    public static final int MODE_NON = -1; //미 동작 상태
    public static final int MODE_ZOOM = 0; //줌 인,아웃 효과 적용상태
    public static final int MODE_SLIDE = 1; //슬라이딩 상태

    private View mCurView = null; //대상 뷰
    private Bitmap mBitmap = null; //캐쉬 비트맵 저장

    private ZoomAnimation mZoomAnimation = null; //MODE_ZOOM시 사용하는 애니메이션
    private int mZoomTopMargin = 0; //Zoom 애니메이션시 top 마진 이동값 저장


    private int mPrevCurrentX, mScreenCurIdx; //페이지 인덱스 저장
    private final float SCALE_MIN = 0.6f; //슬라이딩 시 최소 sclae값

    private Paint mPaint; //공용 페인터

    private int mMode = MODE_NON; //모드 저장
    private int mAlpha;
    private float mScale;

    public ScreenEffector(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
    }

    //드래그 효과는 터치 때만 일어난다.
    @Override
    public void dragStart(View v, int screenIdx, int currentX) {
        if(mMode != MODE_NON) //다른 효과가 진행중이면 무시
            return ;

        mMode = MODE_SLIDE;
        mScreenCurIdx = screenIdx;
        mPrevCurrentX = currentX;
        mCurView = v;
        mScale = 1.0f;
        mAlpha = 255;

        if(mBitmap != null && !mBitmap.isRecycled())
            mBitmap.recycle();
        mBitmap = null;

        if(mCurView != null)
            mBitmap = Utility.getViewBitmap(mCurView);
        invalidate();
    }

    @Override
    public void dragging(int scrollX) {
        if(mMode != MODE_SLIDE)
            return ;

        final int moveX = mPrevCurrentX - scrollX;

        if(mCurView != null && mCurView.getVisibility() == View.VISIBLE && Math.abs(moveX) > 1)
            mCurView.setVisibility(View.INVISIBLE);

        mAlpha = getAlphaValue(moveX); //현재 알파값 저장
        mScale = getScaleValue(moveX); //현재 스케일 값 저장

        invalidate();//이동 값이 변경됨에 따라 갱신
    }

    @Override
    public void dragStop(int screenIdx, int delay) {
        if(mMode != MODE_SLIDE)
            return ;

        if(screenIdx == mScreenCurIdx) { //페이지 인덱스의 변화가 없다면 원래 크기로 복귀하는 애니메이션 적용
            if (mZoomAnimation == null)
                mZoomAnimation = new ZoomAnimation(this);
            mZoomAnimation.setDuration(150);
            mZoomAnimation.setData(new int[]{1,1}, new int[]{1,1}, mScale, 0);
            mZoomAnimation.setAnimationListener(this);
            startAnimation(mZoomAnimation);
        }
        else { //페이지 인덱스가 변화되었다면 150ms후 클리어
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    clear();
                }
            }, 150);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if(mBitmap != null) {
            if(mMode == MODE_SLIDE) { //슬라이드 모드일때
                final int[] location = new int[2];
                mCurView.getLocationOnScreen(location);

                final Matrix matrix = new Matrix();
                final int left = (int) ((mBitmap.getWidth() - mBitmap.getWidth() * mScale)/2);
                final int top = (int) ((mBitmap.getHeight() - mBitmap.getHeight() * mScale)/2) + location[1];

                mPaint.setAlpha(mAlpha);
                matrix.setScale(mScale, mScale);

                canvas.save();
                canvas.setMatrix(matrix);
                canvas.drawBitmap(mBitmap, left, top, mPaint);
                canvas.restore();
            }
            else if(mMode == MODE_ZOOM){ //줌 모드 일때
                mPaint.setAlpha(255);
                canvas.save();
                canvas.drawBitmap(mBitmap, 0, mZoomTopMargin, mPaint);
                canvas.restore();
            }
        }
    }

    public int getModeState(){
        return mMode;
    }

    //캐쉬 설정
    public void savePageCache(View v){
        if(mBitmap != null && !mBitmap.isRecycled()) //이전 캐쉬가 있으면 지움
            mBitmap.recycle();
        mBitmap = Utility.getViewBitmap(v);
    }

    //Zoom 효과 초기값 설정
    public void initZoomEffect(View v, int transY){
        mMode = MODE_ZOOM;
        mCurView = v;
        mZoomTopMargin = transY; //top 마진 값 설정
        invalidate();
        v.setVisibility(View.INVISIBLE);
    }

    //Zoom효과 시작
    //oldSize : 시작 크기 width, height
    //changeSize : 끝날때 크기 width, height
    public void startZoomEffect(int[] oldSize, int[] changedSize, int topMargin, float scale){
        mZoomAnimation = new ZoomAnimation(this);
        mZoomAnimation.setInterpolator(AnimationUtils.loadInterpolator(getContext(), android.R.anim.linear_interpolator));
        mZoomAnimation.setDuration(300);
        mZoomAnimation.setAnimationListener(this);
        mZoomAnimation.setData(oldSize, changedSize, scale, topMargin);

        startAnimation(mZoomAnimation);
    }

    private int getAlphaValue(int moveX){
        final int alpha = (int) (255 - Math.abs(moveX) / (getWidth() / 255) * 1.2f);
        return alpha > 255 ? 255 : alpha < 0 ? 0 : alpha;
    }

    private float getScaleValue(int moveX){
        final float scale = 1.0f - (float)Math.abs(moveX) / (float)getWidth() / 2.0f;
        return scale > 1.0f ? 1.0f : scale < SCALE_MIN ? SCALE_MIN: scale;
    }

    private void clear(){
        if(mBitmap != null && !mBitmap.isRecycled())
            mBitmap.recycle();

        if(mCurView != null) {
            mCurView.setVisibility(View.VISIBLE);
            if(mCurView instanceof PageWorkSpace)
                ((PageWorkSpace)mCurView).scrollToCenter(0);
        }

        mMode = MODE_NON;
        mCurView = null;
        mBitmap = null;

        invalidate();
    }

    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationEnd(Animation animation) {
        if(mZoomAnimation != null) {
            mZoomAnimation.setAnimationListener(null);
            mZoomAnimation.cancel();
        }
        mZoomAnimation = null;

        clear();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {}
}
