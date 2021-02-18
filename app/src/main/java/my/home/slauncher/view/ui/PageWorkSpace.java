package my.home.slauncher.view.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;

import my.home.slauncher.Default;
import my.home.slauncher.R;
import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.IconData;
import my.home.slauncher.database.LauncherDBHelper;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.tools.Utility;
import my.home.slauncher.view.activity.MainActivity;
import my.home.slauncher.view.drawable.FastBitmapDrawable;
import my.home.slauncher.view.effect.ScreenEffector;
import my.home.slauncher.view.interfaces.DragAndDropEvent;
import my.home.slauncher.view.interfaces.TouchEvent;

/**
 * Created by ShinSung  on 2017-08-31.
 * 종류 : 모든 페이지를 관리하는 클래스
 * 내용 : CellLayout을 자식으로 가지며 페이지에 대한 스크롤 효과를 제어
 * LauncherConotroller > PageWorkSpace > CellLayout > IconView
 */

public class PageWorkSpace extends ViewGroup implements TouchEvent, DragAndDropEvent {
    public static final int ANIMATION_DELAY = 250; //0.25초

    private LauncherModel mLauncherModel;
    private ScreenEffector mScreenEffector;
    private WideBackgroundLayout mWideBackgroundLayout;
    private IndicatorView mIndicatorView;
    private Handler mMainHandler;

    private VelocityTracker mVelocityTracker; //터치 이동 속도 측정
    private Scroller mScroller;
    private PointF mPrevPoint;

    private Paint mPaint;

    private int mTouchStartX = 0;
    private int mCurrentPageIndex = 0, mPrevPageIndex = 0;

    private boolean isPageCustomMode = false;

    private float mScaleValue;

    public PageWorkSpace(Context context) {
        super(context, null);
    }

    public PageWorkSpace(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PageInfo, 0, 0);
        mScaleValue = typedArray.getFloat(R.styleable.PageInfo_scaleValue, 0.8f); //줌아웃에 대한 값 설정

