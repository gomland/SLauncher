package my.home.slauncher.view.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;

import my.home.slauncher.R;
import my.home.slauncher.data.IconData;
import my.home.slauncher.view.drawable.FastBitmapDrawable;
import my.home.slauncher.view.interfaces.DragAndDropEvent;

/**
 * Created by ShinSung  on 2017-08-31.
 * 종류 : 1개의 페이지 단위 클래스
 * 내용 : 일반 모드와 페이지 에디터 모드가 있다.
 * 일반 모드는 전체화면
 * 페이지 에디터 모드는 설정한 scale 값만큼 줄어든다.
 */

public class CellLayout extends BaseCellLayout implements DragAndDropEvent {
    private int mAxisX = 0, mAxisY = 0; //가로 세로 격자 분할 크기
    private int mCellWidth, mCellHeight; //가로, 세로 격자 하나당 크기

    private FastBitmapDrawable mIconCacheDrawable = null;

    private IconView[][] mMap, mDummyMap; //실제 맵데이터, 더미 데이터(아이콘 애니메이션 시 활용)

    private ArrayList<IconView> mIconMoveList = new ArrayList<>();
    private int mCheckSpacePrevX = -1, mCheckSpacePrevY = -1; //ACTION_MOVE에 따른 같은위치 중복 체크 방지
    private Point mCheckSpaceViewSize; //빈공간 체크가 발생하였을때 해당 뷰의 크기저장

    private static BitmapDrawable mChangeDrawable = null;

    public static final int NOT_FOUND = 0; //빈공간 없음
    public static final int FOUND = 1;  //빈공간 있음
    public static final int STAY_STATE = 2;  //현상태 유지

    public CellLayout(Context context) {
        this(context, null);
    }
    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

        mCheckSpaceViewSize = new Point(0, 0);
        mPaint = new Paint(); //페이지 편집 모드시 배경 화면 페인터
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAxisX = getResources().getInteger(R.integer.cell_layout_x_axis); //가로 격자 개수
        mAxisY = mAxisX * 2; //세로의 길이는 X따라 가변적이기 때문에 2배로 버퍼 설정

