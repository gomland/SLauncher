package my.home.slauncher.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import my.home.slauncher.Default;
import my.home.slauncher.R;
import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.IconData;
import my.home.slauncher.database.LauncherDBHelper;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.tools.Utility;
import my.home.slauncher.view.activity.MainActivity;
import my.home.slauncher.view.drawable.FastBitmapDrawable;
import my.home.slauncher.view.effect.GlowView;
import my.home.slauncher.view.effect.ScreenEffector;
import my.home.slauncher.view.interfaces.TouchEvent;
import my.home.slauncher.view.ui.AllIconGirdView;
import my.home.slauncher.view.ui.AnimationLinearLayout;
import my.home.slauncher.view.ui.BaseCellLayout;
import my.home.slauncher.view.ui.BottomCellLayout;
import my.home.slauncher.view.ui.CellLayout;
import my.home.slauncher.view.ui.IconView;
import my.home.slauncher.view.ui.IndicatorView;
import my.home.slauncher.view.ui.MenuButton;
import my.home.slauncher.view.ui.PageWorkSpace;
import my.home.slauncher.view.ui.WideBackgroundLayout;

/**
 * Created by ShinSung on 2017-09-18.
 * 종류 : Acitivity와 View사이를 중계, 터치 이벤트 관리
 * Activity - LauncherConotroller > PageWorkSpace > CellLayout > IconView
 */

public class LauncherController extends FrameLayout implements View.OnClickListener, TouchEvent{
    //화면 상태 설정
    private final int SCREEN_MAIN = 0;
    private final int SCREEN_PAGE_EDIT = 1;
    private final int SCREEN_DRAG_MODE = 2;
    private final int SCREEN_APPS = 3;
    private final int SCREEN_WIDGET = 4;
    private int mPageState = SCREEN_MAIN; //화면 상태 저장

    //VIew 자원
    private PageWorkSpace mPageWorkSpace; //드래그 작업 공간
    private BottomCellLayout mBottomCelllayout; //하단 아이콘 영역

    private WideBackgroundLayout mWideBackgroundLayout; //배경 이미지 드로잉
    private ScreenEffector mScreenEffector;  //드래그시 페이지 애니메이션 효과 레이아웃
    private GlowView mAppsLayout;  //앱스,위젯 리스트 부모 뷰
    private AllIconGirdView mAppsGridView; //앱스,위젯 리스트 그리드 뷰
    private AnimationLinearLayout mLayoutBottomArea, mLayoutMenu, mLayoutTop; //하단, 메뉴, 상단 버튼 영역
    private FrameLayout mDragLayout; //드래그를 위한 투명 레이아웃

    //Activity 자원
    private LauncherModel mLauncherModel;
    private Handler mMainHandler;

    //아이콘이 픽 되었을때 저장하는 임시 변수
    private View mPickView = null;

    private int mEmptySearchState; //드래그 시 빈 공간 찾기 결과
    private boolean isScreenDrag = false; //화면 영역 드래그에 대한 플래그
    private boolean isIconMoving = false; //아이콘 드래그에 대한 플래그

    //ACTION DOWN이 발생했을 때 위치 저장
    private Point mPrevXy = new Point();

    public LauncherController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(LauncherModel launcherModel, Handler handler){
        mLauncherModel = launcherModel;
        mMainHandler = handler;

        initView();
    }