        init();
    }

    private void init(){
        mScroller = new Scroller(getContext());
        mPrevPoint = new PointF();
        mPaint = new Paint();
        createPage();
    }

    //페이지 로드
    private void createPage(){
        final int pageCnt = LauncherDBHelper.inst().getPageCnt();

        if(pageCnt == 0){ //페이지가 없으면
            CellLayout cellLayout = new CellLayout(getContext()); //1개 생성
            addView(cellLayout);
            LauncherDBHelper.inst().addPage();
        }
        else{ //페이지가 있다면
            for(int i=0; i<pageCnt; i++){ //있는 개수 만큼 생성
                CellLayout cellLayout = new CellLayout(getContext());
                addView(cellLayout);
            }
        }

        BaseCellLayout emptyLayout = new BaseCellLayout(getContext());
        emptyLayout.setAdderScreen(true);
        addView(emptyLayout);
    }

    //페이지 정보를 DB로부터 읽어온다
    public void loadData(){
        final ArrayList<Integer> pageKeyList = LauncherDBHelper.inst().getPageKey(); //페이지 키 리스트
        for(int idx=0; idx<pageKeyList.size(); idx++) {
            View v = getChildAt(idx);
            if(v instanceof CellLayout) {
                final int key = pageKeyList.get(idx);
                final ArrayList<IconData> iconDataList = LauncherDBHelper.inst().getIconData(key); //해당 페이지키에 해당하는 아이콘을 검색
                for (IconData iconData : iconDataList) {
                    IconView iconView = createIconView(iconData);

                    if (iconData.getType() == IconData.TYPE_APPS) { //앱스 아이콘이면 모델에서 아이콘을 가져와 그린다
                        final int modelIdx = mLauncherModel.getAppIndexByPackageName(iconData.getPackageName(), iconData.getComponentName());
                        if(modelIdx != -1) { //TODO:삭제된 녀석에 대한 정보 제거 필요
                            ApplicationData data = mLauncherModel.getAppsData(modelIdx);
                            iconData.setDrawable(data.getIconDrawable());
                        }
                    } else if (iconData.getType() == IconData.TYPE_WIDGET) { //위젯이면 위젯호스트뷰생 성
                        iconView.addWidgetHostView();
                    }

                    ((CellLayout) v).push(iconView);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        final float ratio = heightSpecSize/(float)widthSpecSize;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if(view instanceof CellLayout) {
                CellLayout cellLayout = (CellLayout) view;
                cellLayout.measure(widthMeasureSpec, heightMeasureSpec);
                cellLayout.setScaleValue(mScaleValue, ratio); //줌아웃시 값 설정
            }
            else if(view instanceof BaseCellLayout){
                BaseCellLayout emptyLayout = (BaseCellLayout) view;
                emptyLayout.measure(widthMeasureSpec, heightMeasureSpec);
                emptyLayout.setScaleValue(mScaleValue, ratio);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int padding = getPaddingLeft();
        int childLeft = 0;

        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, padding, childLeft+childWidth, padding+child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }

        mWideBackgroundLayout.setBackgroundPosition(mCurrentPageIndex, 0, getChildCount(), isPageCustomMode, false); //배경 업데이트
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        for (int i = 0; i < getChildCount(); i++) {
            drawChild(canvas, getChildAt(i), 100); // 차일드 뷰들을 그린다.
        }
        drawGuide(canvas);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }

        if(!isPageCustomMode && mScreenEffector != null){
            mScreenEffector.dragging(getScrollX());
        }
    }

    //++++++++++ 터치 이벤트 ++++++++++++//
    @Override
    public void touchStart(MotionEvent event) {
        mWideBackgroundLayout.setBackgroundPosition(mCurrentPageIndex, 0, getChildCount(), isPageCustomMode, false);

        if (!mScroller.isFinished()) //스크롤 중이면 스크롤을 멈춤
            mScroller.abortAnimation();

        mPrevPoint.set(event.getX(), event.getY()); //현재 좌표 저장
        mTouchStartX = (int) mPrevPoint.x;

        invalidate();//갱신
    }

    @Override
    public void touchIng(MotionEvent event) {
        int moveX = (int) (event.getX() - mPrevPoint.x); //이동 거리

        mWideBackgroundLayout.setBackgroundPosition(mCurrentPageIndex, (int) (event.getX()-mTouchStartX), getChildCount(), isPageCustomMode, false); //배경 업데이트

        scrollBy(-moveX, 0); //이동
        invalidate();//갱신

        mPrevPoint.set(event.getX(), event.getY()); //현재 좌표 저장

        //페이지 편집모드가 아니면 드래그 애니메이션 적용
        if(!isPageCustomMode && mScreenEffector != null) {
            enableChildrenCache(true); //자식 뷰에 드로잉캐시를 켠다
            mScreenEffector.dragStart(getChildAt(mCurrentPageIndex), mCurrentPageIndex, mScroller.getCurrX()); //페이지 드래그 효과 시작
        }
    }

    @Override
    public void touchEnd(MotionEvent event) {
        cancelIconAnimation(false); //아이콘 애니메이션 끄기

        if(mScreenEffector.getModeState() != ScreenEffector.MODE_ZOOM) { //현재 이펙트가 Zoom모드라면 스크롤은 하지 않음
            int velocity = 0;
            if(mVelocityTracker != null) {
                mVelocityTracker.computeCurrentVelocity(100);
                velocity = (int) Math.abs(mVelocityTracker.getXVelocity()); // x축 이동 속도를 구함
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            final int width = isPageCustomMode ? getChildAt(0).getWidth() : getWidth();
            int gap = getScrollX() - mCurrentPageIndex * width; // 드래그 이동 거리 체크

            int pageCnt = getChildCount();
            if (!isPageCustomMode)
                pageCnt -= 1;

            if (((gap < 0 && velocity > Default.SNAP_VELOCITY) || gap < -width / 2) && mCurrentPageIndex > 0)
                mCurrentPageIndex--;
            else if (((gap > 0 && velocity > Default.SNAP_VELOCITY) || gap > width / 2) && mCurrentPageIndex < pageCnt - 1)
                mCurrentPageIndex++;

            mIndicatorView.setPosition(mCurrentPageIndex, getChildCount()-1, !isPageCustomMode);

            final int move = width * mCurrentPageIndex - getScrollX() - (int) (isPageCustomMode ? getWidth() * ((1.0f - mScaleValue) / 2) : 0);
            mScroller.startScroll(getScrollX(), 0, move, 0, ANIMATION_DELAY); //해당 위치로 이동

            invalidate();
        }

        mWideBackgroundLayout.setBackgroundPosition(mCurrentPageIndex, 0, getChildCount(), isPageCustomMode, true);

        if(!isPageCustomMode && mScreenEffector != null)
            mScreenEffector.dragStop(mCurrentPageIndex, ANIMATION_DELAY); //페이지 드래그 효과 제거

        enableChildrenCache(false);//캐쉬 off

    }

    //페이지 픽의 경우
    @Override
    public View pick(int x, int y) {
        final View curView = getChildAt(mCurrentPageIndex);
        IconView iconView  =null;

        if(curView instanceof CellLayout){
            int[] size = new int[2];
            size[0] = (int) (curView.getWidth() * 0.4f);
            size[1] = (int) (curView.getHeight() * 0.4f);
            FastBitmapDrawable drawable = Utility.getCacheDrawable(curView, size,1);

            if(drawable != null){
                iconView = new IconView(getContext());
                IconData iconData = new IconData();
                iconData.setPageKey(mCurrentPageIndex);
                iconData.setType(IconData.TYPE_SCREEN);
                iconData.setDrawable(drawable);
                iconData.setCacheView(true);
                iconView.setIconData(iconData);
                iconView.setSize(0, 0, size[0], size[1]);
                curView.setAlpha(0.5f);

                mPrevPageIndex = mCurrentPageIndex;
            }
        }

        return iconView;
    }

    @Override
    public boolean drop(View v, int x, int y, boolean result) {
        if(v instanceof IconView){ //드랍되는 종류가 아이콘이면
            final IconView iconView = (IconView) v;
            final IconData iconData = iconView.getIconData();

            iconData.setDummyView(false);

            if(iconData.getType() == IconData.TYPE_SCREEN){
                CellLayout cellLayout = (CellLayout) getChildAt(iconData.getPageKey());
                cellLayout.setAlpha(1);  //상태복귀

                final View targetView = getChildAt(mPrevPageIndex); //이전 인덱스 화면 얻기
                removeView(targetView); //지우고
                addView(targetView, mCurrentPageIndex); //새로운 인덱스에 넣기

                LauncherDBHelper.inst().changePage(mPrevPageIndex, mCurrentPageIndex);
            }
            else {
                CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPageIndex);
                if (cellLayout != null) { //페이지가 있다면
                    final boolean isSuccess = cellLayout.drop(v, x, y, result); //놓일 영역을 자식 뷰에 확인

                    if (isSuccess) { //놓일 자리가 있다면
                        if (iconData.getType() == IconData.TYPE_WIDGET && iconData.isCacheView()) { //위젯이면 생성 요청을 보냄
                            Message msg = new Message();
                            msg.what = MainActivity.HANDLE_PICK_WIDGET_CREATE;
                            msg.obj = iconData;
                            mMainHandler.sendMessage(msg);
                        } else //그외 아이템이면 해당 자리에 넣기
                            push((IconView) v);
                    }

                    return isSuccess;
                }
            }
        }

        return false;
    }

    //드래그 속도 측정 켜기
    public void setVelocityTracker(MotionEvent event){
        if (mVelocityTracker == null) //스크롤 속도 체크를 위한 객체 생성
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);
    }

    //터치 이펙트 주기
    public void setTouchEffect(int x, int y, int type){
        if(isPageCustomMode) { //페이지 편집 모드면 스크린 터치 효과를 줌
            View v = getChildAt(mCurrentPageIndex);
            if (v instanceof BaseCellLayout)
                ((BaseCellLayout) v).setFocus(type);
        }
        else {
            final IconView iconView = (IconView) getChild(x, y); //터치 영역 검사
            if (iconView != null) {  //찾은 아이콘이 있으면
                final IconData data = iconView.getIconData();
                if (data != null && data.getType() != IconData.TYPE_WIDGET) //위젯이 아니면 터치효과 주기
                    iconView.setAlpha(0.5f);
            }
        }
    }

    //패키지 이름으로 아이콘 삭제
    public void deleteIconByPackageName(String packageName){
        for(int i=0; i<getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof CellLayout) {
                ((CellLayout)view).deleteIconByPackageName(packageName);
            }
        }
    }

    //페이지 추가가 일어났는지 확인
    public void checkAddedPage(){
        View v = getChildAt(mCurrentPageIndex);
        if(!(v instanceof CellLayout)){ //EmptyLayout이면 페이지가 추가됨을 감지
            CellLayout cellLayout = new CellLayout(getContext());
            addView(cellLayout, mCurrentPageIndex);
            LauncherDBHelper.inst().addPage();
        }
    }

    //드래그 취소 제자리 복귀
    public void cancelDragIcon(IconView iconView){
        final IconData iconData = iconView.getIconData();

        if(LauncherDBHelper.inst().isIconExisted(iconData.getKey())) { //기존에 저장되어있던 아이콘이면 원래 자리로 복귀
            CellLayout cellLayout;
            if (mCurrentPageIndex != mPrevPageIndex)
                cellLayout = (CellLayout) getChildAt(mPrevPageIndex);
            else
                cellLayout = (CellLayout) getChildAt(mCurrentPageIndex);

            if(cellLayout != null)
                cellLayout.push(iconView);
        }
    }

    //아이콘 삭제
    public void deleteItem(IconView iconView){
        final IconData iconData = iconView.getIconData();

        if(iconData.getType() == IconData.TYPE_SCREEN) { //화면 삭제이면
            final View deleteView = getChildAt(mPrevPageIndex);
            int childCnt = getChildCount() - 1;

            if(childCnt > 1){ //자식 뷰가 1개 이상있을경우에만 지운다
                if(deleteView != null)
                    removeView(deleteView);
                int pageKey = LauncherDBHelper.inst().getPageKey(mPrevPageIndex);
                LauncherDBHelper.inst().deletePage(pageKey);
                if(mCurrentPageIndex == getChildCount()-1)
                    mCurrentPageIndex--;
            }
            else
                deleteView.setAlpha(1);  //상태복귀
        }
        else{ //일반 아이콘이면
            LauncherDBHelper.inst().deleteData(iconData.getKey()); //DB에서 삭제
            if (iconData.getType() == IconData.TYPE_WIDGET) //위젯 타입이면 아이디 삭제
                MainActivity.getAppWidgetHost().deleteAppWidgetId(iconData.getWidgetId());
            else if(iconData.getType() == IconData.TYPE_APPS && iconData.isDummyView()){
                Message msg = new Message();
                msg.what = MainActivity.HANDLE_DELETE_APP;
                msg.obj = iconData.getPackageName();
                mMainHandler.sendMessage(msg);
            }
        }
    }

    //드래그 효과 레이어 설정
    public void setDragLayer(ScreenEffector dragEffector){
        mScreenEffector = dragEffector;
    }

    //배경 드로잉 레이어 설정
    public void setBackgroundLayout(WideBackgroundLayout wideBackgroundLayout){
        mWideBackgroundLayout = wideBackgroundLayout;
    }

    //인디케이터 뷰를 설정
    public void setIndicator(IndicatorView indicatorView){
        mIndicatorView = indicatorView;
    }

    //런처 모델 설정
    public void setModel(LauncherModel launcherModel){
        mLauncherModel = launcherModel;
    }

    //메인 핸들러 설정
    public void setMainHandler(Handler mainHandler){
        mMainHandler = mainHandler;
    }

    //1개의 아이콘 사이즈를 반환
    public int[] getIconSize(){
        CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPageIndex);
        if(cellLayout != null)
            return cellLayout.getIconSize();
        return new int[]{0, 0};
    }

    //스크롤 센터 정렬
    public void scrollToCenter(int delay){
        mScroller.abortAnimation();

        int width = isPageCustomMode ? (int) (getWidth() * mScaleValue) : getWidth();
        int move = width * mCurrentPageIndex - getScrollX() - ((int)(isPageCustomMode ? getWidth() * ((1.0f - mScaleValue)/2)  : 0));
        mScroller.startScroll(getScrollX(), 0, move, 0, delay);

        invalidate();
    }

    //페이지 에디터 모드 설정
    public void setPageEditMode(boolean editMode){
        if(isPageCustomMode == editMode)
            return ;

        isPageCustomMode = editMode;

        //모든 뷰를 에디터 모드로 갱신
        for(int i=0; i<getChildCount(); i++){
            View view = getChildAt(i);
            if(view instanceof CellLayout) {
                CellLayout cellLayout = (CellLayout) view;
                cellLayout.setPageEditingMode(isPageCustomMode);
                cellLayout.requestLayout();
            }
            else if(view instanceof BaseCellLayout){
                BaseCellLayout emptyLayout = (BaseCellLayout) view;
                emptyLayout.setPageEditingMode(isPageCustomMode);
                emptyLayout.requestLayout();
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    public int[] getPageSize(){
        if(getChildCount() > 0) {
            final View v = getChildAt(0);
            return new int[]{v.getWidth(), v.getHeight()};
        }
        else
            return new int[]{0,0};
    }

    //현재 화면 저장
    public void savePageCache(){
        mScreenEffector.savePageCache(this);
    }

    //Zoom 효과에 대한 초기화
    public void initZoomEffect(int transY){
        mScreenEffector.initZoomEffect(this, transY);
    }

    //줌인 줌 인/아웃 효과 시작
    public void startZoomEffect(int oldWidth, int oldHeight, int changedWidth, int changedHeight, int topMargin){
        mScreenEffector.startZoomEffect(
                new int[]{oldWidth, oldHeight}, //시작 크기
                new int[]{changedWidth, changedHeight}, //도착 크기
                topMargin, //top에 대한 마진
                mScaleValue); //최소 scale 값
    }

    //터치 좌표를 기준으로 아이템 찾기
    public View getChild(int x, int y){
        CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPageIndex);
        if(cellLayout != null) {
            View v = cellLayout.find(x, y);
            return v;
        }

        return null;
    }

    //터치 좌표를 기준으로 자식 뷰에서 아이템을 꺼냄
    public View getChildPick(int x, int y){
        View view = getChildAt(mCurrentPageIndex);
        if(view instanceof CellLayout) {
            CellLayout cellLayout = (CellLayout)view;
            if (cellLayout != null) {
                View v = cellLayout.pick(x, y);
                if (v != null)
                    mPrevPageIndex = mCurrentPageIndex;

                return v;
            }
        }

        return null;
    }

    //드래그 중 공간 확인
    public int checkSpace(View v, int x, int y){
        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        final View childView = getChildAt(mCurrentPageIndex);
        final int left = (displayMetrics.widthPixels - childView.getWidth())/2;
        final int right = left + childView.getWidth();
        int pageCnt = getChildCount()-1;

        //왼쪽으로 페이지 이동
        if(x < left && mCurrentPageIndex > 0 && mScroller.isFinished()){
            clearCellLayout(childView, true);
            mCurrentPageIndex--;
            scrollToCenter(500);
            mIndicatorView.setPosition(mCurrentPageIndex, getChildCount()-1, !isPageCustomMode);
            mWideBackgroundLayout.setBackgroundPosition(mCurrentPageIndex, 0, getChildCount(), isPageCustomMode, true); //배경 업데이트
        }
        //오른쪽으로 페이지 이동
        else if(x > right && mCurrentPageIndex < pageCnt-1 && mScroller.isFinished()){
            clearCellLayout(childView, true);
            mCurrentPageIndex++;
            scrollToCenter(500);
            mIndicatorView.setPosition(mCurrentPageIndex, getChildCount()-1, !isPageCustomMode);
            mWideBackgroundLayout.setBackgroundPosition(mCurrentPageIndex, 0, getChildCount(), isPageCustomMode, true); //배경 업데이트
        }
        //아이콘 놓일 자리 확인
        else{
            if(childView instanceof CellLayout) {
                int state = ((CellLayout) childView).checkEmptySpace(v, x, y);
                if(state == CellLayout.FOUND)
                    invalidate();

                return state;
            }
        }
        return CellLayout.STAY_STATE;
    }

    //전체 아이콘 애니메이션 효과 끄기
    public void cancelIconAnimation(boolean isRunAnimation) {
        for (int i = 0; i < getChildCount(); i++) {
            clearCellLayout(getChildAt(i), isRunAnimation);
        }
    }

    //줌 아웃 설정 값 반환
    public float getScaleValue(){
        return mScaleValue;
    }


    //위젯 등록 요청 -> Request 완료 -> 추가
    public void addWidgetByRequest(IconView iconView){
        iconView.addWidgetHostView();
        push(iconView);
    }

    //페이지 모드 설정값 확인
    public boolean isPageCustomMode(){
        return isPageCustomMode;
    }

    //CellLayout 상태 초기화
    private void clearCellLayout(View v, boolean isRunAnimation){
        if(v != null) {
            if (v instanceof CellLayout)
                ((CellLayout) v).cancelMoveAnimation(isRunAnimation);
            if (v instanceof BaseCellLayout)
                ((BaseCellLayout) v).setFocus(BaseCellLayout.FOCUS_NON);
        }

    }

    //자식 뷰에 추가 및 DB 저장
    private void push(IconView iconView){
        CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPageIndex);
        if(cellLayout != null) {
            final IconData iconData = iconView.getIconData();
            final int pageKey = LauncherDBHelper.inst().getPageKey(mCurrentPageIndex);
            iconData.setPageKey(pageKey); //현재 페이지 키를 설정하고
            LauncherDBHelper.inst().insertIconData(pageKey, iconData); //해당 페이지 키에 저장
            cellLayout.push(iconView);
        }
    }

    //자식 뷰에 Drawing cache 설정
    private void enableChildrenCache(boolean set) {
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if(view instanceof CellLayout) {
                final CellLayout layout = (CellLayout) view;
                layout.setChildrenDrawingCacheEnabled(set);
            }
        }
    }


    //아이콘 데이터를 기반으로 아이콘뷰 생성
    private IconView createIconView(IconData iconData){
        IconView iconView = new IconView(getContext());
        iconView.setIconData(iconData);
        return iconView;
    }

    //하단 가이드 이미지 그리기
    private void drawGuide(Canvas canvas){
        if(mScreenEffector.getModeState() == ScreenEffector.MODE_ZOOM)
            return ;

        final BaseCellLayout emptyLayout = (BaseCellLayout) getChildAt(0);
        final int guideTop = (int) (emptyLayout.getHeight() * (isPageCustomMode ? 1.02f : 0.99f));
        final int guideHeight = getHeight() - guideTop;

        if(isPageCustomMode){
            final int childCnt = getChildCount();
            final int padding = 50;
            final int cellWidth = (int) (guideHeight*((float)getWidth()/getHeight()));
            final int leftMargin = (int) (emptyLayout.getWidth() * (1.0f-mScaleValue)/2);
            final int scrollLeft = getScrollX() - leftMargin + getWidth()/2 + padding - cellWidth/2
                    - mCurrentPageIndex*cellWidth - mCurrentPageIndex*padding/2 + getPaddingLeft();
            final int top = guideTop+(guideHeight-guideHeight);

            mPaint.setStyle(Paint.Style.FILL);

            for(int i=0; i<childCnt; i++){
                final int left = scrollLeft + cellWidth*i + (padding/2*i) + padding/2;

                if(mCurrentPageIndex == i)
                    mPaint.setColor(Color.argb(120, 255, 255, 255));
                else
                    mPaint.setColor(Color.argb(30, 255, 255, 255));

                canvas.drawRect(left, top, left + cellWidth, guideTop + guideHeight, mPaint); //배경

                if(i == childCnt-1) { //추가 페이지
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    mPaint.setTextAlign(Paint.Align.CENTER);
                    mPaint.setColor(Color.argb(160, 255, 255, 255));
                    mPaint.setTextSize(45);
                    canvas.drawText("+", left + cellWidth/2, top + guideHeight/2, mPaint);
                }
                else {
                    View v = getChildAt(i);
                    if (v instanceof CellLayout) {
                        CellLayout cellLayout = (CellLayout) v;
                        cellLayout.drawGuideCell(canvas, left, top, cellWidth, guideHeight, mPaint);
                    }
                }
            }
        }
    }
}
