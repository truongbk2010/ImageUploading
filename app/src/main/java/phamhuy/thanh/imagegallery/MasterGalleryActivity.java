package phamhuy.thanh.imagegallery;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

    public static final String EXTRAS_UPLOAD_TYPE = "EXTRAS_UPLOAD_TYPE";

    String TAG = "LEGEND";
    OuterImageListAdapter mOuterImageListAdapter;
    List<ImageListIem> mImageGridList;
    public static List<Pair<Uri, Boolean>> mImageList;
    //ExtendedViewPager mImagePager;
   // TouchImageAdapter imagePagerAdapter;
    //  ListView listView;
    ScalableListView listView;
    boolean isSelect = false;
    private float scale = 1f;
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    View zoomedThumbnail;
    //service
    private boolean binded=false;
    private ImageService imageService;
    ServiceConnection imageServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ImageService.ImageBinder binder = (ImageService.ImageBinder) service;
            imageService = binder.getService();
            binded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binded = false;
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_gallery);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = this.getIntent();
        int type = intent.getIntExtra(EXTRAS_UPLOAD_TYPE, 0);
        if (type == MainActivity.MANUAL_UPLOAD) {
            new AsyncTask<Void, Void, Void>() {
                ProgressDialog progressDlg = null;

                @Override
                protected void onPreExecute() {
                    progressDlg = ProgressDialog.show(MasterGalleryActivity.this, "Loading Data ...",
                            "", true);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    mImageGridList = new ArrayList<>();
                    mImageList = new ArrayList<>();
                    Set<String> seen = new HashSet<String>();
                    SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
                    ImageListIem imageListIem = null;
                    //checksum
                    List<String> listChecksum = new ArrayList<String>();
                    //Step 0: Prepare get checksum on server
                    try {
                        String jsonString = "";
                        URL url = new URL("http://web.dev.fshare.vn/test-image?action=listimage&bucket=user_test");
                        URLConnection conn = url.openConnection();
                        conn.connect();
                        // Get the server response
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        // Read Server Response
                        while ((line = reader.readLine()) != null) {
                            // Append server response in string
                            sb.append(line + "\n");
                        }

                        jsonString = sb.toString();
                        JSONObject imagesJson = new JSONObject(jsonString);
                        JSONArray imgJsonArray = imagesJson.getJSONArray("images");

                        for (int idx = 0; idx < imgJsonArray.length(); idx++) {
                            JSONObject imgJson = imgJsonArray.getJSONObject(idx);
                            String checksumStr = imgJson.getString("e_tag").toUpperCase();
                            listChecksum.add(checksumStr);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "Exception is occured: " + e.getMessage());
                    }

                 /* Step 1: Get local images list */
                    Cursor cursor = getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            null,
                            null,
                            null,
                            //Query short by DATE_ADD, DATE_TAKEN, DATE_MODIFY
                            MediaStore.Images.Media.DATE_ADDED + " DESC"
                    );

                    while (cursor.moveToNext()) {
                        String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        Uri imageUri = Uri.fromFile(new File(imagePath));
                        String calChecksum = Util.fileToMD5(imageUri.getPath());
                        if(listChecksum.contains(calChecksum)){
                            //do nothing
                            continue;
                        }
                        String date = displayDateFormat.format(1000 * cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)));

                        if (!seen.contains(date)) {  /* Another Date */
                            imageListIem = new ImageListIem();
                            imageListIem.setDate(date);
                            imageListIem.addImage(imageUri, true);
                            //Log.d(TAG, "URI test: " + imagePath);
                            seen.add(date);
                            mImageGridList.add(imageListIem);
                        } else {                     /* Previous Date */
                            if (imageListIem != null) {
                                imageListIem.addImage(imageUri, true);
                              //  Log.d(TAG, "URI test: " + imagePath);
                            }
                        }
                    }
                    cursor.close();



                /* Step 2: Get remote image list */
                    try {
                        String jsonString = "";
                        URL url = new URL("http://web.dev.fshare.vn/test-image?action=listimage&bucket=user_test");
                        URLConnection conn = url.openConnection();
                        conn.connect();

                        // Get the server response
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        // Read Server Response
                        while ((line = reader.readLine()) != null) {
                            // Append server response in string
                            sb.append(line + "\n");
                        }

                        jsonString = sb.toString();

                        JSONObject imagesJson = new JSONObject(jsonString);

                        JSONArray imgJsonArray = imagesJson.getJSONArray("images");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

                        boolean found;
                        for (int idx = 0; idx < imgJsonArray.length(); idx++) {
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
                                    mImageGridList.add(i, imageListIem);
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

                    for (int i = 0; i < mImageGridList.size(); i++) {
                        List<Pair<Uri, Boolean>> imgList = mImageGridList.get(i).getImageList();
                        for (int j = 0; j < imgList.size(); j++) {
                            mImageList.add(imgList.get(j));
                        }
                    }
                    //imageService.setmImageList(mImageList);
                    ImageSingleton.getInstance().setImageUriList(mImageList);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {

                    mOuterImageListAdapter = new OuterImageListAdapter(MasterGalleryActivity.this, mImageGridList);
                    listView = (ScalableListView) findViewById(R.id.outer_list_image);
                    listView.setAdapter(mOuterImageListAdapter);
                    mOuterImageListAdapter.setOnItemClickedListener(MasterGalleryActivity.this);

//                    mImagePager = (ExtendedViewPager) findViewById(R.id.image_pager);
//                    imagePagerAdapter = new TouchImageAdapter(MasterGalleryActivity.this, mImageList);
//                    mImagePager.setAdapter(imagePagerAdapter);

                    mShortAnimationDuration = getResources().getInteger(android.R.integer.config_longAnimTime);

                    if (progressDlg.isShowing()) {
                        progressDlg.dismiss();
                    }
                }
            }.execute();
        } else {

        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MasterActivity start");
        Intent intent = new Intent(this, ImageService.class);
        this.bindService(intent, imageServiceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MasterActivity stop");
        if (binded) {
            this.unbindService(imageServiceConnection);
            binded = false;
        }
    }


    List<Integer> indexsSelect = new ArrayList<>();
    List<Uri> imageUriList = new ArrayList<>();
    Set<RelativeLayout> views = new HashSet<>();
    List<Pair<Uri, Boolean>> uploadImages = new ArrayList<>();

    @Override
    public void onClick(int singleImageIndex, View thumbnail, View container) {
        if (!isSelect) {
           // mImagePager.setCurrentItem(singleImageIndex);
           // mImagePager.setVisibility(View.VISIBLE);
            Intent i = new Intent(this, DetailPagerActivity.class);
            //i.putExtra(DetailPagerActivity.EXTRAS_LIST_IMAGES, mImageList);
           // i.putStringArrayListExtra(DetailPagerActivity.EXTRAS_LIST_IMAGES, mImageList)
            //i.putStringArrayListExtra(DetailPagerActivity.EXTRAS_LIST_IMAGES, uris);
            i.putExtra(DetailPagerActivity.EXTRAS_CURRENT_INDEX, singleImageIndex);
            //i.putExtra(DetailPagerActivity.EXTRAS_LIST_IMAGES, (Parcelable) mImageList);
            startActivity(i);


        } else {
            //disable scale

            RelativeLayout temp = (RelativeLayout) thumbnail;
            int width = temp.getWidth();
            int height = temp.getHeight();
            RelativeLayout.LayoutParams imParams =
                    new RelativeLayout.LayoutParams(width / 3, height / 3);
            ImageView imSex = new ImageView(this);
            imSex.setImageResource(R.drawable.ico_4);

            //temp.setImageResource(R.drawable.default_place_holder);
            if (!indexsSelect.contains(singleImageIndex)) {
                temp.addView(imSex, imParams);
                indexsSelect.add(singleImageIndex);
                imageUriList.add(mImageList.get(singleImageIndex).first);
                views.add(temp);
                uploadImages.add(mImageList.get(singleImageIndex));
                //
            } else {
                // temp.clearColorFilter();
                temp.removeViewAt(2);
                indexsSelect.remove(singleImageIndex);
                imageUriList.remove(mImageList.get(singleImageIndex).first);
                uploadImages.remove(mImageList.get(singleImageIndex));
                views.remove(temp);
            }
        }
        zoomedThumbnail = thumbnail;
    }


    @Override
    public void onBackPressed() {
//        if(mImagePager !=null){
//            if (mImagePager.getVisibility() == View.VISIBLE) {
//                mImagePager.setVisibility(View.GONE);
//            } else {
//                super.onBackPressed();
//            }
//        }else{
            super.onBackPressed();
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        // menu.findItem(R.id.menu_local).setVisible(false);
        if (!isSelect) {
            menu.findItem(R.id.menu_select).setVisible(true);
            menu.findItem(R.id.menu_done).setVisible(false);
            menu.findItem(R.id.menu_upload).setVisible(false);
        } else {
            menu.findItem(R.id.menu_select).setVisible(false);
            menu.findItem(R.id.menu_done).setVisible(true);
            menu.findItem(R.id.menu_upload).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select:
                isSelect = true;
                listView.setZoomable(false);
                invalidateOptionsMenu();
                break;
            case R.id.menu_done:
                isSelect = false;
                listView.setZoomable(true);
                invalidateOptionsMenu();
                clearEffect();
                break;
            case R.id.menu_upload:
                //upload action
                doUpload();
                //  clearEffect();
                isSelect = false;
                listView.setZoomable(true);
                invalidateOptionsMenu();
                break;
            // case R.id.menu_net:
            //Page loads image using api
                /*Intent intent = new Intent(MasterGalleryActivity.this, NetworkImagesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);*/
            // break;
        }
        return true;
    }

    //upload image after selecting
    public void doUpload() {
        if (imageUriList != null && imageUriList.size() > 0) {
            Toast.makeText(MasterGalleryActivity.this, "Number of images selected " + imageUriList.size(), Toast.LENGTH_SHORT).show();

            new AsyncTask<Void, Integer, Void>() {
                ProgressDialog progressDlg = null;
                int numberOfImages = 0;
                List<Uri> uploadedUri = new ArrayList<Uri>();
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progressDlg = ProgressDialog.show(MasterGalleryActivity.this, "Uploading image",
                            "", true);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    List<String> pathUploadList = new ArrayList<>();
                    List<String> imageNames = new ArrayList<>();
                    List<String> urlList = new ArrayList<>();
                    numberOfImages = imageUriList.size();

                    for (Uri uri : imageUriList) {
                        if (URLUtil.isFileUrl(uri.toString())) {
                            String imagePath = uri.getPath();
                            pathUploadList.add(imagePath);
                            //Log.d(TAG, "Path : " + imagePath);
                            File file = new File(imagePath);
                            String strFileName = file.getName();
                            imageNames.add(strFileName);
                           // Log.d(TAG, "Name : " + strFileName);
                            uploadedUri.add(uri);
                        }
                    }
                    urlList = Util.getListURLUpload(imageNames);
                    for (int i = 0; i < pathUploadList.size(); i++) {
                        String filePath = pathUploadList.get(i);
                        String url = urlList.get(i);
                        Util.uploadFile(filePath, url);
                        publishProgress(i + 1);
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    super.onProgressUpdate(values);
                    Log.d(TAG, "Uploaded " + values[0] + " image");
                    progressDlg.setMessage("Uploaded " + values[0] + "/" + numberOfImages + " images");

                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    Log.d(TAG, "Upload successfully");
                    progressDlg.setMessage("Upload successfully");
                    if (progressDlg.isShowing()) {
                        progressDlg.dismiss();
                    }
                    for (RelativeLayout temp : views) {
                        ImageView imageView= (ImageView) temp.getChildAt(1);
                        imageView.setVisibility(View.VISIBLE);
                    }

//                    if(mImageList != null && mImageList.size()>0){
//                        for(Pair<Uri, Boolean> pair : uploadImages){
//                            mImageList.remove(pair);
//                            mImageList.add(new Pair<Uri, Boolean>(pair.first, false));
//                        }
//                    }
//                    imagePagerAdapter.notifyDataSetChanged();

                    for (int i = 0; i < mImageGridList.size(); i++) {
                        List<Pair<Uri, Boolean>> imgList = mImageGridList.get(i).getImageList();
                        for(Pair<Uri, Boolean> pair : uploadImages){
                            if(imgList.contains(pair)){
                                imgList.remove(pair);
                                imgList.add(new Pair<Uri, Boolean>(pair.first, false));
                            }
                        }

                    }
                    mImageList.clear();
                    for (int i = 0; i < mImageGridList.size(); i++) {
                        List<Pair<Uri, Boolean>> imgList = mImageGridList.get(i).getImageList();
                        for (int j = 0; j < imgList.size(); j++) {
                            mImageList.add(imgList.get(j));
                        }
                    }
                    //imageService.setmImageList(mImageList);
                    ImageSingleton.getInstance().setImageUriList(mImageList);

                    mOuterImageListAdapter.notifyDataSetChanged();
                    clearEffect();
                }
            }.execute();
        }
    }

    public void clearEffect() {
        for (RelativeLayout temp : views) {
            temp.removeViewAt(2);
        }
        indexsSelect.clear();
        imageUriList.clear();
        views.clear();
        uploadImages.clear();

    }



}
