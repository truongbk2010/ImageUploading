package phamhuy.thanh.imagegallery;

import android.net.Uri;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Truong on 11/7/2016.
 */
public class ImageSingleton {
    private static ImageSingleton instance;
    private List<Pair<Uri, Boolean>> imageUriList = new ArrayList<>();

    public static void initInstance()
    {
        if (instance == null)
        {
            instance = new ImageSingleton();
        }
    }

    public static synchronized ImageSingleton getInstance()
    {
        if (instance == null)
        {
            // Create the instance
            instance = new ImageSingleton();
        }
        return instance;
    }

    private ImageSingleton()
    {
        // Constructor hidden because this is a singleton
    }

    public List<Pair<Uri, Boolean>> getImageUriList() {
        return imageUriList;
    }

    public void setImageUriList(List<Pair<Uri, Boolean>> imageUriList) {
        this.imageUriList = imageUriList;
    }
}
