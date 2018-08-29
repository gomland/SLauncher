package my.home.slauncher.view.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import my.home.slauncher.R;


/**
 * Created by ShinSung on 2017-09-20.
 * 종류 : 메뉴의 등장 및 사라짐 효과를 구현
 */

public class AnimationLinearLayout extends LinearLayout implements Animation.AnimationListener{
    public static final int MODE_NON = -1;
    public static final int MODE_FADE_OUT = 0;
    public static final int MODE_SLIDE_UP = 1;
    public static final int MODE_SLIDE_DOWN = 2;

    //애니메이션 효과
    private Animation mAnimation;
    private int mMode = MODE_NON;

    public AnimationLinearLayout(Context context){
        this(context, null);
    }

    public AnimationLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAnimationVisiblity(int mode){
        clearAnimation();
        mAnimation = null;

        mMode = mode;

        if(MODE_FADE_OUT == mode) {
            mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        }
        else if(MODE_SLIDE_UP == mode) {
            setVisibility(View.VISIBLE);
            mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
        }
        else if(MODE_SLIDE_DOWN == mode){
            setVisibility(View.VISIBLE);
            mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
        }

        if(mAnimation != null) {
            mAnimation.setAnimationListener(this);
            startAnimation(mAnimation);
        }
    }

    public View checkChildSelected(int x){
        final int[] location = new int[2];
        for(int i=0; i<getChildCount(); i++){
            View v = getChildAt(i);
            v.getLocationOnScreen(location);
            if(x >= location[0] && x< location[0] + v.getWidth()) {
                v.setSelected(true);
                return v;
            }

        }

        return null;
    }

    public void clearSelected(){
        for(int i=0; i<getChildCount(); i++){
            getChildAt(i).setSelected(false);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if(mMode == MODE_FADE_OUT)
            setVisibility(View.GONE);

        animation.setAnimationListener(null);
        mMode = MODE_NON;
        mAnimation = null;
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}
