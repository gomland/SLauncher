package my.home.slauncher.view.interfaces;

import android.view.View;

/**
 * Created by OWNER on 2017-09-08.
 */
public interface DragEvent {
    void dragStart(View v, int screenIdx, int currentX);
    void dragging(int scrollX);
    void dragStop(int screenIdx,int delay);
}
