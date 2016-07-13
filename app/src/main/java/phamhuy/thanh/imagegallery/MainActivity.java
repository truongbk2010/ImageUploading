package phamhuy.thanh.imagegallery;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static final int MANUAL_UPLOAD = 0;
    public static final int AUTO_UPLOAD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }

    public void manualClick(View view){
        Intent i = new Intent(this, MasterGalleryActivity.class);
        i.putExtra(MasterGalleryActivity.EXTRAS_UPLOAD_TYPE, MANUAL_UPLOAD);
        startActivity(i);
    }

    public void autoClick(View view){
        Intent i = new Intent(this, MasterGalleryActivity.class);
        i.putExtra(MasterGalleryActivity.EXTRAS_UPLOAD_TYPE, AUTO_UPLOAD);
        startActivity(i);
    }
}