        mMap = new IconView[mAxisX][mAxisY];
        mDummyMap = new IconView[mAxisX][mAxisY];
        if(mChangeDrawable == null)
            mChangeDrawable = (BitmapDrawable) AppCompatResources.getDrawable(getContext(), R.drawable.page_change);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);

        if(isPageMode){ //페이지 모드가 활성화 되어 있다면 zoom out 시킴
            widthSpecSize *= mScale;
            heightSpecSize = (int) (widthSpecSize * mRatio);//일반 모드일때의 비율로 계산
        }
        else // 가로,세로 화면 비율과 동일한 비율로 격자 생성
            mAxisY = (int) Math.ceil(heightSpecSize / (widthSpecSize/mAxisX));

        final int parentPadding = ((ViewGroup)getParent()).getPaddingLeft(); //부모뷰 페딩
        final int count = getChildCount(); //자식의 개수

        //1개의 격자 크기 저장
        mCellWidth = (widthSpecSize - getPaddingLeft()*4 - parentPadding*2)/mAxisX;
        mCellHeight = (heightSpecSize - getPaddingLeft()*4 - parentPadding*2)/mAxisY;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if(child instanceof IconView) {
                final IconView iconView = (IconView) child;
                final IconData data = iconView.getIconData();

                LayoutParams params = new LayoutParams(data.getX() * mCellWidth + getPaddingLeft() + parentPadding,
                        data.getY() * mCellHeight + getPaddingLeft() + parentPadding,
                        data.getCellWidth() * mCellWidth,
                        data.getCellHeight() * mCellHeight);
                iconView.setLayoutParams(params);  //아이콘 위치 및 크기 저장

                if(data.getType() == IconData.TYPE_WIDGET) { //위젯 타입이라면
                    int childWidthMeasureSpec;
                    int childheightMeasureSpec;

                    if(isPageMode) { //페이지 편집 모드일땐 scale를 적용
                        childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec((int) (params.width * (2-mScale)), View.MeasureSpec.EXACTLY);
                        childheightMeasureSpec = View.MeasureSpec.makeMeasureSpec((int) (params.height* (2-mScale)), View.MeasureSpec.EXACTLY);
                        iconView.setWidgetScale(params.width, params.height, mScale + 0.04f);
                    }
                    else { //아닐 경우 원래 크기
                        childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY);
                        childheightMeasureSpec = View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec.EXACTLY);
                        iconView.setWidgetScale(0, 0, 1.0f);
                    }

                    iconView.measure(childWidthMeasureSpec, childheightMeasureSpec); //measure로 자식 뷰의 크기를 잡아줌
                }

            }
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == View.VISIBLE) {
                if(child instanceof IconView) {
                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    child.layout(params.x, params.y, params.x + params.width, params.y + params.height);
                }
            }
        }
    }


    @Override
    //터치 좌표에서 아이템 꺼내기
    public View pick(int x, int y) {
        final IconView iconView = find(x, y);
        if(iconView != null){
            IconData data = iconView.getIconData();
            setMapData(mMap, data.getX(), data.getY(), data.getCellWidth(), data.getCellHeight(), null, false);
            removeView(iconView);
            return iconView;
        }

        return null;
    }

    @Override
    //터치 좌표에 아이템 넣기
    public boolean drop(View v, int x, int y, boolean result) {
        final IconView iconView = (IconView) v;
        final IconData data = iconView.getIconData();
        final int[] cellXY = convertXY(x, y, data.getCellWidth(), data.getCellHeight());

        if(cellXY[0] >= mAxisX || cellXY[1] >= mAxisY)
            return false;

        if(result) { //빈 공간 탐색이 성공일 경우 dummy와 바꾸기
            copyMap(mDummyMap, mMap, true);
            data.setX(cellXY[0]);
            data.setY(cellXY[1]);
            setMapData(mMap, data.getX(), data.getY(), data.getCellWidth(), data.getCellHeight(), null, false); //다음 액션을 위해 비우기
            return true;
        }

        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if(isPageMode){
            //캐싱된 Drawable이 있다면 그린다.
            if(mIconCacheDrawable != null && mIconCacheDrawable.getBitmap() != null && !mIconCacheDrawable.getBitmap().isRecycled()) {
                final BlurMaskFilter blur = new BlurMaskFilter(5, BlurMaskFilter.Blur.OUTER);
                final int[] offsetXY = new int[2];
                final int parentPadding = ((ViewGroup)getParent()).getPaddingLeft();
                final int x = mCheckSpacePrevX * mCellWidth + getPaddingLeft() + parentPadding;
                final int y = mCheckSpacePrevY * mCellHeight + getPaddingLeft() + parentPadding;

                mPaint.setColor(Color.WHITE);
                mPaint.setAlpha(255);
                mPaint.setMaskFilter(blur);

                Bitmap outerBitmap = mIconCacheDrawable.getBitmap().extractAlpha(mPaint, offsetXY);
                canvas.drawBitmap(outerBitmap, x, y, mPaint);
                outerBitmap.recycle();
            }

            if(mFoucsType == BaseCellLayout.FOCUS_CHANGED){
                if(mChangeDrawable != null && mChangeDrawable.getBitmap() != null && !mChangeDrawable.getBitmap().isRecycled()) {
                    final Bitmap b = mChangeDrawable.getBitmap();
                    final int left = getWidth()/2 - b.getWidth()/2;
                    final int top = getHeight()/2 - b.getHeight()/2;
                    canvas.drawBitmap(b, left, top, mPaint);
                }
            }
        }
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
        }
    }

    public void deleteIconByPackageName(String packageName){
        final int count = getChildCount();
        for (int i = count-1; i >=0; i--) {
            final View view = getChildAt(i);
            if(view instanceof IconView){
                IconData data = ((IconView)view).getIconData();
                if(data.getPackageName().equalsIgnoreCase(packageName)) {
                    removeView(view);
                }
            }
        }
        invalidate();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setVisibility(visibility);
        }
    }

    //부모 뷰의 canvas에 미니맵을 그린다
    public void drawGuideCell(Canvas canvas, int left, int top, int width, int height, Paint paint){
        final int cellWidth = width/mAxisX;
        final int cellHeight = height/mAxisY;

        int cellLeft, cellTop;

        paint.setColor(Color.argb(222, 255, 255, 255));

        IconView[][] map;

        if(mCheckSpacePrevX != -1 && mCheckSpacePrevY != -1){ //빈공간 탐색이 일어나고 있는 경우 아이콘 더미로 업데이트
            cellLeft = left + mCheckSpacePrevX * cellWidth;
            cellTop = top + mCheckSpacePrevY * cellHeight;
            canvas.drawRect(cellLeft, cellTop, cellLeft+cellWidth*mCheckSpaceViewSize.x, cellTop+cellHeight*mCheckSpaceViewSize.y, paint);

            map = mDummyMap;
        }
        else //아닐 경우 원래 맵으로 그림
            map = mMap;

        for(int i=0; i<mAxisX; i++) {
            for (int j = 0; j<mAxisY; j++) {
                if(map[i][j] != null) {
                    cellLeft = left + i * cellWidth;
                    cellTop = top + j * cellHeight;
                    canvas.drawRect(cellLeft, cellTop, cellLeft+cellWidth, cellTop+cellHeight, paint);
                }
            }
        }

    }

    //해당 위치에 push
    public void push(IconView iconView) {
        IconData data = iconView.getIconData();

        if(isEmpty(mMap, data.getX(), data.getY(), data.getCellWidth(), data.getCellHeight())){ //해당 공간이 비었다면
            setMapData(mMap, data.getX(), data.getY(), data.getCellWidth(), data.getCellHeight(), iconView, false);
            addView(iconView);
        }
    }

    //현재 위치에 빈공간 확인
    public int checkEmptySpace(View v, int x, int y){
        final IconView pickIconView = (IconView) v;
        final IconData pickData = pickIconView.getIconData();
        final int[] cellXY = convertXY(x, y, pickData.getCellWidth(), pickData.getCellHeight()); //Grid 좌표 형으로 바꿈

        if(pickData.getType() == IconData.TYPE_SCREEN) {
            setFocus(BaseCellLayout.FOCUS_CHANGED);
            return STAY_STATE;
        }

        if(mCheckSpacePrevX == cellXY[0] && mCheckSpacePrevY == cellXY[1]) //이동이 일어났는지 확인
            return STAY_STATE;

        cancelMoveAnimation(true); //이전 애니메이션 효과가 있다면 모두 제자리로

        if(cellXY[0] + pickData.getCellWidth()-1 >= mAxisX || cellXY[1] + pickData.getCellHeight()-1 >= mAxisY ||
                cellXY[0] < 0 || cellXY[1] < 0) //현재 좌표에 뷰가 들어가지 못한다면
            return NOT_FOUND;

        //이전 Grid위치 저장
        mCheckSpacePrevX = cellXY[0];
        mCheckSpacePrevY = cellXY[1];
        mCheckSpaceViewSize.x = pickData.getCellWidth();  //미니 맵에서 크기 계산용
        mCheckSpaceViewSize.y = pickData.getCellHeight();

        copyMap(mMap, mDummyMap, false); //더미 맵에 실제 데이터 복사

        for (int i = cellXY[0]; i < cellXY[0] + pickData.getCellWidth(); i++) {
            for (int j = cellXY[1]; j < cellXY[1] + pickData.getCellHeight(); j++) {
                if (i >= mAxisX || j >= mAxisY || i < 0 || j < 0) //탐색영역이 grid를 벗어났으면 무시
                    continue;

                final IconView checkIconView = mDummyMap[i][j];
                if (checkIconView == null || checkIconView == pickIconView) //현재 공간이 빈공간이거나 pick한 뷰라면 무시
                    continue;

                final IconData checkIconData = checkIconView.getIconData();

                //맵에서 제외
                setMapData(mDummyMap, checkIconData.getX(), checkIconData.getY(), checkIconData.getCellWidth(), checkIconData.getCellHeight(), null, false);

                //픽된 데이터 넣기
                setMapData(mDummyMap, cellXY[0], cellXY[1], pickData.getCellWidth(), pickData.getCellHeight(), pickIconView, true);

                //빈공간 검사
                Point point = getEmpty(mDummyMap, checkIconData.getX(), checkIconData.getY(), checkIconData.getCellWidth(), checkIconData.getCellHeight());
                if (point != null) { //이동 할수 있는 좌표가 있다면
                    final int padding = ((ViewGroup) getParent()).getPaddingLeft() + getPaddingLeft();
                    checkIconView.setMoveAnimation(point.x * mCellWidth + padding, point.y * mCellHeight + padding); //애니메이션 설정
                    mIconMoveList.add(checkIconView); //애니메이션 목록 리스트 업

                    //새로 찾아낸 공간에 맵핑
                    setMapData(mDummyMap, point.x, point.y, checkIconData.getCellWidth(), checkIconData.getCellHeight(), checkIconView, false);
                } else { //빈공간이 없으면 복귀
                    return NOT_FOUND;
                }
            }
        }

        mIconCacheDrawable = pickIconView.getIconCache();
        startMoveAnimation(); //애니메이션 시작

        return FOUND;
    }

    //1격자 아이콘 크기 반환
    public int[] getIconSize(){
        return new int[]{mCellWidth, mCellHeight};
    }


    //맵 데이터 설정 (isNullChek : 해당 위치에 null값을 확인할지 여부)
    private void setMapData(View[][] map, int x, int y, int width, int height, View v, boolean isNullCheck){
        for(int i=x; i<x+width; i++){
            for(int j=y; j<y+height; j++){
                if(i<0 || j<0 || i>=mAxisX || j>=mAxisY)
                    continue;

                if(!isNullCheck || map[i][j] == null)
                    map[i][j] = v;
            }
        }
    }

    //빈공간 인지 확인
    private boolean isEmpty(IconView[][] map, int x, int y, int width, int height){
        if(x+width > mAxisX || y+height > mAxisY)
            return false;

        for(int i=x; i<x+width; i++){
            for(int j=y; j<y+height; j++){
                if(map[i][j] != null) {
                    return false;
                }
            }
        }

        return true;
    }

    //src아이템 좌표를 des에 복사
    private void copyMap(IconView[][] src, IconView[][] des, boolean isPositionUpdate){
        final HashSet<IconView> checkSet = new HashSet<>();
        for(int i=0; i<mAxisX; i++) {
            for (int j = 0; j < mAxisY; j++) {
                des[i][j] = src[i][j];
                if (des[i][j] != null && !checkSet.contains(des[i][j]) && isPositionUpdate) {
                    des[i][j].getIconData().setX(i);
                    des[i][j].getIconData().setY(j);
                    checkSet.add(des[i][j]);
                }
            }
        }
        checkSet.clear();
    }


    //이동을 위한 빈자리 탐색
    private Point getEmpty(IconView[][] map, int x, int y, int width, int height){
        Point point = null;
        int rangeMax = (mAxisX > mAxisY) ? mAxisX: mAxisY;

        //해당 지점으로부터 원형 탐색
        for(int range=1; range<rangeMax; range++){
            for (int j=y-range; j<=y+range; j++) {
                for(int i=x-range; i<=x+range; i++) {
                    if(i == x-range || i == x+range || j == y-range || j == y+range) {  //테두리 검출
                        if (i >= 0 && j >= 0 && i < mAxisX && j < mAxisY) { //범위 검사
                            if (isEmpty(map, i, j, width, height)) {
                                return new Point(i, j);
                            }
                        }
                    }
                }
            }
        }

        return point;
    }

    //애니메이션 시작
    private void startMoveAnimation(){
        for(int i=0; i<mIconMoveList.size();i++){
            final IconView iconView = mIconMoveList.get(i);
            iconView.startMoveAnimation();
        }
    }

    //애니메이션 취소
    public void cancelMoveAnimation(boolean animation){
        mIconCacheDrawable = null;
        mCheckSpacePrevX = -1;
        mCheckSpacePrevY = -1;
        mCheckSpaceViewSize.x = 0;
        mCheckSpaceViewSize.y = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof IconView) {
                ((IconView)view).cancelMoveAnimation(animation);
                view.setAlpha(1);
            }
        }
        mIconMoveList.clear();
        invalidate();
    }

    //터치 좌표에서 아이템 가져오기
    public IconView find(int x, int y) {
        final int[] cellXY = convertXY(x, y, 0, 0);

        if(cellXY[0] >= mAxisX || cellXY[1] >= mAxisY || cellXY[0] < 0 || cellXY[1] < 0)
            return null;

        mCheckSpacePrevX = -1;
        mCheckSpacePrevY = -1;
        mCheckSpaceViewSize.x = 0;
        mCheckSpaceViewSize.y = 0;

        final IconView iconView = mMap[cellXY[0]][cellXY[1]];
        if(iconView != null)
            return iconView;

        return null;
    }

    //터치 좌표계를 Grid좌표계로 변환
    public int[] convertXY(int x, int y, int cellSizeX, int cellSizeY){
        final int[] location = new int[2];
        getLocationOnScreen(location);

        int left = ((ViewGroup)getParent()).getPaddingLeft() + getPaddingLeft() + location[0];
        int top =  ((ViewGroup)getParent()).getPaddingTop() + getPaddingTop() + location[1];

        if(isPageMode){
            left += mCellWidth*cellSizeX/2 - mCellWidth/2;
            top += mCellHeight*cellSizeY/2 - mCellHeight/2;
        }

        return new int[]{ (x-left)/mCellWidth, (y-top)/mCellHeight};
    }

    //Icon 좌표용 x, y가 추가 된 layoutparam
    public static class LayoutParams extends ViewGroup.LayoutParams{
        int x, y;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int x, int y, int width, int height) {
            this(width, height);
            this.x = x;
            this.y = y;
        }
    }
}
