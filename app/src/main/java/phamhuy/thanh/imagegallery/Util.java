package phamhuy.thanh.imagegallery;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Truong on 27/6/2016.
 */
public class Util {
    static String TAG = "LEGEND";

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }


    public static int uploadFile(String filepath, String upLoadServerUrl) {

        String fileName = filepath;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(filepath);
        int serverResponseCode = 0;
        if (!sourceFile.isFile()) {
            Log.d(TAG, "File not found");
            return 0;

        } else {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUrl);
                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("PUT");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Connection", "Keep-Alive");
                String imageType = filepath.toUpperCase();
               // boolean matchJPG = imageType.matches("JPG$");
                //boolean matchPNG = imageType.matches("PNG$");

                if (imageType.contains(".JPG")) {
                    conn.setRequestProperty("Content-Type", "image/jpeg");
                } else if (imageType.contains(".PNG")) {
                    conn.setRequestProperty("Content-Type", "image/png");
                } else {
                    fileInputStream.close();
                    Log.d(TAG, "Cannot upload image from source URI: " + filepath);
                    return 0;
                }


                dos = new DataOutputStream(conn.getOutputStream());
                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                Log.d(TAG, "Data is available: " + bytesAvailable);
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.d(TAG, "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {
                    Log.d(TAG, "Upload successfully");
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                Log.e(TAG, "Cannot upload file to server: " + ex.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Upload file to server Exception: " + e.getMessage());
            }
            return serverResponseCode;

        } // End else block
    }

    public static List<String> getListURLUpload(List<String> listNames) {
        List<String> listUrls = new ArrayList<>();
        String params = "";
        if (listNames.size() > 0) {
            for (String s : listNames) {
                params = params + s + ",";
            }
            params = params.substring(0, params.length() - 1);
            Log.d(TAG, "List params : " + params);
        } else {
            return listUrls;
        }
        try {
            URL url = new URL("http://web.dev.fshare.vn/test-image?action=upload&bucket=user_test&filename=" + params);
            Log.d(TAG, "URL is : " + url);

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
            JSONArray jsonArray = object.getJSONArray("images");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonImage = jsonArray.getJSONObject(i);
                String url_upload = jsonImage.getString("url");
                Log.d(TAG, "URL for upload " + url_upload);
                listUrls.add(url_upload);
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception occurred");
        }
        return listUrls;
    }
}
