package phamhuy.thanh.imagegallery;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ImageService extends Service {
    private static String TAG = "LEGEND";
    List<Pair<Uri, Boolean>> mImageList;

    private final IBinder binder = new ImageBinder();

    public class ImageBinder extends Binder {
        public ImageService getService()  {
            return ImageService.this;
        }
    }

    public ImageService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mImageList = new ArrayList<>();
        Log.i(TAG, "Service onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"Service onBind");
        return this.binder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "Service onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Service onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        mImageList.clear();
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
    }

    public List<Pair<Uri, Boolean>> getmImageList() {
        return mImageList;
    }

    public void setmImageList(List<Pair<Uri, Boolean>> mImageList) {
        this.mImageList = mImageList;
    }
}