    private void initView() {
        mPageWorkSpace = findViewById(R.id.layout_page_workspace);
        mScreenEffector = findViewById(R.id.layout_drag);
        mWideBackgroundLayout = findViewById(R.id.layout_wide_background);
        mPageWorkSpace.setDragLayer(mScreenEffector);
        mPageWorkSpace.setBackgroundLayout(mWideBackgroundLayout);
        mPageWorkSpace.setIndicator((IndicatorView) findViewById(R.id.layout_indicator));
        mPageWorkSpace.setModel(mLauncherModel);
        mPageWorkSpace.setMainHandler(mMainHandler);

        mBottomCelllayout = findViewById(R.id.layout_bottom);
        mBottomCelllayout.setModel(mLauncherModel);

        mAppsLayout = findViewById(R.id.layout_apps);
        mAppsGridView = findViewById(R.id.layout_apps_grid);
        mAppsGridView.init(mLauncherModel, mMainHandler);

        mLayoutTop = findViewById(R.id.layout_top);
        mLayoutBottomArea = findViewById(R.id.layout_bottom_area);
        mLayoutMenu = findViewById(R.id.layout_menu);

        mDragLayout = findViewById(R.id.layout_drag_area);

        MenuButton menuButton = findViewById(R.id.widget_btn);
        menuButton.setDrawable((BitmapDrawable) AppCompatResources.getDrawable(getContext(), R.drawable.btn_widget));
        menuButton .setOnClickListener(this);

        MenuButton settingButton = findViewById(R.id.settings);
        settingButton.setDrawable((BitmapDrawable) AppCompatResources.getDrawable(getContext(), R.drawable.btn_setting));
        settingButton .setOnClickListener(this);

        MenuButton wallPaperButton = findViewById(R.id.wallpaper_btn);
        wallPaperButton.setDrawable((BitmapDrawable) AppCompatResources.getDrawable(getContext(), R.drawable.btn_wallpaper));
        wallPaperButton .setOnClickListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch(action){
            case MotionEvent.ACTION_DOWN:
                touchStart(event);
                break;
            case MotionEvent.ACTION_MOVE:
                touchIng(event);
                break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                touchEnd(event);
                return isScreenDrag;
        }

        return isIconMoving;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        mPageWorkSpace.setVelocityTracker(event);

        switch(action){
            case MotionEvent.ACTION_MOVE:
                touchIng(event);
                break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                touchEnd(event);
                break;
        }

        return true;
    }

    //++++++++++++터치 인터페이스++++++++++++++//
    @Override
    public void touchStart(MotionEvent event) {
        isScreenDrag = false;
        isIconMoving = false;

        View touchAreaView = getTouchArea((int) event.getY());
        if(touchAreaView == null) //Workspace 영역 인지 확인
            return ;
        else if(touchAreaView == mPageWorkSpace)
            mPageWorkSpace.setTouchEffect((int) event.getX(), (int) event.getY(), BaseCellLayout.FOCUS_PRESS);
        else if(touchAreaView == mBottomCelllayout)
            mBottomCelllayout.setTouchEffect((int) event.getX());

        if(mMainHandler.hasMessages(MainActivity.HANDLE_LONG_TOUCH)) //이전 롱터치 이벤트가 있다면 제거
            mMainHandler.removeMessages(MainActivity.HANDLE_LONG_TOUCH);

        sendLongTouchEvent(event); //롱터치 이벤트 발생
        mPrevXy.set((int) event.getX(), (int) event.getY()); //현재 좌표 저장

        mPageWorkSpace.touchStart(event); //터치이벤트 시작
    }

    @Override
    public void touchIng(MotionEvent event) {
        View touchAreaView = getTouchArea((int) event.getY());
        if(touchAreaView == null) //Workspace 영역 인지 확인
            return ;

        else if(touchAreaView instanceof AnimationLinearLayout)
            ((AnimationLinearLayout)touchAreaView).checkChildSelected((int) event.getX());

        if (Math.abs(mPrevXy.x - (int) event.getX()) > Default.TOUCH_CHECK_AREA ||
                Math.abs(mPrevXy.y - (int) event.getY()) > Default.TOUCH_CHECK_AREA) { //일정 범위 이상 움직였는지 확인
            if (!isIconMoving) //아이콘 드래그 상태가 아니라면
                mPageWorkSpace.touchIng(event); //페이지 드래깅
            else {  //아이콘 드래깅
                moveIconDrag(touchAreaView, (int) event.getX(), (int) event.getY());
                if(touchAreaView != mPageWorkSpace) //page workspace가 아니면 애니메이션 취소
                    mPageWorkSpace.cancelIconAnimation(true);
            }

            if (mMainHandler.hasMessages(MainActivity.HANDLE_LONG_TOUCH)) //이동이 일어났다면 롱터치 이벤트 제거
                mMainHandler.removeMessages(MainActivity.HANDLE_LONG_TOUCH);

            isScreenDrag = true;
        }
    }

