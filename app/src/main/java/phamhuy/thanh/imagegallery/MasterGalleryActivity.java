package phamhuy.thanh.imagegallery;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterGalleryActivity extends AppCompatActivity implements OuterImageListAdapter.OnItemClickedListener {
    String TAG = "LEGEND";
    OuterImageListAdapter mOuterImageListAdapter;
    List<ImageListIem> mImageGridList;
    List<Pair<Uri, Boolean>> mImageList;
    ExtendedViewPager mImagePager;
    //  ListView listView;
    ScalableListView listView;
    boolean isSelect = false;
    private float scale = 1f;
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    View zoomedThumbnail;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_gallery);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        new AsyncTask<Void, Void, Void>(){
            ProgressDialog progressDlg = null;
            @Override
            protected void onPreExecute()
            {
                progressDlg = ProgressDialog.show(MasterGalleryActivity.this, "Loading Data ...",
                        "", true);
            }

            @Override
            protected Void doInBackground(Void... params) {
                 /* Step 1: Get local images list */
                Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        null,
                        null,
                        null,
                        //Query short theo DATE_ADD, DATE_TAKEN, DATE_MODIFY
                        MediaStore.Images.Media.DATE_ADDED + " DESC"
                );


                mImageGridList = new ArrayList<>();
                mImageList = new ArrayList<>();
                Set<String> seen = new HashSet<String>();
                SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, d MMM yyyy");

                ImageListIem imageListIem = null;

                while (cursor.moveToNext()) {
                    String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    Uri imageUri = Uri.fromFile(new File(imagePath));
                    Log.d(TAG, "URI test: " + imageUri.getPath());

//                    mImageList.add(imageUri);

                    String date = displayDateFormat.format(1000 * cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)));
                    if (!seen.contains(date)) {  /* Another Date */
                        imageListIem = new ImageListIem();
                        imageListIem.setDate(date);
                        imageListIem.addImage(imageUri,true);
                        seen.add(date);
                        mImageGridList.add(imageListIem);
                    } else {                     /* Previous Date */
                        if (imageListIem != null) {
                            imageListIem.addImage(imageUri, true);
                        }
                    }
                }
                cursor.close();



        /* Step 2: Get remote image list */
                try {
                    String jsonString = "";
                    URL url = new URL("http://web.dev.fshare.vn/test-image?action=listimage&bucket=user_test");
                    URLConnection conn = url.openConnection();
                    //HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    //conn.setAllowUserInteraction(false);
                   // conn.setInstanceFollowRedirects(true);
                    //conn.setRequestMethod("GET");
                    //System.setProperty("http.keepAlive", "false");
                    conn.connect();

                    // Get the server response

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = null;

                    // Read Server Response
                    while((line = reader.readLine()) != null)
                    {
                        // Append server response in string
                        sb.append(line + "\n");
                    }

                    jsonString = sb.toString();

                    JSONObject imagesJson = new JSONObject(jsonString);

                    JSONArray imgJsonArray = imagesJson.getJSONArray("images");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

                    boolean found;
                    for( int idx = 0; idx < imgJsonArray.length(); idx++) {
                        JSONObject imgJson = imgJsonArray.getJSONObject(idx);
                        String imageUrl = imgJson.getString("url");
                        Uri imageUri = Uri.parse(imageUrl);
                        Date dateTime = dateFormat.parse(imgJson.getString("last_modified"));
                        Date displayDate = displayDateFormat.parse(displayDateFormat.format(dateTime));

                        found = false;
                        for (int i = 0; i < mImageGridList.size(); i++) {
                            Date compareDate = displayDateFormat.parse(mImageGridList.get(i).getDate());
                            if (displayDate.equals(compareDate)) {
                                mImageGridList.get(i).addImage(imageUri, false);
                                found = true;
                                break;
                            } else if (displayDate.getTime() > compareDate.getTime()) {
                                imageListIem = new ImageListIem();
                                imageListIem.setDate(displayDateFormat.format(displayDate));
                                imageListIem.addImage(imageUri, false);
                                // if(i == 0){
                                //  mImageGridList.add(0, imageListIem);
                                // } else {
                                mImageGridList.add(i, imageListIem);
                                // }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            imageListIem = new ImageListIem();
                            imageListIem.setDate(displayDateFormat.format(displayDate));
                            imageListIem.addImage(imageUri, false);
                            mImageGridList.add(imageListIem);
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Exception is occured: " + e.getMessage());
                }

                /* Step 3 Combine into image list */
                for( int i = 0; i < mImageGridList.size(); i++){
                    List<Pair<Uri, Boolean>> imgList = mImageGridList.get(i).getImageList();
                    for( int j = 0; j < imgList.size(); j++){
                        mImageList.add(imgList.get(j));
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                mOuterImageListAdapter = new OuterImageListAdapter(MasterGalleryActivity.this, mImageGridList);
                listView = (ScalableListView) findViewById(R.id.outer_list_image);
                listView.setAdapter(mOuterImageListAdapter);
                mOuterImageListAdapter.setOnItemClickedListener(MasterGalleryActivity.this);

                mImagePager = (ExtendedViewPager) findViewById(R.id.image_pager);
                TouchImageAdapter imagePagerAdapter = new TouchImageAdapter(MasterGalleryActivity.this, mImageList);
                mImagePager.setAdapter(imagePagerAdapter);

                mShortAnimationDuration = getResources().getInteger(android.R.integer.config_longAnimTime);

                if( progressDlg.isShowing()){
                    progressDlg.dismiss();
                }
            }
        }.execute();

    }

    Set<Integer> indexsSelect = new HashSet<>();
    Set<Uri> imageUriList = new HashSet<>();
    Set<ImageView> views = new HashSet<>();

    @Override
    public void onClick(int singleImageIndex, View thumbnail, View container) {
        if (!isSelect) {

            mImagePager.setCurrentItem(singleImageIndex);
            mImagePager.setVisibility(View.VISIBLE);
        } else {
            //disable scale
            ImageView temp = (ImageView) thumbnail;
            //temp.setImageResource(R.drawable.default_place_holder);
            if (!indexsSelect.contains(singleImageIndex)) {
                temp.setColorFilter(Color.argb(200, 230, 230, 255), PorterDuff.Mode.SRC_ATOP);
                temp.invalidate();
                indexsSelect.add(singleImageIndex);
                imageUriList.add(mImageList.get(singleImageIndex).first);
                Log.d(TAG, "Add item on " + singleImageIndex);
                views.add(temp);
                //

            } else {
                temp.clearColorFilter();
                temp.invalidate();
                indexsSelect.remove(singleImageIndex);
                imageUriList.remove(mImageList.get(singleImageIndex));
                Log.d(TAG, "Remove item on " + singleImageIndex);
                views.remove(temp);
            }
        }
        zoomedThumbnail = thumbnail;
    }


    @Override
    public void onBackPressed() {
        if (mImagePager.getVisibility() == View.VISIBLE) {
            mImagePager.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_layout, menu);
      //  menu.findItem(R.id.menu_local).setVisible(false);
//        if (!isSelect) {
//            menu.findItem(R.id.menu_select).setVisible(true);
//            menu.findItem(R.id.menu_done).setVisible(false);
//            menu.findItem(R.id.menu_upload).setVisible(false);
//        } else {
//            menu.findItem(R.id.menu_select).setVisible(false);
//            menu.findItem(R.id.menu_done).setVisible(true);
//            menu.findItem(R.id.menu_upload).setVisible(true);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_select:
//                isSelect = true;
//                listView.setZoomable(false);
//                invalidateOptionsMenu();
//                break;
//            case R.id.menu_done:
//                isSelect = false;
//                listView.setZoomable(true);
//                invalidateOptionsMenu();
//                clearEffect();
//                break;
//            case R.id.menu_upload:
//                //upload action
//                doUpload();
//                clearEffect();
//                isSelect = false;
//                listView.setZoomable(true);
//                invalidateOptionsMenu();
//                break;
//            case R.id.menu_net:
//                //Page loads image using api
//                /*Intent intent = new Intent(MasterGalleryActivity.this, NetworkImagesActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                startActivity(intent);*/
//                break;
//        }
        return true;
    }

    //upload image after selecting
    public void doUpload() {
        if (imageUriList != null) {
            Toast.makeText(MasterGalleryActivity.this, "Number of images selected " + imageUriList.size(), Toast.LENGTH_SHORT).show();
            for (Uri uri : imageUriList) {
                Log.d(TAG, "Upload to image has path: " + uri);
            }
        }
    }

    public void clearEffect() {
        for (ImageView temp : views) {
            temp.clearColorFilter();
            temp.invalidate();
        }
        indexsSelect.clear();
        imageUriList.clear();
        views.clear();

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
                    if( mIsLocal){
                        bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), mImageUri);
                    } else{
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
            }.initParams(MasterGalleryActivity.this, listImages.get(position)).execute((Void[]) null);


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
