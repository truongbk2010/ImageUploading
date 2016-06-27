package phamhuy.thanh.imagegallery;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by User on 5/16/2016.
 */
public class ScalableListView extends ListView {
    private OuterImageListAdapter mAdapter;
    private ScaleGestureDetector SGD;
    private float mScaleRatio;
    private final float MAX_SCALE_RATIO = 4.0f;
    private final float MIN_SCALE_RATIO = 0.25f;
    private boolean mScalable = true;
    private boolean mZoomable = true;

    public ScalableListView(Context context) {
        super(context);
    }

    public ScalableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScalableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(mZoomable){
            SGD.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setAdapter( ListAdapter adapter) {
        super.setAdapter(adapter);
        mScaleRatio = 1.0f;
        SGD = new ScaleGestureDetector(getContext(),new ScaleListener());
        mAdapter = (OuterImageListAdapter)adapter;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if( mScalable){
                mScaleRatio *= detector.getScaleFactor();
                mScaleRatio = Math.max(MIN_SCALE_RATIO, Math.min(mScaleRatio, MAX_SCALE_RATIO));
                if( mAdapter != null){

                    if(mAdapter.doScale(mScaleRatio )){
                        mScalable = false;
                        mAdapter.endScale();
                        //mScaleRatio = 1.0f;
//                    mNetworkImagesAdapter.endScale();
                    }
                    else
                    {
                        Log.d("Cannot Scale", "!");
                    }
                }

            }
            return true;

            //Toast.makeText(getContext(), "Scale Ratio:" + mScaleRatio, Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //Toast.makeText(MasterGalleryActivity.this, "Scale End:" + scale, Toast.LENGTH_SHORT).show();
            mScaleRatio = 1.0f;
            mAdapter.endScale();
            mScalable = true;
        }

    }


    public boolean ismZoomable() {
        return mZoomable;
    }

    public void setZoomable(boolean zoomable) {
        this.mZoomable = zoomable;
    }
}
