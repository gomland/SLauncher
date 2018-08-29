package my.home.slauncher.view.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by ShinSung on 2017-03-24.
 * 종류 : 이미지를 Crop 할 수 있는 뷰
 */
public class AreaSelectView extends android.support.v7.widget.AppCompatImageView{
    //화면 설정 모드
    public static final int CROP = 0;
    public static final int WIDE = 1;

    private float SELECTED_SCREEN_WIDTH = 1024f;
    private float SELECTED_SCREEN_HEIGHT = 600f;

    private Bitmap mSelectedBitmap = null;
    private Paint mSelectedLine, mDimming;
    private RectF screenRect;
    private Point mSelectOffset;

    private int mMode = CROP;
    private boolean isTouchEventCatch = true;

    public AreaSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSelectedLine = new Paint(); //선택영역 페인트
        mSelectedLine.setStyle(Paint.Style.STROKE);
        mSelectedLine.setColor(Color.argb(255, 106, 113 ,204));
        mSelectedLine.setStrokeWidth(4);

        mDimming = new Paint(); //디밍영역 페인트
        mDimming.setStyle(Paint.Style.FILL);
        mDimming.setColor(Color.argb(174, 79, 79, 79));
        
        mSelectOffset = new Point(0, 0); //터치 오프셋

        //화면 전체 사이즈 맵핑
        SELECTED_SCREEN_WIDTH = getResources().getDisplayMetrics().widthPixels;
        SELECTED_SCREEN_HEIGHT = getResources().getDisplayMetrics().heightPixels;
    }


    @Override
    public void setImageResource(int resId){
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        BitmapFactory.Options options =  calculateInSampleSize(resId, (int)viewWidth, (int)viewHeight);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId, options);
        setImageBitmap(bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mSelectedBitmap != null) {
            canvas.drawBitmap(mSelectedBitmap, null, screenRect, mSelectedLine); //비트맵 이미지 그리기
            drawSelectedArea(canvas);
        }
    }

    //이미지 모드 설정 : 크롭, 와이드
    public void setImageMode(int mode){
        mMode = mode;
        invalidate();
    }

    //선택된 영역 비트맵 전송
    public Bitmap getSelectBitmap(){
        Bitmap bitmap = null;

        if(mSelectedBitmap != null) {
            float width = screenRect.right - screenRect.left;
            float height = screenRect.bottom - screenRect.top;
            float widthRatio = mSelectedBitmap.getWidth() / width;
            float heightRatio = mSelectedBitmap.getHeight() / height;

            float selectWidth = (screenRect.bottom - screenRect.top) * (SELECTED_SCREEN_WIDTH / SELECTED_SCREEN_HEIGHT);
            float selectHeight = (screenRect.right - screenRect.left) * (SELECTED_SCREEN_HEIGHT / SELECTED_SCREEN_WIDTH);

            if((int)selectHeight < (int)(screenRect.bottom-screenRect.top)) { //세로형
                bitmap = Bitmap.createBitmap(mSelectedBitmap,
                        (int)(mSelectOffset.x * widthRatio),
                        (int)(mSelectOffset.y * heightRatio),
                        (int)(width * widthRatio),
                        (int)(selectHeight * heightRatio));
            }
            else if((int)selectWidth < (int)(screenRect.right-screenRect.left) && mMode == CROP) { //가로형
                bitmap = Bitmap.createBitmap(mSelectedBitmap,
                        (int)(mSelectOffset.x * widthRatio),
                        (int)(mSelectOffset.y * heightRatio),
                        (int)(selectWidth * widthRatio),
                        (int)(height * heightRatio));
            }
            else  //일치형
                bitmap = Bitmap.createBitmap(mSelectedBitmap, 0, 0, mSelectedBitmap.getWidth(), mSelectedBitmap.getHeight());
        }
        
        if(bitmap != null && mMode == CROP)
            return Bitmap.createScaledBitmap(bitmap, (int) SELECTED_SCREEN_WIDTH, (int) SELECTED_SCREEN_HEIGHT, true);
        else
            return bitmap;
    }

    //return : WIDE모드를 지원하는 이미지인 경우 true
    public boolean setImageFile(String filePath){
        final float viewWidth = getWidth();
        final float viewHeight = getHeight();

        boolean isWideSupported = false;

        BitmapFactory.Options options =  calculateInSampleSize(filePath, (int)viewWidth, (int)viewHeight);
        options.inPreferredConfig = Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if(bitmap != null) {
            Bitmap rotateBitmap = rotateBitmap(filePath, bitmap);
            if (rotateBitmap != null)
                isWideSupported = setImageRect(rotateBitmap);
            else
                isWideSupported = setImageRect(bitmap);
        }
        else
            Log.e(getClass().toString(), "이미지 로드 실패...");

        return isWideSupported;
    }

    public int getMode(){
        return mMode;
    }

    public void clear(){
        if(mSelectedBitmap != null && !mSelectedBitmap.isRecycled())
            mSelectedBitmap.recycle();
        mSelectedBitmap=null;
    }

    //이미지에 맞는 Rect 크기 지정
    public boolean setImageRect(Bitmap bitmap){
        boolean isWideSupported = false;

    	float viewWidth = getWidth();
        float viewHeight = getHeight();

        if(mSelectedBitmap != null)
            mSelectedBitmap.recycle();

        mSelectOffset.x = 0;
        mSelectOffset.y = 0;

        mSelectedBitmap = bitmap;

        float bitmapWidth = mSelectedBitmap.getWidth();
        float bitmapHeight = mSelectedBitmap.getHeight();

        float screenRatio = viewWidth / viewHeight;
        float bitmapRatio = bitmapWidth / bitmapHeight;

        if(screenRatio > bitmapRatio) { //세로형
            float imageWidth = bitmapWidth * (viewHeight / bitmapHeight);
            screenRect = new RectF((viewWidth - imageWidth)/2 , 0, imageWidth + (viewWidth - imageWidth)/2, viewHeight);
        }
        else if(screenRatio < bitmapRatio) { //가로형
            float imageHeight = bitmapHeight * (viewWidth / bitmapWidth);
            screenRect = new RectF(0, (viewHeight - imageHeight)/2, viewWidth, imageHeight + (viewHeight - imageHeight)/2);
            isWideSupported = true;
        }
        else //일치형
            screenRect = new RectF(0, 0, viewWidth, viewHeight);

        invalidate();

        return isWideSupported;
    }

    //이미지 결과물에 대한 회전 옵션 적용
    private Bitmap rotateBitmap(String path, Bitmap src){
    	Bitmap rotateBitmap = null;

    	try {
			ExifInterface exif = new ExifInterface(path);
			String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
			int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
			int rotation = 0;
			if(orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
			else if(orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
			else if(orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
			
			Matrix matrix = new Matrix();
			matrix.setRotate(rotation, src.getWidth()/2, src.getHeight()/2);
			rotateBitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return rotateBitmap;
    }

    //영역 그리기
    private void drawSelectedArea(Canvas canvas){
        float width = (screenRect.bottom - screenRect.top) * (SELECTED_SCREEN_WIDTH / SELECTED_SCREEN_HEIGHT);
        float height = (screenRect.right - screenRect.left) * (SELECTED_SCREEN_HEIGHT / SELECTED_SCREEN_WIDTH);

        if((int)height < (int)(screenRect.bottom-screenRect.top)) {  //세로형
            //선택 영역
            canvas.drawRect(screenRect.left, screenRect.top + mSelectOffset.y, screenRect.right, screenRect.top + height + mSelectOffset.y, mSelectedLine);

            //디밍 영역
            canvas.drawRect(screenRect.left, screenRect.top, screenRect.right, screenRect.top + mSelectOffset.y, mDimming);
            canvas.drawRect(screenRect.left, screenRect.top + height + mSelectOffset.y, screenRect.right, screenRect.bottom, mDimming);
        }
        else if((int)width < (int)(screenRect.right-screenRect.left) && mMode == CROP) { //가로형
            //선택 영역
            canvas.drawRect(screenRect.left + mSelectOffset.x, screenRect.top, screenRect.left + width + mSelectOffset.x, screenRect.bottom, mSelectedLine);

            //디밍 영역
            canvas.drawRect(screenRect.left, screenRect.top, screenRect.left + mSelectOffset.x, screenRect.bottom, mDimming);
            canvas.drawRect(width + mSelectOffset.x, screenRect.top, screenRect.right, screenRect.bottom, mDimming);
        }
    }

    //터치 발생 시 오프셋 계산
    private void changeScreenOffset(float moveX, float moveY){
        float width = (screenRect.bottom - screenRect.top) * (SELECTED_SCREEN_WIDTH / SELECTED_SCREEN_HEIGHT);
        float height = (screenRect.right - screenRect.left) * (SELECTED_SCREEN_HEIGHT / SELECTED_SCREEN_WIDTH);

        if((int)height < (int)(screenRect.bottom-screenRect.top)) {  //세로형
            mSelectOffset.x = 0;
            mSelectOffset.y += moveY;

            if(0 > mSelectOffset.y)
                mSelectOffset.y = 0;
            else if(screenRect.top + mSelectOffset.y + height > screenRect.bottom)
                mSelectOffset.y = (int) (screenRect.bottom - height - screenRect.top);
        }
        else if((int)width < (int)(screenRect.right-screenRect.left)) { //가로형
            mSelectOffset.x += moveX;
            mSelectOffset.y = 0;

            if (0 > mSelectOffset.x)
                mSelectOffset.x = 0;
            else if (mSelectOffset.x + width > screenRect.right) {
                mSelectOffset.x = (int) (screenRect.right - width);
            }
        }

        invalidate();
    }


    //샘플링 사이즈 계산
    private BitmapFactory.Options getSampleSize(BitmapFactory.Options options, int width, int height) {   
        int targetSize = width;
        int itemSize = options.outWidth;

        if(options.outWidth < options.outHeight) {
            targetSize = height;
            itemSize = options.outHeight;
        }

        int cnt = 0;
        if(targetSize > 0)
        	cnt = (int)Math.ceil(itemSize/targetSize);
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        while(cnt > 1) {
            cnt /= 2;
            options.inSampleSize *= 2;
            if(options.inSampleSize >= 64)
                break;
        }
        
        return options;
    }
    
    //file로 부터 샘플사이즈 얻기
    private BitmapFactory.Options calculateInSampleSize(String filePath, int width, int height) {
        BitmapFactory.Options options =  new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

    	BitmapFactory.decodeFile(filePath, options);
    	
    	return getSampleSize(options, width, height);
    }
    
    private BitmapFactory.Options calculateInSampleSize(FileDescriptor fd, int width, int height) {
        BitmapFactory.Options options =  new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

    	BitmapFactory.decodeFileDescriptor(fd, null, options);
    	
    	return getSampleSize(options, width, height);
    }
    
    //resource id로 부터 샘플사이즈 얻기
    private BitmapFactory.Options calculateInSampleSize(int resId, int width, int height) {
        BitmapFactory.Options options =  new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(getResources(), resId, options);
        
    	return getSampleSize(options, width, height);
    }

    //터치 이벤트 영역
    private float mTouchX, mTouchY;
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            mTouchX = event.getX();
            mTouchY = event.getY();
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE){
            changeScreenOffset(event.getX() - mTouchX, event.getY() - mTouchY);
            mTouchX = event.getX();
            mTouchY = event.getY();
        }

        return isTouchEventCatch;
    }
}
