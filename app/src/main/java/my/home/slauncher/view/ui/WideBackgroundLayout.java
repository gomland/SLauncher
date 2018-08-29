package my.home.slauncher.view.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by ShinSung on 2017-09-24.
 * 종류 : 배경이미지를 그리기위한 레이아웃
 */

public class WideBackgroundLayout extends FrameLayout implements ValueAnimator.AnimatorUpdateListener{
    private BitmapDrawable mBackgroundDrawable = null;

    private int mBackgroundLeftX = 0; //WIDE모드일때 좌표값 저장

    public static final int MODE_CROP = 0; //폰 화면 사이즈에 맞게 자른 경우
    public static final int MODE_WIDE = 1; //가로가 더 긴 이미지
    public static final int MODE_RATIO = 2; //가로가 더 긴 이미지나 화면에 딱 맞게 표출

    private int mMode = MODE_CROP; //기본 모드

    public WideBackgroundLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawBackgroundImage(canvas);
    }

    //배경 이미지 설정
    public void setBackground(int mode, BitmapDrawable d){
        mMode = mode;
        mBackgroundDrawable = d;
        setBackgroundPosition(0, 0, 1, false, false); //기본 포지션 설정
        invalidate(); //갱신
    }

    //배경 이미지 삭제
    public void clear(){
        if(mBackgroundDrawable != null && mBackgroundDrawable.getBitmap() != null && !mBackgroundDrawable.getBitmap().isRecycled())
            mBackgroundDrawable.getBitmap().recycle();

        invalidate();
    }

    //백그라운드 이미지 포지션 계산
    //isPageMode : 페이지 모드가 활성화되면 추가페이지가 있어서 child 1개를 덜 계산함
    public void setBackgroundPosition(int idx, int moveX, int cnt, boolean isPageMode, boolean isAnimated){
        if(mBackgroundDrawable != null && mMode == MODE_WIDE) {
            final BitmapDrawable bitmapDrawable = mBackgroundDrawable;
            final Bitmap b = bitmapDrawable.getBitmap();

            final float drawableWidth = b.getWidth() * ((float) getHeight() / b.getHeight()); //배경 비트맵이 설정된 실제 너비
            final float scrollWidthRatio = (drawableWidth - getWidth()) / (getWidth() * (cnt)); //페이지 사이즈대 배경 너비 비율
            final int screenDrawableWidth = (int) ((drawableWidth - getWidth()) / cnt); //실제로 1페이지당 드래그 할 영역

            bitmapDrawable.setBounds(0, 0, (int) drawableWidth, getHeight()); //배경 비트맵 범위 설정

            int left = (int) (-idx * screenDrawableWidth + moveX*scrollWidthRatio);  //스크롤 위치 설정

            if (left > 0) //좌측 영역을 벗어날 경우 0으로 초기화
                left = 0;
            else if (left != 0 && left < -(drawableWidth - getWidth() - screenDrawableWidth - (isPageMode ? 0 : screenDrawableWidth))) //우측 영역을 벗어날 경우 최대사이즈로 초기화
                left = (int) -(drawableWidth - getWidth() - screenDrawableWidth - (isPageMode ? 0 : screenDrawableWidth));

            if(isAnimated){ //애니메이션이 필요하면 애니메이션 적용 : 드래그중 손을 때었을때 효과를 위해 추가
                final int duration = (int) (Math.abs(mBackgroundLeftX-left)/(float)screenDrawableWidth * 200);
                mBackgroundLeftX += (left-mBackgroundLeftX)/8;
                final ValueAnimator va = ValueAnimator.ofInt(mBackgroundLeftX, left);
                va.setDuration(duration);
                va.addUpdateListener(this);
                va.start();
            }
            else
                mBackgroundLeftX = left;
            invalidate();
        }
    }

    //캔버스에 그리기
    private void drawBackgroundImage(Canvas canvas) {
        if(mBackgroundDrawable != null && mBackgroundDrawable.getBitmap() != null && !mBackgroundDrawable.getBitmap().isRecycled()){
            canvas.save();

            mBackgroundDrawable.setAlpha(220);

            if(mMode == MODE_WIDE) { //WIDE모드
                Matrix m = new Matrix();
                m.setTranslate(mBackgroundLeftX, 0);
                canvas.setMatrix(m);
            }
            else if(mMode == MODE_RATIO){ //화면 비율에 딱맞는 모드
                final BitmapDrawable bitmapDrawable = mBackgroundDrawable;
                final Bitmap b = bitmapDrawable.getBitmap();
                final int bitmapHeight = (int) (getWidth()*((float)b.getHeight()/b.getWidth()));
                final int top = (getHeight() - bitmapHeight)/2;

                mBackgroundDrawable.setBounds(0, top, getWidth(), top + bitmapHeight);
            }
            else if(mMode == MODE_CROP) //자른이미지는 전체화면에 맞게
                mBackgroundDrawable.setBounds(0, 0, getWidth(), getHeight());

            mBackgroundDrawable.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        mBackgroundLeftX = (int) valueAnimator.getAnimatedValue();
        postInvalidate();
    }
}
