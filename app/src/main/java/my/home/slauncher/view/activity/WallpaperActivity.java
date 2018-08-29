package my.home.slauncher.view.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.content.res.AppCompatResources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import my.home.slauncher.R;
import my.home.slauncher.model.LauncherModel;
import my.home.slauncher.view.ui.AreaSelectView;
import my.home.slauncher.view.ui.MenuButton;
import my.home.slauncher.view.ui.WideBackgroundLayout;

import static android.content.ContentValues.TAG;

/**
 * Created by ShinSung on 2017-09-26.
 * 종류 : 배경 이미지를 적용한다. 크롭, 와이드 2가지 형태 제공
 */


public class WallpaperActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{
    public static final int REQ_CODE_PICK_PICTURE = 0;
    public static final int REQ_CODE_TAKE_PICTURE = 1;
    public static final int REQ_MULTI_PREMISSION = 999;

    private static final int HANDLE_START_PROGRESS = 0;
    private static final int HANDLE_STOP_PROGRESS = 1;

    //관련 퍼미션 확인
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    private String mTakePicPath = null;
    private String mSelectedFilePath = null;
    private Bitmap mSelectedBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSelectedBitmap = null;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_out);
    }

    private void init() {
        initView();
        initEvent();
        checkPermissions();
    }

    private void initView() {
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) //카메라 지원 여부에 따라 버튼 활성화
            findViewById(R.id.camera_btn).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.camera_btn).setVisibility(View.GONE);

        BitmapDrawable drawable = LauncherModel.getBackgroundDrawable();

        if (drawable != null){ //이미 지정된 배경이 있다면 그린다.
            WideBackgroundLayout wideBackgroundLayout = findViewById(R.id.selected_img);
            final Bitmap bitmap = drawable.getBitmap();
            if(bitmap != null) {
                //배경 이미지에 대한 모드를 계산하기 위한 과정
                final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                final float bitmapRatio = (float) bitmap.getWidth() / bitmap.getHeight();
                final float screenRatio = (float) displayMetrics.widthPixels /displayMetrics.heightPixels;

                int mode = WideBackgroundLayout.MODE_CROP;

                if (bitmapRatio > screenRatio)
                    mode = WideBackgroundLayout.MODE_RATIO;

                wideBackgroundLayout.setBackground(mode, drawable);
            }
        }
    }

    private void initEvent() {
        MenuButton cameraBtn = findViewById(R.id.camera_btn);
        cameraBtn.setDrawable((BitmapDrawable) AppCompatResources.getDrawable(getApplicationContext(), R.drawable.btn_camera));
        cameraBtn .setOnClickListener(this);

        MenuButton galleryBtn = findViewById(R.id.gallery_btn);
        galleryBtn.setDrawable((BitmapDrawable) AppCompatResources.getDrawable(getApplicationContext(), R.drawable.btn_gallery));
        galleryBtn .setOnClickListener(this);

        MenuButton clearBtn = findViewById(R.id.clear_btn);
        clearBtn.setDrawable((BitmapDrawable) AppCompatResources.getDrawable(getApplicationContext(), R.drawable.btn_delete));
        clearBtn .setOnClickListener(this);

        findViewById(R.id.done_btn).setOnClickListener(this);
        findViewById(R.id.cancel_btn).setOnClickListener(this);

        ((RadioGroup)findViewById(R.id.option_layout)).setOnCheckedChangeListener(this);
        ((RadioGroup)findViewById(R.id.option_layout)).check(R.id.radio_group_crop);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_MULTI_PREMISSION: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[0])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();
                            }
                        } else if (permissions[i].equals(this.permissions[1])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();

                            }
                        } else if (permissions[i].equals(this.permissions[2])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();

                            }
                        }
                    }
                } else {
                    showNoPermissionToastAndFinish();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "onActivityResult : " + requestCode + ", resultCod" + resultCode);

        mHandler.sendEmptyMessage(HANDLE_STOP_PROGRESS);

        if (requestCode == REQ_CODE_PICK_PICTURE || requestCode == REQ_CODE_TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) { //사진을 정상적으로 가져왔다면
                if(requestCode == REQ_CODE_TAKE_PICTURE){ //카메라에서 부터
                	mSelectedFilePath = mTakePicPath;
                	
                	File file = null;
                	try{
                		file = new File(mSelectedFilePath);
                	}catch(Exception e){
                		e.printStackTrace();
                	}                	
                	
                	if(file != null && file.exists())
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                }
                else{ //갤러리인 경우
                	final Uri pickItem = data.getData();
                	mSelectedFilePath = getRealPathFromURI(pickItem);
                }

                callImageCrop(); //결과에 대한 크롭화면 설정
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.camera_btn: //카메라 호출
                mHandler.sendEmptyMessage(HANDLE_START_PROGRESS);
                startCamera();
                break;
            case R.id.gallery_btn: //갤러리 호출
                mHandler.sendEmptyMessage(HANDLE_START_PROGRESS);
                startGallery();
                break;
            case R.id.clear_btn: //이미지 제거
                clearBg();
                break;
            case R.id.done_btn: //CROP 페이지에서 확인
                completeCropImage();
                break;
            case R.id.cancel_btn: //CROP 페이지에서 취소
                cancel();
                break;
        }
    }

    @Override
    //Crop 페이지에서 이미지를 CROP할지 WIDE로 할지 옵션
    //가로,세로 비가 화면 크기보다 가로가 더 긴경우에만 활성화 되는 옵션
    public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
        final AreaSelectView areaSelectView = findViewById(R.id.image_select);
        switch(id){
            case R.id.radio_group_crop:
                areaSelectView.setImageMode(AreaSelectView.CROP);
                break;
            case R.id.radio_group_wide:
                areaSelectView.setImageMode(AreaSelectView.WIDE);
                break;
        }
    }

    //이미지 Crop 뷰를 호출
    private void callImageCrop(){
        if(mSelectedFilePath != null) {
            findViewById(R.id.image_editing_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.btns_layout).setVisibility(View.INVISIBLE);

            AreaSelectView areaSelectView = findViewById(R.id.image_select); //크롭하는 이미지 뷰를 호출
            boolean isWideSupported = areaSelectView.setImageFile(mSelectedFilePath);

            if(!isWideSupported)
                findViewById(R.id.option_layout).setVisibility(View.GONE);
            else {
                ((RadioGroup)findViewById(R.id.option_layout)).check(R.id.radio_group_crop);
                findViewById(R.id.option_layout).setVisibility(View.VISIBLE);
            }
        }
    }

    //배경 삭제
    private void clearBg(){
        if(mSelectedBitmap != null && !mSelectedBitmap.isRecycled())
            mSelectedBitmap.recycle();

        mSelectedBitmap = null;

        WideBackgroundLayout wideBackgroundLayout = findViewById(R.id.selected_img); //현재 뷰에서 삭제
        wideBackgroundLayout.setBackground(WideBackgroundLayout.MODE_WIDE, null);

        LauncherModel.setBackgroundDrawable(getApplicationContext(), null); //모델에서도 삭제
    }

    //Crop페이지 완료 후 이미지 저장 및 갱신
    private void completeCropImage(){
        if(mSelectedBitmap != null && !mSelectedBitmap.isRecycled())
            mSelectedBitmap.recycle();

        AreaSelectView areaSelectView = findViewById(R.id.image_select);
        mSelectedBitmap = areaSelectView.getSelectBitmap();

        if(mSelectedBitmap != null) {
            BitmapDrawable drawable = new BitmapDrawable(getResources(), mSelectedBitmap);

            WideBackgroundLayout wideBackgroundLayout = findViewById(R.id.selected_img);
            wideBackgroundLayout.clear();

            LauncherModel.setBackgroundDrawable(getApplicationContext(), drawable); //모델에도 저장

            int mode = WideBackgroundLayout.MODE_CROP;
            if(areaSelectView.getMode() == AreaSelectView.WIDE)
                mode = WideBackgroundLayout.MODE_RATIO;

            wideBackgroundLayout.setBackground(mode, drawable);
        }
        else
            LauncherModel.setBackgroundDrawable(getApplicationContext(), null);

        findViewById(R.id.btns_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.image_editing_layout).setVisibility(View.INVISIBLE);
    }

    //취소
    private void cancel(){
        if(mSelectedBitmap != null && !mSelectedBitmap.isRecycled())
            mSelectedBitmap.recycle();

        mSelectedBitmap = null;

        findViewById(R.id.btns_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.image_editing_layout).setVisibility(View.INVISIBLE);
    }

    //퍼미션 확인
    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) { //사용자가 해당 권한을 가지고 있지 않을 경우 리스트에 해당 권한명 추가
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) { //권한이 추가되었으면 해당 리스트가 empty가 아니므로 request 즉 권한을 요청합니다.
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), REQ_MULTI_PREMISSION);
            return false;
        }
        return true;
    }

    private void showNoPermissionToastAndFinish() {
        Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    //갤러리 호출
    private void startGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_CODE_PICK_PICTURE);
    }

    //카메라 호출
    private void startCamera(){
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "Pictures";

        File fileFolder = new File(path);
        if(!fileFolder.exists())
            fileFolder.mkdir();

        SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        File fileName = new File(path + File.separator + fileFormat.format(new Date()) + ".jpg");
        mTakePicPath = fileName.getAbsolutePath();

        Uri outputFileUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            outputFileUri = FileProvider.getUriForFile(WallpaperActivity.this,"my.home.slauncher.provider", fileName);
        else
            outputFileUri = Uri.fromFile(fileName);

        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(intent, REQ_CODE_TAKE_PICTURE);
    }

    //Uri에 대한 실제 경로 획득
    private String getRealPathFromURI(Uri contentUri){
        final String[] proj = { MediaStore.Audio.Media.DATA };
        final Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        final int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private MsgHandler mHandler = new MsgHandler(this);
    private static class MsgHandler extends Handler{
        private WeakReference<WallpaperActivity> imageTransferActivityWeakReference;

        public MsgHandler(WallpaperActivity act){
            imageTransferActivityWeakReference = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            WallpaperActivity act = imageTransferActivityWeakReference.get();
            switch(msg.what){
                case HANDLE_START_PROGRESS:
                    act.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                    break;
                case HANDLE_STOP_PROGRESS:
                    act.findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    break;
            }
        }
    }

}
