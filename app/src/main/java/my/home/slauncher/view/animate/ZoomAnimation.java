package my.home.slauncher.view.animate;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by ShinSung on 2017-09-30.
 * 종류 : 줌 인/아웃 시 효과를 주기 위한 애니메이션 클래스
 */
public class ZoomAnimation extends Animation {
    private View mTargetView;
    private int[] mOldSize, mChangedSize;
    private float mScaleValue;
    private int mTopMargin;

    public ZoomAnimation(View targetView){
        mTargetView = targetView;
    }

    public void setData(int[] oldSize, int[] changedSize, float scale, int topMargin){
        mOldSize = oldSize;
        mChangedSize = changedSize;
        mScaleValue = scale;
        mTopMargin = topMargin;
    }

    @Override
    public void cancel() {
        super.cancel();
        mTargetView.setTranslationY(0);
        mTargetView.setScaleX(1);
        mTargetView.setScaleY(1);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        if(interpolatedTime <= 0)
            return ;

        final float scaleX = mChangedSize[0]/(float)mOldSize[0];
        final float scaleY = mChangedSize[1]/(float)mOldSize[1];

        if(mOldSize[0] > mChangedSize[0]) {
            mTargetView.setTranslationY(mTopMargin*interpolatedTime);
            mTargetView.setScaleX(1 - (1 - scaleX) * interpolatedTime);
            mTargetView.setScaleY(1 - (1 - scaleY) * interpolatedTime);
        }
        else{
            mTargetView.setTranslationY(mTopMargin*interpolatedTime);
            mTargetView.setScaleX(1 + (mScaleValue*scaleX-mScaleValue)*interpolatedTime);
            mTargetView.setScaleY(1 + (mScaleValue*scaleY-mScaleValue)*interpolatedTime);
        }
    }
}

