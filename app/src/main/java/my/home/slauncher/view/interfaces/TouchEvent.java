package my.home.slauncher.view.interfaces;

import android.view.MotionEvent;

/**
 * Created by OWNER on 2017-08-31.
 */

public interface TouchEvent {
    void touchStart(MotionEvent event);
    void touchIng(MotionEvent event);
    void touchEnd(MotionEvent event);
}