    @Override
    public void touchEnd(MotionEvent event) {
        View view = getTouchArea((int) event.getY());

        //터치 이벤트 종료
        mPageWorkSpace.touchEnd(event);
        mBottomCelllayout.clearState();

        stopIconDrag(view, (int) event.getX(), (int) event.getY()); //드래그 종료

        if (mMainHandler.hasMessages(MainActivity.HANDLE_LONG_TOUCH)) { //HANDLE_LONG_TOUCH이 존재한다면 클릭으로 간주
           clickAction(view, event);
            mMainHandler.removeMessages(MainActivity.HANDLE_LONG_TOUCH);
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.widget_btn:
                setScreen(SCREEN_WIDGET);
                break;
            case R.id.settings:
                runActivity(MainActivity.HANDLE_RUN_SETTING_ACTIVITY);
                break;
            case R.id.wallpaper_btn:
                runActivity(MainActivity.HANDLE_RUN_WALLPAPER_ACTIVITY);
                break;
        }
    }

    //배경이미지 설정
    public void setBackgroundImage(BitmapDrawable drawable){
        final Bitmap bitmap = drawable.getBitmap();

        if(bitmap != null){
            int mode = WideBackgroundLayout.MODE_CROP;
            float bitmapRatio = (float)bitmap.getWidth() / bitmap.getHeight();
            float screenRatio = (float)getWidth() / getHeight();

            if(bitmapRatio > screenRatio)
                mode = WideBackgroundLayout.MODE_WIDE;

            mWideBackgroundLayout.setBackground(mode, drawable);
        }
        else
            mWideBackgroundLayout.clear();

    }

    //배경 이미지 제거
    public void clearBackgroundImage(){
        mWideBackgroundLayout.clear();
    }

    //Back키를 누를 시 화면 상태
    public boolean onBackPressed(){
        if(mPageState != SCREEN_MAIN) {
            if (mPageState == SCREEN_WIDGET)
                setScreen(SCREEN_PAGE_EDIT);
            else
                setScreen(SCREEN_MAIN);
            return  true;
        }
        return false;
    }

    //DB로부터 페이지 정보(아이콘)를 로드한다
    public void loadData(){
        //배경이미지를 로드한다
        BitmapDrawable drawable = LauncherModel.loadBackgroundFile(getContext());
        if(drawable != null)
            setBackgroundImage(drawable);

        //워크스페이스 영역을 로드한다
        mPageWorkSpace.loadData();
        mBottomCelllayout.loadData();
    }

    //위젯 요청에 따라 위젯 추가
    public void addWidgetByRequest(IconData iconData){
        final IconView iconView = new IconView(getContext());
        final int iconSize[] = mPageWorkSpace.getIconSize();

        iconData.setCacheView(false);
        iconData.setDrawable(null);

        iconView.setWidgetData(iconData);
        iconView.setSize(0, 0, iconSize[0]*iconData.getCellWidth(), iconSize[1]*iconData.getCellHeight());
        mPageWorkSpace.addWidgetByRequest(iconView);
    }

