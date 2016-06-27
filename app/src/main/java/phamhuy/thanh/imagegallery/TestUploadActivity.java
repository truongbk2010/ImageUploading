package phamhuy.thanh.imagegallery;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class TestUploadActivity extends AppCompatActivity {
    static String TAG = "LEGEND";
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_upload);
        button = (Button) findViewById(R.id.button);
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                //Query short theo DATE_ADD, DATE_TAKEN, DATE_MODIFY
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );
        cursor.moveToFirst();
        final String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        File file = new File(imagePath);
        final String strFileName = file.getName();
        Log.d(TAG, "File name : " + strFileName);
        String md5 = Util.fileToMD5(imagePath);
        Log.d(TAG, "MD5 tag : " + md5);
        //Get url for uploadding
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    URL url = new URL("http://web.dev.fshare.vn/test-image?action=upload&bucket=user_test&filename=" + strFileName);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    String content = sb.toString();
                    JSONObject object = new JSONObject(content);
                    JSONObject one_image = object.getJSONArray("images").getJSONObject(0);
                    String url_upload = one_image.getString("url");
                    Log.d(TAG, "Uploading ... to " + url_upload);
//                    ----------
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    Log.d(TAG, "Bitmap length: " + bitmap.getByteCount());
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    //compress the image to jpg format
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                    /*
                    * encode image to base64 so that it can be picked by saveImage.php file
                    * */
                    String encodeImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(),Base64.DEFAULT);
                    try{
                        String response = Util.postData(url_upload,encodeImage);

                    }catch (Exception e){
                        e.printStackTrace();
                        Log.e(TAG,"ERROR  "+e);
                        return null;
                    }

                } catch (Exception e) {
                    Log.d(TAG, "Exception occurred");

                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }


            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }

        }.execute();


    }


    private String hashMapToUrl(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public void uploadAction(View v) {
        Log.d(TAG, "Upload image to server");

    }
}
