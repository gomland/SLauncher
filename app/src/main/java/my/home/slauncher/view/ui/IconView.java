package my.home.slauncher.view.ui;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.StringTokenizer;

import my.home.slauncher.data.ApplicationData;
import my.home.slauncher.data.IconData;
import my.home.slauncher.view.activity.MainActivity;
import my.home.slauncher.view.animate.MoveAnimation;
import my.home.slauncher.view.drawable.FastBitmapDrawable;

import static android.graphics.Paint.Align.CENTER;

/**
 * Created by ShinSung  on 2017-09-13.
 * 종류 : 런처 아이콘 뷰
 * 내용 : 앱, 위젯, 폴더 아이콘을 모두 다룸
 * 폴더는 추후 필요하면 구현
 */

public class IconView extends FrameLayout {
    private IconData mIconData; //아이콘 데이터
    private Paint mTextPainter;
    private WidgetView widgetHostView = null;
    private MoveAnimation mMoveAnimation = null;
    private FastBitmapDrawable mIconCache = null;

    public IconView(Context context) {
        this(context, null);
    }

    public IconView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        init();
    }

    private void init(){
        CellLayout.LayoutParams p = new CellLayout.LayoutParams(0, 0, 1, 1);
        setLayoutParams(p);

        mTextPainter = new Paint();
        mTextPainter.setStyle(Paint.Style.FILL_AND_STROKE);
        mTextPainter.setTextAlign(CENTER);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        final ViewGroup.LayoutParams p = getLayoutParams();

        if((mIconData.getType() == IconData.TYPE_WIDGET || mIconData.getType() == IconData.TYPE_SCREEN)
                && mIconData.isCacheView()){ //위젯 또는 스크린이며 preview를 보여주어야 할 경우
            final Drawable background = mIconData.getDrawable();
            if (background != null) {
                final int height = (int) (p.width * ((float)mIconData.getCellHeight() / mIconData.getCellWidth()));
                background.setBounds(0, 0, p.width, height);
                background.draw(canvas);
            }
        }
        else{ //그외의 경우 아이콘을 그려줌
            final Drawable background = mIconData.getDrawable();
            final int iconSize = (int) (p.height * 0.7f);
            final String text = mIconData.getName();

            //아이콘과 텍스트는 7:3의 공간 비율을 차지함
            if (background != null) { //아이콘 배경
                final int left = (p.width-iconSize)/2;
                background.setBounds(left, 0, left+iconSize, iconSize);
                background.draw(canvas);
            }

            if (text != null) { //아이콘 이름
                final int textSize = (int) (p.height*0.135f);
                final int padding = (int) (p.height*0.015f);
                final String[] textResult = getIconTextParsing(text);

                mTextPainter.setTextSize(textSize);
                mTextPainter.setStyle(Paint.Style.FILL_AND_STROKE);
                mTextPainter.setStrokeWidth(1);
                mTextPainter.setTextAlign(CENTER);
                mTextPainter.setAntiAlias(true);

                if(textResult[1].length() == 0){
                    mTextPainter.setColor(Color.BLACK);
                    canvas.drawText(textResult[0], p.width/2 + 2, textSize + iconSize + padding*2 + 2, mTextPainter);

                    mTextPainter.setColor(Color.WHITE);
                    canvas.drawText(textResult[0], p.width/2, textSize + iconSize + padding*2, mTextPainter);
                }
                else {
                    mTextPainter.setColor(Color.BLACK);
                    canvas.drawText(textResult[0], p.width/2 + 2, textSize/2 + iconSize + padding*4 + 2, mTextPainter);
                    canvas.drawText(textResult[1], p.width/2 + 2, textSize/2 + iconSize + textSize + padding*5 + 2, mTextPainter);

                    mTextPainter.setColor(Color.WHITE);
                    canvas.drawText(textResult[0], p.width/2, textSize/2 + iconSize + padding*4, mTextPainter);
                    canvas.drawText(textResult[1], p.width/2, textSize/2 + iconSize + textSize + padding*5, mTextPainter);
                }
            }
        }

        super.dispatchDraw(canvas);
    }

    private final int TEXT_MAX = 10;
    private String[] getIconTextParsing(String text){
        String[] result = new String[]{"", ""};
        StringTokenizer stringTokenizer = new StringTokenizer(text);
        int line = 0;

        while(stringTokenizer.hasMoreTokens()){
            String token = stringTokenizer.nextToken();
            if(result[0].length() == 0 && token.length() > TEXT_MAX) {
                result[0] = token.substring(0, TEXT_MAX) + "...";
                break;
            }
            else if(result[line].length() + token.length() > TEXT_MAX) {
                if(line == 1){
                    result[line] += "...";
                    break;
                }
                else
                    line++;
            }

            if(result[line].length() == 0)
                result[line] = token;
            else
                result[line] += " " + token;
        }

        return result;
    }

    public void setIconData(IconData iconData){
        mIconData = iconData;
    }

    //아이콘 데이터 반환
    public IconData getIconData(){
        return mIconData;
    }

    //ApplicationData로 부터 아이콘 데이터 생성
    public void setApplicationData(ApplicationData data){
        if(mIconData == null)
            mIconData = new IconData();

        mIconData.generateKey();
        mIconData.setComponentName(data.getComponentName());
        mIconData.setPackageName(data.getPackageName());
        mIconData.setDummyView(true);
        mIconData.setName(data.getName());
        mIconData.setType(IconData.TYPE_APPS);
        mIconData.setCellWidth(1);
        mIconData.setCellHeight(1);
        mIconData.setDrawable(data.getIconDrawable());
    }

    //아이콘 데이터로 부터 위젯 데이터 생성
    public void setWidgetData(IconData data){
        mIconData = data;
        mIconData.generateKey();
        mIconData.setType(IconData.TYPE_WIDGET);
    }

    //아이콘 뷰의 사이즈 재설정
    public void setSize(int x, int y, int width, int height){
        final CellLayout.LayoutParams p = (CellLayout.LayoutParams) getLayoutParams();
        if(x != -1)
            p.x = x;
        if(y != -1)
            p.y = y;
        p.width = width;
        p.height = height;
        setLayoutParams(p);
    }

    //++++++++++ 애니메이션 설정 +++++++++++++++
    //순서 : setMoveAnimation -> startMoveAnimation -> cancelMoveAnimation

    //해당 start, end좌표로 애니메이션 제작
    private void createAnimator(int startX, int startY, int endX, int endY){
        if(mMoveAnimation == null)
            mMoveAnimation = new MoveAnimation(this);

        clearAnimation();

        final int delay = (Math.abs(startX-endX) + Math.abs(startY-endY)) >> 1;
        mMoveAnimation.setDuration(delay > 25 ? delay : 25); //이동거리에 비례해서 delay를 주며 최하 25ms를 가짐
        mMoveAnimation.setData(startX, startY, endX, endY);
    }

    //1. 애니메이션 설정
    public void setMoveAnimation(int desX, int desY){
        final ViewGroup.LayoutParams params = getLayoutParams();
        if(params instanceof  CellLayout.LayoutParams) {
            final CellLayout.LayoutParams p = (CellLayout.LayoutParams) params;
            final int tranX = (int) getTranslationX(); //현재 translate값으로 부터 목적지의 거리를 계산
            final int tranY = (int) getTranslationY();
            createAnimator(tranX, tranY, desX - p.x, desY - p.y);
        }
    }

    //2.애니메이션 시작
    public void startMoveAnimation(){
        if(mMoveAnimation != null)
            startAnimation(mMoveAnimation);
    }

    //3. 애니메이션 취소 - (isAnimation : 취소시 애니메이션이 있게 취소할지 설정)
    public void cancelMoveAnimation(boolean isAnimation){
        if(isAnimation) { //애니메이션이 있게 하고 싶다면
            final int tranX = (int) getTranslationX();
            final int tranY = (int) getTranslationY();
            createAnimator(tranX, tranY, 0, 0); //원점으로 복귀하는 애니메이션을 만들고
            startMoveAnimation(); //시작
        }
        else{ //복귀, 삭제
            clearAnimation();
            mMoveAnimation = null;
            setTranslationX(0);
            setTranslationY(0);
        }
    }

    //아이콘 데이터에 맵핑된 widget Id를 기준으로 widget host view제작
    public void addWidgetHostView(){
        if(widgetHostView != null)
            removeView(widgetHostView);

        AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(getContext()).getAppWidgetInfo(mIconData.getWidgetId());
        widgetHostView = (WidgetView) MainActivity.getAppWidgetHost().createView(getContext(), mIconData.getWidgetId(), appWidgetInfo);
        widgetHostView.setId(mIconData.getWidgetId());
        widgetHostView.setAppWidget(mIconData.getWidgetId(), appWidgetInfo);
        addView(widgetHostView, 0);
    }

    //위젯 뷰에 대하여 별도로 zoom값 맵핑
    //페이지 모드 활성화시 크기를 줄여주기 위함
    public void setWidgetScale(int width, int height, float scale){
        if(widgetHostView != null){
            width -= (width * scale);
            height -= (height * scale);
            widgetHostView.setTranslationX(-width/2);
            widgetHostView.setTranslationY(-height/2);
            widgetHostView.setScaleX(scale);
            widgetHostView.setScaleY(scale);
        }
    }

    public FastBitmapDrawable getIconCache() {
        return mIconCache;
    }

    public void setIconCache(FastBitmapDrawable iconCache) {
        mIconCache = iconCache;
    }
}