    //롱터치에 대한 액션
    public void longTouchAction(int x, int y) {
        final View touchView = getTouchArea(y);

        if (mPageState == SCREEN_MAIN || mPageState == SCREEN_PAGE_EDIT) { //메인, 페이지 편집 화면
            if(mPageState == SCREEN_PAGE_EDIT) { //화면을 픽한 경우
                if(touchView == mPageWorkSpace)
                    mPickView = mPageWorkSpace.pick(x, y);
            }
            else { //메인 페이지인 경우 픽하는 경우
                if(touchView == mPageWorkSpace)
                    mPickView = mPageWorkSpace.getChildPick(x, y);
                else if(touchView == mBottomCelllayout) {
                    mPickView = mBottomCelllayout.pick(x, y);
                }
            }

            if (mPickView != null) {
                if (mPageState == SCREEN_MAIN) {
                    getLongTouchCache(touchView, true);//아이콘 비트맵 캐쉬
                    setScaleIcon(mPickView);//워크스페이스 영역 크기 기준으로 복귀
                }
                startIconDrag(touchView, x, y);
            } else if(touchView == mPageWorkSpace)
                setScreen(SCREEN_PAGE_EDIT);
        }
    }

    //패키지 이름으로 아이콘 삭제
    public void deleteIconByPackageName(String packageName){
       mPageWorkSpace.deleteIconByPackageName(packageName);
    }

    //Apps화면 에 대한 롱터치
    public void longTouchActionApps(int x, int y, int modelIdx){
        if(mPageState == SCREEN_APPS) { //앱스에서 롱터치를 한 경우
            if(modelIdx != -1) { //패키지 리스트에 있을 경우
                ApplicationData data = mLauncherModel.getAppsData(modelIdx);
                if (data != null) { //정보를 찾았으면 드래그 시작
                    final IconView iconView = new IconView(getContext());
                    final int iconSize[] = mPageWorkSpace.getIconSize();
                    iconView.setApplicationData(data);
                    iconView.setSize(x, y, iconSize[0], iconSize[1]);
                    setScaleIcon(iconView);
                    mPickView = iconView;
                    startIconDrag(getTouchArea(y), x + iconSize[0]/2, y + iconSize[1]/2);

                    mMainHandler.sendEmptyMessage(MainActivity.HANDLE_CACHE_PICK_ITEM);//아이콘 비트맵 캐쉬
                    return ;
                }
            }
        }

        //아이콘 정보를 못찾은 경우 그냥 메인을 표출
        setScreen(SCREEN_MAIN);
    }

    //Widget에 대한 롱터치
    public void longTouchActionWidget(int x, int y, IconData iconData){
        if(mPageState == SCREEN_WIDGET) { //앱스에서 롱터치를 한 경우
            if(iconData != null) { //패키지 리스트에 있을 경우
                final IconView iconView = new IconView(getContext());
                final int iconSize[] = mPageWorkSpace.getIconSize();
                final int cellWidth = iconSize[0]*iconData.getCellWidth();
                final int cellHeight = iconSize[1]*iconData.getCellHeight();

                iconView.setWidgetData(iconData);

                iconView.setSize(x, y, cellWidth, cellHeight);
                iconView.invalidate();
                mPickView = iconView;
                startIconDrag(getTouchArea(y), x + cellWidth/2, y + cellHeight/2);

                mMainHandler.sendEmptyMessage(MainActivity.HANDLE_CACHE_PICK_ITEM);//아이콘 비트맵 캐쉬
                return ;
            }
        }

        //정보를 못찾은 경우 그냥 메인을 표출
        setScreen(SCREEN_MAIN);
    }

