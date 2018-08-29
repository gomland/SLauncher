package my.home.slauncher.view.interfaces;

import android.view.View;

/**
 * Created by Shin on 2017-09-13.
 */

public interface DragAndDropEvent {
    View pick(int x, int y);
    boolean drop(View v, int x, int y, boolean result);
}
