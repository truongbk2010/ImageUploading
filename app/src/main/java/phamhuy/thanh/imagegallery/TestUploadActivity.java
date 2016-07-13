package phamhuy.thanh.imagegallery;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

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
                    Util.uploadFile(imagePath, url_upload);

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


}