    public void getLongTouchCache(View v, boolean isScale){
        if(mPickView != null) {
            final int btmIconWidth = mBottomCelllayout.getIconSize();
            final int[] workSpaceSize = mPageWorkSpace.getIconSize();

            float scale, btnScale;
            int[] iconSize;

            //상단과 하단 영역의 종횡비가 틀리기 때문에 자를 영역을 계산함
            final float heightRatio = btmIconWidth/(float)workSpaceSize[1]; // 세로가 줄어든 비율
            final float widthReal = workSpaceSize[0] * heightRatio;//줄어든 가로의 실제 크기
            int cutSize = 0;

            if(v == mBottomCelllayout) {
                iconSize = new int[]{btmIconWidth, btmIconWidth};
                scale = (float)workSpaceSize[1]/btmIconWidth * mPageWorkSpace.getScaleValue();
                btnScale = 1;
                cutSize = (int) (btmIconWidth - widthReal)/2;
            }
            else {
                iconSize = workSpaceSize;
                scale = mPageWorkSpace.getScaleValue();
                btnScale = (float)btmIconWidth/workSpaceSize[1];
            }

            //워크스페이스 영역에 캐시이미지 저장
            FastBitmapDrawable drawable = setCacheImage(mPickView, iconSize, cutSize, scale, isScale);
            ((IconView) mPickView).setIconCache(drawable);

            //하단 아이콘 영역 그림자 이미지 - apps아이콘일 경우만 생성
            if(mPickView instanceof IconView) {
                if(((IconView)mPickView).getIconData().getType() == IconData.TYPE_APPS) {
                    FastBitmapDrawable btmDrawable = setCacheImage(mPickView, iconSize, 0, btnScale, isScale);
                    mBottomCelllayout.setIconCacheDrawable(btmDrawable);
                }
            }
        }
    }

    //아이콘 드래그용 캐쉬 이미지 저장
    private FastBitmapDrawable setCacheImage(View v, int[] iconSize, int cutSize, float scale, boolean isScale){
        FastBitmapDrawable drawable = Utility.getCacheDrawable(v,  iconSize, isScale ? scale : 1); //캐쉬 이미지 아이콘 사이즈에 맞게 추출

        if (drawable != null) {
            final Bitmap src = drawable.getBitmap();
            if(src != null && cutSize != 0){ //잘라낼 사이즈가 있다면
                Bitmap resize =Bitmap.createBitmap(src, cutSize, 0, src.getWidth() - cutSize, src.getHeight());
                drawable = new FastBitmapDrawable(resize);
                src.recycle();
            }
        }

        return drawable;
    }

    //현재 좌표가 어떤 영역인지 확인
    private View getTouchArea(int y){
        final int[] location = new int[2];

        mLayoutTop.clearSelected();

        if(mPageState == SCREEN_MAIN){
            mBottomCelllayout.getLocationOnScreen(location);
            if(y < location[1])
                return mPageWorkSpace;
            else
                return mBottomCelllayout;
        }
        else if(mPageState == SCREEN_PAGE_EDIT){
            mPageWorkSpace.getLocationOnScreen(location);
            if(y < location[1]){
                return mLayoutTop;
            }
            else{
                mLayoutMenu.getLocationOnScreen(location);
                if(y < location[1])
                    return mPageWorkSpace;
                else
                    return mLayoutMenu;

            }
        }
        else if(mPageState == SCREEN_DRAG_MODE){
            mPageWorkSpace.getLocationOnScreen(location);
            if(y < location[1]){
                return mLayoutTop;
            }
            else{
                mBottomCelllayout.getLocationOnScreen(location);
                if(y < location[1])
                    return mPageWorkSpace;
                else
                    return mBottomCelllayout;
            }
        }

        return null;
    }

    //액티비티 실행 요청
    private void runActivity(int what){
        mMainHandler.sendEmptyMessage(what);
    }

