package com.example.slartibartfast.mospho.Adapters;

/**
 * Created by slartibartfast on 11/19/16.
 */

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

//The adapter class associated with  class
public class ImageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Bitmap> imageChunks;
    private int imageWidth, imageHeight;

    //constructor
    public ImageAdapter(Context c, ArrayList<Bitmap> images) {
        mContext = c;
        imageChunks = images;
        imageWidth = images.get(0).getWidth();
        imageHeight = images.get(0).getHeight();
    }

    @Override
    public int getCount() {
        return imageChunks.size();
    }

    @Override
    public Object getItem(int position) {
        return imageChunks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView image;
        if (convertView == null) {
            image = new ImageView(mContext);

			/*
             * NOTE: I have set imageWidth - 10 and imageHeight
			 * as arguments to LayoutParams class.
			 * But you can take anything as per your requirement
			 */
            image.setLayoutParams(new GridView.LayoutParams(imageWidth, imageHeight));
            image.setPadding(0, 0, 0, 0);
        } else {
            image = (ImageView) convertView;
        }
        image.setImageBitmap(imageChunks.get(position));
        return image;
    }
}
