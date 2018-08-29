package my.home.slauncher.data;

import android.graphics.drawable.Drawable;

import java.util.Random;

/**
 * Created by ShinSung on 2017-09-13.
 */

public class IconData {
    public static final int KEY_APPS = -1;

    public static final int TYPE_APPS = 0;
    public static final int TYPE_WIDGET = 1;
    public static final int TYPE_FOLDER = 2; //TODO:필요하면 추후 구현
    public static final int TYPE_SCREEN = 3;
    public static final int TYPE_FIXED = 4;

    private String name;
    private String packageName;
    private String componentName;
    private Drawable drawable;
    private int key, pageKey, type;
    private int x, y;
    private int cellWidth, cellHeight;
    private int widgetId;
    private boolean isCacheView = false; //실제 뷰 대신 cache를 보여줘야 할경우 설정
    private boolean isDummyView = false; //실제 화면에 배치된 뷰인지, 신규로 배치되는 뷰인지 확인, App Uninstall 기능으로 활용중

    public int getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(int widgetId) {
        this.widgetId = widgetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public int getKey() {
        return key;
    }

    public void generateKey(){
        Random ran = new Random();
        key = ran.nextInt(99999);
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public void setCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public void setCellHeight(int cellHeight) {
        this.cellHeight = cellHeight;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public int getPageKey() {
        return pageKey;
    }

    public void setPageKey(int pageKey) {
        this.pageKey = pageKey;
    }

    public boolean isCacheView() {
        return isCacheView;
    }

    public void setCacheView(boolean set) {
        isCacheView = set;
    }

    public boolean isDummyView() {
        return isDummyView;
    }

    public void setDummyView(boolean dummyView) {
        isDummyView = dummyView;
    }
}