    //화면 상태 변경
    private void setScreen(int state){
        if(mPageState == state)
            return ;

        if(mMainHandler.hasMessages(MainActivity.HANDLE_LONG_TOUCH))
            mMainHandler.removeMessages(MainActivity.HANDLE_LONG_TOUCH);

        if(state == SCREEN_PAGE_EDIT){ //페이지를 에디터하는 화면
            if(mLayoutBottomArea.getVisibility() == View.VISIBLE)
                mLayoutBottomArea.setAnimationVisiblity(AnimationLinearLayout.MODE_FADE_OUT);

            if(mLayoutMenu.getVisibility() != View.VISIBLE)
                mLayoutMenu.setAnimationVisiblity(AnimationLinearLayout.MODE_SLIDE_UP);
            if(mLayoutTop.getVisibility() != View.VISIBLE)
                mLayoutTop.setAnimationVisiblity(AnimationLinearLayout.MODE_SLIDE_DOWN);
            mAppsLayout.setAnimationVisiblity(View.GONE);

            if(!mPageWorkSpace.isPageCustomMode()) {
                sendPageZoomEffect(MainActivity.HANDLE_PAGE_EFFECT_ZOOM_OUT);

                mPageWorkSpace.savePageCache();
                mPageWorkSpace.setPageEditMode(true); //페이지 모드 변경
                mPageWorkSpace.initZoomEffect(0);
            }
        }
        else if(state == SCREEN_DRAG_MODE){ //드래그 발생시 화면 -> 페이지와 같은 화면을 쓰지만 기능이 다름
            if(mLayoutTop.getVisibility() != View.VISIBLE)
                mLayoutTop.setAnimationVisiblity(AnimationLinearLayout.MODE_SLIDE_DOWN);
            mAppsLayout.setAnimationVisiblity(View.GONE);

            if(!mPageWorkSpace.isPageCustomMode()) {
                sendPageZoomEffect(MainActivity.HANDLE_PAGE_EFFECT_ZOOM_OUT);

                mPageWorkSpace.savePageCache();
                mPageWorkSpace.setPageEditMode(true); //페이지 모드 변경
                mPageWorkSpace.initZoomEffect(0);
            }
        }
        else if(state == SCREEN_APPS){ //앱아이콘 리스트 화면
            final Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.btn_apps);
            final int size = getResources().getDimensionPixelSize(R.dimen.menu_btn_size);
            d.setBounds(0, 0, size, size);

            ((TextView)findViewById(R.id.menu_name)).setCompoundDrawables(d, null, null , null);
            ((TextView)findViewById(R.id.menu_name)).setText(R.string.apps);

            mAppsGridView.changeMode(AllIconGirdView.MODE_APPS);
            mAppsLayout.setAnimationVisiblity(View.VISIBLE);
        }
        else if(state == SCREEN_WIDGET){ //위젯 리스트 화면
            final Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.btn_widget);
            final int size = getResources().getDimensionPixelSize(R.dimen.menu_btn_size);
            d.setBounds(0, 0, size, size);

            ((TextView)findViewById(R.id.menu_name)).setCompoundDrawables(d, null, null , null);
            ((TextView)findViewById(R.id.menu_name)).setText(R.string.widgets);

