package my.home.slauncher.tools;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.io.ByteArrayOutputStream;

import my.home.slauncher.R;
import my.home.slauncher.data.IconData;
import my.home.slauncher.view.drawable.FastBitmapDrawable;
import my.home.slauncher.view.ui.CellLayout;
import my.home.slauncher.view.ui.IconView;

/**
 * Created by ShinSung on 2017-09-11.
 */

public class Utility {
    private static Canvas mCanvas = new Canvas();
    private static final Rect mOldBounds = new Rect();

    public static Drawable createResizeDrawable(Drawable icon, int width, int height) {
        final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        final Bitmap thumb = Bitmap.createBitmap(width, height, c);
        final Canvas canvas = mCanvas;
        canvas.setBitmap(thumb);

        mOldBounds.set(icon.getBounds());
        icon.setBounds(0, 0, width, height);
        icon.draw(canvas);
        icon.setBounds(mOldBounds);
        icon = new FastBitmapDrawable(thumb);

        return icon;
    }

    //Drawing cache로 부터 bitmap 복사
    public static Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();

        Bitmap src = Bitmap.createBitmap(cacheBitmap);
        cacheBitmap.recycle();

        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return src;
    }


    //위젯 사이즈 계산
    public static int[] rectToCell(Context context, int width, int height) {
        int widgetCellWidth =  context.getResources().getDimensionPixelSize(R.dimen.widget_cell_width); //위젯을 토막 칠 값
        int widgetCellHeight =  context.getResources().getDimensionPixelSize(R.dimen.widget_cell_height);
        final int smallerSize = Math.min(widgetCellWidth, widgetCellHeight);
        final int spanX = (width + smallerSize) / smallerSize;
        final int spanY = (height + smallerSize) / smallerSize;
        return new int[] { spanX, spanY };
    }




    public static Bitmap getViewCacheBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();

        Bitmap src = Bitmap.createBitmap(cacheBitmap);
        cacheBitmap.recycle();

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return src;
    }

    //캐쉬 비트맵 리사이즈 크기로 반환
    public static FastBitmapDrawable getCacheDrawable(View v, int[] size, float scale){
        if(v != null) {
            v.setDrawingCacheEnabled(true);
            Bitmap b = Utility.getViewBitmap(v);
            v.setDrawingCacheEnabled(false);

            if (b != null) {
                Bitmap resized = null;

                if (v instanceof IconView) {
                    final IconView iconView = (IconView) v;
                    final IconData iconData = iconView.getIconData();

                    resized = Bitmap.createScaledBitmap(b,
                            (int) (size[0] * iconData.getCellWidth() * scale),
                            (int) (size[1] * iconData.getCellHeight() * scale), false);
                } else if (v instanceof CellLayout)
                    resized = Bitmap.createScaledBitmap(b, size[0], size[1], false);

                b.recycle();

                if (resized != null) {
                    final FastBitmapDrawable drawable = new FastBitmapDrawable(resized);
                    return drawable;
                }
            }
        }

        return null;
    }

    public static FastBitmapDrawable getReszieBitmap(FastBitmapDrawable d, int width , int height){
        Bitmap src = d.getBitmap();
        if(src != null) {
            final float srcWidth = src.getWidth();
            final float srcHeight = src.getHeight();

            if(width < height)
                height = (int) (width * (srcHeight/srcWidth));
            else
                width = (int) (height * (srcWidth/srcHeight));

            Bitmap resized = Bitmap.createScaledBitmap(src, width, height, false);
            if(resized != null){
                src.recycle();
                final FastBitmapDrawable drawable = new FastBitmapDrawable(resized);
                return drawable;
            }
        }

        return d;
    }

    public static int getStatusBarHeight(Context context){
        int statusHeight = 0;

        int screenSizeType = (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK);

        if(screenSizeType != Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");

            if (resourceId > 0) {
                statusHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
        }

        return statusHeight;
    }

    public static byte[] getByteArrayFromDrawable(BitmapDrawable drawable){
        final Bitmap bitmap = drawable.getBitmap();

        if(bitmap != null){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return outputStream.toByteArray();
        }

        return null;
    }

    public static Bitmap getDrawableFromBytes(byte[] b){
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }
}
