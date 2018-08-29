package my.home.slauncher.view.animate;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import my.home.slauncher.view.ui.IconView;

/**
 * Created by ShinSung on 2017-ani_02-15.
 * 종류 : 아이콘 이동 효과를 주는 애니메이션 클래스
 */
public class MoveAnimation extends Animation {
    private int mStartX, mStartY, mEndX, mEndY;
    private IconView mTargetView;

    public MoveAnimation(IconView targetView){
        mTargetView = targetView;
    }

    public void setData(int startX, int startY, int endX, int endY){
        mStartX = startX;
        mStartY = startY;
        mEndX = endX;
        mEndY = endY;
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        mTargetView.setTranslationX(mStartX + (mEndX-mStartX)*interpolatedTime);
        mTargetView.setTranslationY(mStartY + (mEndY-mStartY)*interpolatedTime);
    }
}

