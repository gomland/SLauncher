package my.home.slauncher.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Random;

import my.home.slauncher.data.IconData;


/**
 * Created by ShinSung on 2017-07-04.
 * 종류 : 화면 상 아이콘 정보를 유지
 */

public class LauncherDBHelper extends SQLiteOpenHelper {
    public static final int BOTTOM_PAGE_KEY = -999;

    private static final String DB_NAME = "launcher_info.db";
    private static final int VERSION = 1;

    private final String TABLE_PAGE_DATA = "PAGE_DATA";
    private final String TABLE_ICON_DATA = "ICON_DATA";

    //페이지에 대한 데이터 설정
    private final String TABLE_PAGE_DATA_COL =
            " (page_key INT PRIMARY KEY)";

    //아이콘 데이터에 대한 테이블 설정
    private final String TABLE_ICON_DATA_COL =
            " (icon_key INT PRIMARY KEY, " +
                    "page_key INT, " +
                    "package TEXT NOT NULL, " +
                    "component TEXT, " +
                    "type INT, " +
                    "name TEXT, " +
                    "x INT, " +
                    "y INT, " +
                    "width INT, " +
                    "height INT, " +
                    "widget_id INT)";

    private static LauncherDBHelper mCloudDBHelper = null;

    private SQLiteDatabase mReadableDatabase, mWritableDatabase;

    public static LauncherDBHelper inst() {
        return mCloudDBHelper;
    }

    public static void create(Context context) {
        if (mCloudDBHelper == null) {
            mCloudDBHelper = new LauncherDBHelper(context, DB_NAME, null, VERSION);
        }
    }

    public LauncherDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

        mReadableDatabase = getReadableDatabase();
        mWritableDatabase = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_PAGE_DATA + " " + TABLE_PAGE_DATA_COL);
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_ICON_DATA + " " + TABLE_ICON_DATA_COL);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE_DATA);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ICON_DATA);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void close() {
        if (mCloudDBHelper != null)
            mCloudDBHelper = null;

        if(mReadableDatabase != null)
            mReadableDatabase.close();

        if(mWritableDatabase != null)
            mWritableDatabase.close();

        mReadableDatabase = null;
        mWritableDatabase = null;

        super.close();
    }


    /***********
     * REPLACE *
     ***********/
    public void addPage(){
        Random ran = new Random();
        addPage(ran.nextInt(9999));
    }

    public void addPage(int pageKey){
        ContentValues contentValue = new ContentValues();
        contentValue.put("page_key", pageKey);
        mWritableDatabase.insert(TABLE_PAGE_DATA, null, contentValue);
    }

    public void insertIconData(int pageIdx, IconData data) {
        ContentValues contentValue = new ContentValues();
        contentValue.put("icon_key", data.getKey());
        contentValue.put("page_key", pageIdx);
        contentValue.put("package", data.getPackageName());
        contentValue.put("component", data.getComponentName());
        contentValue.put("type", data.getType());
        contentValue.put("name", data.getName());
        contentValue.put("x", data.getX());
        contentValue.put("y", data.getY());
        contentValue.put("width", data.getCellWidth());
        contentValue.put("height", data.getCellHeight());
        contentValue.put("widget_id", data.getWidgetId());
        mWritableDatabase.replace(TABLE_ICON_DATA, null, contentValue);
    }

    public void changePage(int src, int des){
        ArrayList<Integer> pageKeyList = getPageKey();

        int srcPageKey = pageKeyList.get(src); //순서 변경
        pageKeyList.remove(src);
        pageKeyList.add(des, srcPageKey);

        deleteAllPage(); //모든 페이지 삭제

        for(int i=0; i<pageKeyList.size(); i++) //갱신
            addPage(pageKeyList.get(i));
    }

    /***********
     * DELETE *
     ***********/
    public void deletePage(int pageKey){
        mWritableDatabase.delete(TABLE_PAGE_DATA, "page_key=" + pageKey, null);
        mWritableDatabase.delete(TABLE_ICON_DATA, "page_key=" + pageKey, null);
    }

    public void deleteAllPage(){
        mWritableDatabase.delete(TABLE_PAGE_DATA, null, null);
    }

    public void deleteData(int iconKey){
        mWritableDatabase.delete(TABLE_ICON_DATA, "icon_key=" + iconKey, null);
    }

    public void deleteData(String packageName){
        mWritableDatabase.delete(TABLE_ICON_DATA, "package='" + packageName + "'", null);
    }

    /***********
     * SELECT  *
     ***********/
    public int getPageCnt(){
        Cursor cur = mReadableDatabase.rawQuery("SELECT COUNT(*) FROM "+ TABLE_PAGE_DATA, null);
        cur.moveToFirst();

        int cnt = 0;
        if(cur.getCount() > 0)
            cnt = cur.getInt(0);

        cur.close();
        return cnt;
    }

    public int getPageKey(int idx){
        Cursor cur = mReadableDatabase.rawQuery("SELECT * FROM "+ TABLE_PAGE_DATA, null);
        cur.moveToFirst();
        int cnt = 0;

        if(cur.getCount() > 0) {
            do {
                if (cnt++ == idx) {
                    idx = cur.getInt(0);
                    break;
                }
            }while(cur.moveToNext());
        }

        cur.close();
        return idx;
    }

    public ArrayList<Integer> getPageKey(){
        final ArrayList<Integer> pageKeyList = new ArrayList<>();
        Cursor cur = mReadableDatabase.rawQuery("SELECT * FROM "+ TABLE_PAGE_DATA, null);
        cur.moveToFirst();

        if(cur.getCount() > 0) {
            do {
                pageKeyList.add(cur.getInt(0));
            }
            while(cur.moveToNext());
        }

        cur.close();
        return pageKeyList;
    }

    public boolean isIconExisted(int iconKey){
        Cursor cur = mReadableDatabase.rawQuery("SELECT COUNT(*) FROM " + TABLE_ICON_DATA + " WHERE icon_key=" + iconKey, null);
        cur.moveToFirst();
        int cnt = 0;
        if(cur.getCount() > 0)
            cnt = cur.getInt(0);
        cur.close();

        return cnt > 0 ? true : false;
    }

    public ArrayList<IconData> getIconData(int pageKey){
        final ArrayList<IconData> iconDataList = new ArrayList<>();
        Cursor cur = mReadableDatabase.rawQuery("SELECT * FROM " + TABLE_ICON_DATA + " WHERE page_key=" + pageKey, null);
        cur.moveToFirst();

        if(cur.getCount() > 0) {
            do{
                IconData data = new IconData();
                data.setKey(cur.getInt(0));
                data.setPageKey(cur.getInt(1));
                data.setPackageName(cur.getString(2));
                data.setComponentName(cur.getString(3));
                data.setType(cur.getInt(4));
                data.setName(cur.getString(5));
                data.setX(cur.getInt(6));
                data.setY(cur.getInt(7));
                data.setCellWidth(cur.getInt(8));
                data.setCellHeight(cur.getInt(9));
                data.setWidgetId(cur.getInt(10));
                iconDataList.add(data);
            }while (cur.moveToNext());
        }

        cur.close();

        return iconDataList;
    }
}