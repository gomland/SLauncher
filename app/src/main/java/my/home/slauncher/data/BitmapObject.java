package my.home.slauncher.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by ShinSung on 2017-09-27.
 */

public class BitmapObject implements Serializable, Parcelable{
    private static final long serialUID = -395802398509L;
    private Bitmap mBitmap;
    private int mMode;

    public BitmapObject(Bitmap bitmap){
        mBitmap = bitmap;
    }

    protected BitmapObject(Parcel in) {
        mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        mMode = in.readInt();
    }

    public static final Creator<BitmapObject> CREATOR = new Creator<BitmapObject>() {
        @Override
        public BitmapObject createFromParcel(Parcel in) {
            return new BitmapObject(in);
        }

        @Override
        public BitmapObject[] newArray(int size) {
            return new BitmapObject[size];
        }
    };

    public Bitmap getBitmap(){
        return mBitmap;
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mMode) {
        this.mMode = mMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mBitmap, i);
        parcel.writeInt(mMode);
    }
}