            mAppsGridView.changeMode(AllIconGirdView.MODE_WIDGETS);
            mAppsLayout.setAnimationVisiblity(View.VISIBLE);
        }
        else if(state == SCREEN_MAIN){ //메인 화면
            if(mPageState == SCREEN_PAGE_EDIT)
                mPageWorkSpace.checkAddedPage();

            if(mLayoutTop.getVisibility() == View.VISIBLE)
                mLayoutTop.setAnimationVisiblity(AnimationLinearLayout.MODE_FADE_OUT);
            if(mLayoutMenu.getVisibility() == View.VISIBLE)
                mLayoutMenu.setAnimationVisiblity(AnimationLinearLayout.MODE_FADE_OUT);
            if(mLayoutBottomArea.getVisibility() == View.GONE)
                mLayoutBottomArea.setAnimationVisiblity(AnimationLinearLayout.MODE_SLIDE_UP);

            mAppsLayout.setAnimationVisiblity(View.GONE);

            if(mPageWorkSpace.isPageCustomMode()) {
                sendPageZoomEffect(MainActivity.HANDLE_PAGE_EFFECT_ZOOM_IN);

                mPageWorkSpace.setPageEditMode(false); //페이지 모드 변경
                mPageWorkSpace.savePageCache();
                mPageWorkSpace.initZoomEffect(mLayoutTop.getHeight());
            }
        }

        mPageState = state;
    }

    //메인 화면으로 설정
    public void setMainScreen(){
        setScreen(SCREEN_MAIN);
    }

    //페이지 모드 전환 효과 요청
    public void sendPageZoomEffect(int what){
        int[] oldPageSize = mPageWorkSpace.getPageSize(); //변경되기 전 페이지 사이즈 획득

        Message msg = new Message();
        msg.what = what;
        msg.arg1 = oldPageSize[0];
        msg.arg2 = oldPageSize[1];
        mMainHandler.sendMessage(msg);
    }

    //페이지 편집 모드 전환 효과
    public void startZoomEffect(boolean mode, int oldWidth, int oldHeight){
        final int[] pageSize = mPageWorkSpace.getPageSize();
        final int[] location = new int[2];
        mPageWorkSpace.getLocationOnScreen(location);

        final int topMargin = (oldHeight/2 - (pageSize[1]+mPageWorkSpace.getPaddingTop()*2)/2)/2;

        if(mode)
            mPageWorkSpace.startZoomEffect(oldWidth, oldHeight, pageSize[0], pageSize[1], -topMargin);
        else
            mPageWorkSpace.startZoomEffect(oldWidth, oldHeight, pageSize[0], pageSize[1] + location[1], -topMargin);
    }

    //롱터치 이벤트 발생
    private void sendLongTouchEvent(MotionEvent event){
        Message msg = new Message();
        msg.what = MainActivity.HANDLE_LONG_TOUCH;
        msg.arg1 = (int) event.getX();
        msg.arg2 = (int) event.getY();
        mMainHandler.sendMessageDelayed(msg, 500);
    }

    //클릭에 대한 이벤트 처리
    private void clickAction(View view, MotionEvent event){
        if(mPageState == SCREEN_PAGE_EDIT && view == mPageWorkSpace) //화면을 픽한 경우
            setScreen(SCREEN_MAIN);
        else if(mPageState == SCREEN_MAIN){ //메인 화면 이면
            IconView iconView = null;

            //터치 영역 검사
            if(view == mPageWorkSpace)
                iconView = (IconView) mPageWorkSpace.getChild((int) event.getX(), (int) event.getY());
            if(view == mBottomCelllayout)
                iconView = (IconView) mBottomCelllayout.getChild((int) event.getX());

            setClickAction(iconView);
        }
    }

    //클릭했을때 액션
    private void setClickAction(IconView iconView){
        if (iconView != null) {  //찾은 아이콘이 있으면
            final IconData data = iconView.getIconData();

            if (data != null && data.getType() == IconData.TYPE_APPS) { //Apps이면 실행
                Message msg = new Message();
                msg.what = MainActivity.HANDLE_RUN_ACTIVITY;
                msg.obj = data;
                mMainHandler.sendMessage(msg);
            }
            else if (data != null && data.getType() == IconData.TYPE_FIXED) { //고정 아이템일 경우
                if(data.getKey() == IconData.KEY_APPS)//Apps일 경우
                    setScreen(SCREEN_APPS);
            }
        }
    }

    //아이콘 축소
    private void setScaleIcon(View v){
        if(v instanceof IconView) {
            final float scale = mPageWorkSpace.getScaleValue();
            v.setScaleX(scale - 0.05f); //0.05는 보정값
            v.setScaleY(scale - 0.05f);
        }
    }

    //++++드래그 이벤트 액션++++
    // 아이콘 드래그가 발생 하였을때 호출
    private void startIconDrag(View touchView, int x, int y){
        isIconMoving = true;
        mEmptySearchState = CellLayout.STAY_STATE;

        if(mPickView != null) { //드래그 영역에 뷰를 넣음
            mDragLayout.addView(mPickView);
            mPickView.invalidate();
        }

        moveIconDrag(touchView, x, y); //첫 위치 갱신
        setScreen(SCREEN_DRAG_MODE);//화면 드래그 모드 설정
    }

    //아이콘 드래그 중에 발생
    private void moveIconDrag(View touchView, int x, int y){
        if(mPickView != null){
            final LayoutParams p = (LayoutParams) mPickView.getLayoutParams();
            p.leftMargin = x - p.width/2;
            p.topMargin = y - p.height/2;
            mPickView.setLayoutParams(p);  //드래그 영역에 위치를 갱신

            int searchState = CellLayout.NOT_FOUND;

            if(touchView == mBottomCelllayout)
                searchState = mBottomCelllayout.checkSpace(mPickView, x);  //빈공간 탐색
            else if(touchView == mPageWorkSpace) {
                searchState = mPageWorkSpace.checkSpace(mPickView, x, y);  //빈공간 탐색
                mBottomCelllayout.checkSpace(mPickView, -999); //효과 취소
            }

            if (searchState == CellLayout.FOUND) {  //빈공간을 찾은 경우
                mPickView.setAlpha(1f);
                mEmptySearchState = searchState;
            } else if (searchState == CellLayout.NOT_FOUND) { //빈공간을 찾지 못한 경우
                mPickView.setAlpha(0.35f);
                mEmptySearchState = searchState;
            }
        }
    }

    //아이콘 드래그가 끝났을때 발생
    private void stopIconDrag(View selectArea, int x, int y){
        if(isIconMoving && mPickView != null){ //이동이 일어났다면
            mDragLayout.removeView(mPickView); //드래그 영역에서 아이콘을 지움

            //상태 복귀
            mPickView.setScaleX(1.0f);
            mPickView.setScaleY(1.0f);
            mPickView.setAlpha(1f);

            //아이콘 캐쉬 비트맵 제거
            final FastBitmapDrawable drawable = ((IconView)mPickView).getIconCache();
            if(drawable != null && drawable.getBitmap() != null && !drawable.getBitmap().isRecycled())
                drawable.getBitmap().recycle();

            ((IconView)mPickView).setIconCache(null);

            if(selectArea == mPageWorkSpace) { //드래그 영역이 워크스페이스고 해당 위치에 빈공간을 찾았다면
                boolean isSuccess = mPageWorkSpace.drop(mPickView, x, y, mEmptySearchState == CellLayout.FOUND); //드롭
                if(!isSuccess && mPickView instanceof IconView)
                    cancelDragAction((IconView) mPickView);
            }
            else if(selectArea == mBottomCelllayout) { //하단 아이콘 영역일 경우
                boolean isSuccess = mBottomCelllayout.drop(mPickView, x, y, mEmptySearchState == CellLayout.FOUND); //드롭
                if(!isSuccess && mPickView instanceof IconView)
                    cancelDragAction((IconView) mPickView);
            }
            else if(selectArea == mLayoutTop){
                View find = ((AnimationLinearLayout)selectArea).checkChildSelected(x);
                if(find != null){
                    if(find.getId() == R.id.delete)
                        mPageWorkSpace.deleteItem((IconView) mPickView);
                    else //TODO:추후 폴더 기능 구현
                        mPageWorkSpace.cancelDragIcon((IconView) mPickView);
                }
                else
                    mPageWorkSpace.cancelDragIcon((IconView) mPickView);
            }
            else
                mPageWorkSpace.cancelDragIcon((IconView) mPickView);

            //메인 스크린 복귀
            mMainHandler.sendEmptyMessage(MainActivity.HANDLE_SET_SCREEN_MAIN);
        }

        mPickView = null;
        isIconMoving = false;
    }

    //드래그 취소
    private void cancelDragAction(IconView iconView){
        IconData data = iconView.getIconData();

        if(data.getPageKey() != LauncherDBHelper.BOTTOM_PAGE_KEY)
            mPageWorkSpace.cancelDragIcon(iconView);
        else
            mBottomCelllayout.cancelDragIcon(iconView);
    }
}