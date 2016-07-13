package phamhuy.thanh.imagegallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DetailPagerActivity extends AppCompatActivity {
    public static final String TAG="LEGEND";
    public static final String EXTRAS_LIST_IMAGES = "EXTRAS_LIST_IMAGES";
    public static final String EXTRAS_CURRENT_INDEX = "EXTRAS_CURRENT_INDEX";
    ExtendedViewPager mImagePager;
    TouchImageAdapter imagePagerAdapter;
    List<Pair<Uri, Boolean>> mImageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detailtoolbar);
        setSupportActionBar(toolbar);

        mImageList = new ArrayList<>();

        Intent intent = this.getIntent();
        int index = intent.getIntExtra(EXTRAS_CURRENT_INDEX, 0);
        Log.d(TAG, " Index image : " + index);
        mImageList = ImageSingleton.getInstance().getImageUriList();
        Log.d(TAG, " Size of image list : " + mImageList.size());
        mImagePager = (ExtendedViewPager) findViewById(R.id.image_pager);
        imagePagerAdapter = new TouchImageAdapter(DetailPagerActivity.this, mImageList);
        mImagePager.setAdapter(imagePagerAdapter);
        mImagePager.setCurrentItem(index);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_select:
//        }
        return true;
    }




    class TouchImageAdapter extends PagerAdapter {
        Context mContext;
        List<Pair<Uri, Boolean>> listImages;


        public TouchImageAdapter(Context context, List<Pair<Uri, Boolean>> list) {
            listImages = list;
            mContext = context;
        }


        public List<Pair<Uri, Boolean>> getListImages() {
            return listImages;
        }

        public void setListImages(List<Pair<Uri, Boolean>> listImages) {
            this.listImages = listImages;
        }

        @Override
        public int getCount() {
            return listImages.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(mContext)
                    .inflate(R.layout.image_pager, container, false);
            final TouchImageView imageView = (TouchImageView) linearLayout.findViewById(R.id.single_image);
            final ProgressBar progressBar = (ProgressBar) linearLayout.findViewById(R.id.loading_progress_bar);

            new AsyncTask<Void, Void, Bitmap>() {
                Uri mImageUri;
                boolean mIsLocal;
                ImageView mImageView;
                Context mContext;

                public AsyncTask initParams(Context context, Pair<Uri, Boolean> imageInfo) {
                    mImageUri = imageInfo.first;
                    mIsLocal = imageInfo.second;
                    mContext = context;
                    return this;
                }

                @Override
                protected Bitmap doInBackground(Void... params) {
                    //Bitmap bitmap = BitmapFactory.decodeFile(mImagePath);
                    Bitmap bitmap = null;
                    try {
                        if (mIsLocal) {
                            bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), mImageUri);
                        } else {
                            URL url = new URL(mImageUri.toString());
                            bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return bitmap;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                    progressBar.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                }
            }.initParams(DetailPagerActivity.this, listImages.get(position)).execute((Void[]) null);


            container.addView(linearLayout);
            return linearLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }
}
