package phamhuy.thanh.imagegallery;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by User on 5/15/2016.
 */


public class OuterImageListAdapter extends BaseAdapter {
    Context mContext;
    List<ImageListIem> mImageGridList;
    int mColCnt = 5;
    private final int GRID_SPACE = 20;
    int mColWidth;
    Mode mMode;
    int mParentWidth;
    float mOldScaleRatio = 1.0f;
    OnItemClickedListener mOnItemClickedListener;
    ConcurrentHashMap[] mPreLoadedBitmapCache;
    Handler mPreLoadThreadHandler;


    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration;


    enum Mode {
        INIT,
        IDLE,
        SCALING_ON,
        SCALING_OFF,
    }

    interface OnItemClickedListener {
        public void onClick(int singleImageIndex, View thumbnail, View container);
    }

    public void setOnItemClickedListener(OnItemClickedListener onItemClickedListener) {
        mOnItemClickedListener = onItemClickedListener;
    }

    public OuterImageListAdapter(Context context, final List<ImageListIem> imageGridList) {
        mContext = context;
        mImageGridList = imageGridList;
        mMode = Mode.INIT;
    }

    @Override
    public int getCount() {
        return mImageGridList.size();
    }

    @Override
    public Object getItem(int position) {
        return mImageGridList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int listItemPosition, View convertView, final ViewGroup listViewParent) {
        final GridItemViewHolder gridItemViewHolder;
        if (convertView == null) {
            gridItemViewHolder = new GridItemViewHolder();
            convertView = LayoutInflater.from(mContext).
                    inflate(R.layout.outer_image_list, null);
            gridItemViewHolder.gridImgThumbnail = (NonScrollableGridView) convertView.
                    findViewById(R.id.grid_image_thumbnail);
            gridItemViewHolder.imageDate = (TextView) convertView.
                    findViewById(R.id.img_date);
        } else {
            gridItemViewHolder = (GridItemViewHolder) convertView.getTag();
        }


        gridItemViewHolder.imageDate.setText(mImageGridList.get(listItemPosition).getDate());
        ThumbnailGridAdapter thumbnailGridAdapter
                = new ThumbnailGridAdapter(mContext, mImageGridList.get(listItemPosition).getImageList());
        gridItemViewHolder.gridImgThumbnail.setAdapter(thumbnailGridAdapter);

        if (mMode == Mode.INIT) {
            mMode = Mode.IDLE;
            mParentWidth = listViewParent.getWidth();
            mColWidth = (int) ((mParentWidth - (mColCnt - 1) * GRID_SPACE) / (float) mColCnt);
            gridItemViewHolder.gridImgThumbnail.setColumnWidth(mColWidth);
            gridItemViewHolder.gridImgThumbnail.setNumColumns(mColCnt);

        } else if (mMode == Mode.SCALING_OFF) {
            mMode = Mode.IDLE;
            mColCnt = (int) ((GRID_SPACE + mParentWidth) / (float) (GRID_SPACE + mColWidth));
            mColWidth = (int) ((mParentWidth - (mColCnt - 1) * GRID_SPACE) / (float) mColCnt);
            gridItemViewHolder.gridImgThumbnail.setColumnWidth(mColWidth);
            gridItemViewHolder.gridImgThumbnail.setNumColumns(mColCnt);
        } else if (mMode == Mode.SCALING_ON) {
            gridItemViewHolder.gridImgThumbnail.setColumnWidth(mColWidth);
            gridItemViewHolder.gridImgThumbnail.setNumColumns(mColCnt);
        } else { // mMode == Mode.IDLE
            gridItemViewHolder.gridImgThumbnail.setColumnWidth(mColWidth);
            gridItemViewHolder.gridImgThumbnail.setNumColumns(mColCnt);
        }

        gridItemViewHolder.gridImgThumbnail.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> gridViewParent, View view, int gridItemPosition, long id) {
                //Toast.makeText(mContext, "Clicked: " + position, Toast.LENGTH_SHORT).show();
                int singleImageIndex = 0;
                for( int i = 0; i < listItemPosition; i++){
                    singleImageIndex += mImageGridList.get(i).getImageList().size();
                }
                singleImageIndex += gridItemPosition;

                mOnItemClickedListener.onClick(singleImageIndex, view, listViewParent );

            }
        });
        convertView.setTag(gridItemViewHolder);
        return convertView;
    }

    public class GridItemViewHolder {
        TextView imageDate;
        NonScrollableGridView gridImgThumbnail;
    }

    public boolean doScale(float scaleRatio) {
        boolean stopScale = false;
        mMode = Mode.SCALING_ON;
        int newColWidth = (int) ((float) mColWidth */* Math.sqrt(Math.sqrt(*/scaleRatio/*))*/);

        if (newColWidth * 5 + 4 * GRID_SPACE <= mParentWidth) {
            mColCnt = 5;
            mColWidth = (int) ((mParentWidth - (mColCnt - 1) * GRID_SPACE) / (float) mColCnt);
            mMode = Mode.IDLE;
            mOldScaleRatio = scaleRatio;
        } else if (newColWidth * 2 + GRID_SPACE >= mParentWidth) {
            mColCnt = 2;
            mColWidth = (int) ((mParentWidth - (mColCnt - 1) * GRID_SPACE) / (float) mColCnt);
            mMode = Mode.IDLE;
            mOldScaleRatio = scaleRatio;
        } else {
            int oldColCnt = mColCnt;
            int newColCnt = (int) ((GRID_SPACE + mParentWidth) / (float) (GRID_SPACE + newColWidth));
            if (mOldScaleRatio > scaleRatio && (oldColCnt != newColCnt)) {
                mColWidth = newColWidth;
                mOldScaleRatio = 1.0f;
                stopScale = true;
            } else if (mOldScaleRatio < scaleRatio && (oldColCnt - 1 != newColCnt)) {
                mOldScaleRatio = 1.0f;
                stopScale = true;
            } else {
                mColWidth = newColWidth;
                mOldScaleRatio = scaleRatio;
            }
        }

        this.notifyDataSetChanged();
        return stopScale;

    }

    public void endScale() {
        mMode = Mode.SCALING_OFF;
        this.notifyDataSetChanged();
    }


    public class ThumbnailGridAdapter extends BaseAdapter {
        List<Pair<Uri, Boolean>> mImageList;
        Context mContext;

        public ThumbnailGridAdapter(Context context, List<Pair<Uri, Boolean>> imageList) {
            mContext = context;
            mImageList = imageList;
        }

        @Override
        public int getCount() {
            return mImageList.size();
        }

        @Override
        public Object getItem(int position) {
            return mImageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ThumbnailViewHolder thumbnailViewHolder;
            if( convertView ==  null){
                thumbnailViewHolder = new ThumbnailViewHolder();
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.grid_img_thumbnail_item, parent, false);
                thumbnailViewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.image_thumbnail);
                thumbnailViewHolder.cloud = (ImageView)convertView.findViewById(R.id.image_cloud);
              //  thumbnailViewHolder.thumbnail = (GaleryImageView) convertView.findViewById(R.id.image_thumbnail);
                convertView.setTag(thumbnailViewHolder);
            } else{
                thumbnailViewHolder = (ThumbnailViewHolder)convertView.getTag();
            }
            thumbnailViewHolder.thumbnail.getLayoutParams().height = mColWidth;
            thumbnailViewHolder.thumbnail.getLayoutParams().width = mColWidth;
            //thumbnailViewHolder.thumbnail.setBackgroundResource(R.drawable.default_place_holder);
            //Uri uri = Uri.fromFile(new File(mImageList.get(position)));
            Picasso.with(mContext).load(mImageList.get(position).first).resize(256, 256).centerCrop().placeholder(R.drawable.default_place_holder).into(thumbnailViewHolder.thumbnail);
            if(mImageList.get(position).second == true){
                thumbnailViewHolder.cloud.setVisibility(View.GONE);
            } else{
                thumbnailViewHolder.cloud.setVisibility(View.VISIBLE);
            }

            return convertView;
        }

        public class ThumbnailViewHolder {
            ImageView thumbnail;
            ImageView cloud;
        }
    }


}
