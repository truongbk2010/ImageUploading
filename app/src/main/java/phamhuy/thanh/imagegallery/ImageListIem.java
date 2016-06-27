package phamhuy.thanh.imagegallery;

import android.net.Uri;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 5/20/2016.
 */
public class ImageListIem {
    String mDate;
    List<Pair<Uri, Boolean>> mImageList;

    public ImageListIem(){
        mDate = "";
        mImageList = new ArrayList<>();
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String mDate) {
        this.mDate = mDate;
    }

    public List<Pair<Uri, Boolean>> getImageList() {
        return mImageList;
    }

    public void setImageList(List<Pair<Uri, Boolean>> mImageList) {
        this.mImageList = mImageList;
    }

    public void addImage( Uri imagePath, boolean isLocal){
        this.mImageList.add(new Pair<>(imagePath,isLocal));
    }
}
