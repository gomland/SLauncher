package my.home.slauncher.view.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import my.home.slauncher.R;
import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.IconData;
import my.home.slauncher.database.LauncherDBHelper;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.view.drawable.FastBitmapDrawable;
import my.home.slauncher.view.interfaces.DragAndDropEvent;

import static my.home.slauncher.database.LauncherDBHelper.inst;

/**
 * Created by ShinSung on 2017-09-28.
 * 종류 : 하단 아이콘 영역
 */

public class BottomCellLayout extends ViewGroup implements DragAndDropEvent {
    private int mAxisX = 5; //기본 격자 크기

    private LauncherModel mLauncherModel;
    private IconView[] mMap;

    private int mPrevX = -1;
    private int mCellSize;
    private boolean isIconDrawing = false;

    private FastBitmapDrawable mIconCacheDrawable = null;
    private Paint mPaint;

    public BottomCellLayout(Context context) {
        this(context, null);
    }

    public BottomCellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAxisX = getResources().getInteger(R.integer.btm_layout_x_axis);

        mMap = new IconView[mAxisX];
        mPaint = new Paint();
    }

    public void setModel(LauncherModel launcherModel){
        mLauncherModel = launcherModel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        final int count = getChildCount(); //자식의 개수

        //1개의 격자 크기 저장
        mCellSize = widthSpecSize/mAxisX;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if(child instanceof IconView) {
                final IconView iconView = (IconView) child;
                final IconData data = iconView.getIconData();

                CellLayout.LayoutParams params = new CellLayout.LayoutParams(data.getX() * mCellSize,
                        0,
                        mCellSize,
                        mCellSize);
                iconView.setLayoutParams(params);  //아이콘 위치 및 크기 저장
            }
        }

        setMeasuredDimension(widthSpecSize, mCellSize); //격자의 사이즈 만큼 높이를 설정
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == View.VISIBLE) {
                if(child instanceof IconView) {
                    CellLayout.LayoutParams params = (CellLayout.LayoutParams) child.getLayoutParams();
                    child.layout(params.x, params.y, params.x + params.width, params.y + params.height);
                }
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if(isIconDrawing && mIconCacheDrawable != null && mIconCacheDrawable.getBitmap() != null && !mIconCacheDrawable.getBitmap().isRecycled()) {
            final BlurMaskFilter blur = new BlurMaskFilter(5, BlurMaskFilter.Blur.OUTER);
            final int[] offsetXY = new int[2];
            final int x = mPrevX * mCellSize;

            mPaint.setColor(Color.WHITE);
            mPaint.setAlpha(255);
            mPaint.setMaskFilter(blur);

            Bitmap outerBitmapSrc = mIconCacheDrawable.getBitmap().extractAlpha(mPaint, offsetXY);
            canvas.drawBitmap(outerBitmapSrc, x, 0, mPaint);
            outerBitmapSrc.recycle();
        }
    }

    @Override
    public View pick(int x, int y) {
        return getChildPick(x);
    }

    @Override
    public boolean drop(View v, int x, int y, boolean result) {
        if(v instanceof IconView) { //드랍되는 종류가 아이콘이면
            final IconView iconView = (IconView) v;
            final IconData iconData = iconView.getIconData();

            iconData.setDummyView(false);

            if(iconData.getType() == IconData.TYPE_APPS){ //App타입의 아이콘만 허용
                return push(x/mCellSize, iconView);
            }
        }
        return false;
    }

    //런처 실행시 데이터 로드
    public void loadData(){
        setDefaultIcon(); //기본아이콘을 먼저 그리고

        final ArrayList<IconData> iconDataList = LauncherDBHelper.inst().getIconData(LauncherDBHelper.BOTTOM_PAGE_KEY); //해당 페이지키에 해당하는 아이콘을 검색
        for (IconData iconData : iconDataList) {
            IconView iconView = createIconView(iconData);

            if (iconData.getType() == IconData.TYPE_APPS) { //앱스 아이콘이면 모델에서 아이콘을 가져와 그린다
                final int modelIdx = mLauncherModel.getAppIndexByPackageName(iconData.getPackageName(), iconData.getComponentName());
                if(modelIdx != -1) { //TODO:삭제된 녀석에 대한 정보 제거 필요
                    ApplicationData data = mLauncherModel.getAppsData(modelIdx);
                    iconData.setDrawable(data.getIconDrawable());
                }
            }
            push(iconData.getX(), iconView);
        }
    }

    //아이템 넣기
    public boolean push(int x, IconView iconView){
        if(x < mAxisX && mMap[x] == null) {
            mMap[x] = iconView;

            final IconData iconData = iconView.getIconData();
            iconData.setX(x);
            iconData.setY(0);
            addView(iconView);

            if(iconData.getType() != IconData.TYPE_FIXED) {
                iconData.setPageKey(LauncherDBHelper.BOTTOM_PAGE_KEY); //하단 아이콘 영역 키를 설정하고
                inst().insertIconData(LauncherDBHelper.BOTTOM_PAGE_KEY, iconData); //해당 페이지 키에 저장
            }

            return true;
        }

        return false;
    }

    //터치 이펙트 주기
    public void setTouchEffect(int x){
        final IconView iconView = (IconView) getChild(x); //터치 영역 검사
        if (iconView != null)  //찾은 아이콘이 있으면
            iconView.setAlpha(0.5f);
    }

    //자식뷰 상태 초기화
    public void clearState(){
        if(mIconCacheDrawable != null && mIconCacheDrawable.getBitmap() != null && !mIconCacheDrawable.getBitmap().isRecycled())
            mIconCacheDrawable.getBitmap().recycle();
        mIconCacheDrawable =null;
        mPrevX = -1;

        for(int i=0; i<getChildCount(); i++)
            getChildAt(i).setAlpha(1);

        invalidate();
    }

    //롱터치가 일어났을때 픽 가능한 아이템 찾기
    public IconView getChildPick(int x){
        IconView iconView = (IconView) getChild(x);

        if(iconView != null) {
            IconData data = iconView.getIconData();

            if(data.getType() == IconData.TYPE_FIXED)
                return null;

            removeView(iconView);
            mMap[data.getX()] = null;
        }

        return iconView;
    }

    //빈공간 탐색
    public int checkSpace(View v, int x){
        final int cellX = x/mCellSize;
        int returnState = CellLayout.NOT_FOUND;

        isIconDrawing = false;

        if(v != null && v instanceof IconView && ((IconView)v).getIconData().getType() == IconData.TYPE_APPS) {
            if (isEmpty(x)) {
                if (mPrevX != cellX)
                    mPrevX = cellX;
                isIconDrawing = true;
                returnState = CellLayout.FOUND;
            }
        }

        invalidate();

        return returnState;
    }

    //드래그 효과 아이콘 이미지 캐싱
    public void setIconCacheDrawable(FastBitmapDrawable drawable){
        if(mIconCacheDrawable != null && mIconCacheDrawable.getBitmap() != null && !mIconCacheDrawable.getBitmap().isRecycled())
            mIconCacheDrawable.getBitmap().recycle();
        mIconCacheDrawable = drawable;
    }

    //셀 사이즈 반환
    public int getIconSize(){
        return mCellSize;
    }

    //자식뷰 찾기
    public View getChild(int x){
        int cellX = x/mCellSize;

        if(cellX >= mAxisX || cellX < 0)
            return null;
        else
            return mMap[cellX];
    }

    //빈 공간 탐색
    public boolean isEmpty(int x){
        int cellX = x/mCellSize;

        if(cellX >= mAxisX || cellX < 0)
            return false;
        else if(mMap[cellX] == null)
            return true;
        else
            return false;
    }

    //드래그 취소 제자리 복귀
    public void cancelDragIcon(IconView iconView){
        final IconData iconData = iconView.getIconData();

        if(LauncherDBHelper.inst().isIconExisted(iconData.getKey())) //기존에 저장되어있던 아이콘이면 원래 자리로 복귀
            push(iconData.getX(), iconView);
    }

    //기본 Apps아이콘
    private void setDefaultIcon(){
        int positionX;

        if(mAxisX%2 == 0) //격자를 나눈 형태가 짝수면 가장 오른쪽에 위치시킨다.
            positionX = mAxisX-1;
        else
            positionX = mAxisX/2;

        IconView iconView = new IconView(getContext());
        IconData iconData = new IconData();
        iconData.setName(getResources().getString(R.string.apps));
        iconData.setKey(IconData.KEY_APPS);
        iconData.setType(IconData.TYPE_FIXED);
        iconData.setDummyView(false);
        iconData.setX(positionX);
        iconData.setY(0);
        iconData.setCellWidth(mCellSize);
        iconData.setCellHeight(mCellSize);
        iconView.setIconData(iconData);
        iconData.setDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.btn_apps));
        push(positionX, iconView);
    }


    //아이콘 데이터를 기반으로 아이콘뷰 생성
    private IconView createIconView(IconData iconData){
        IconView iconView = new IconView(getContext());
        iconView.setIconData(iconData);
        return iconView;
    }
}
