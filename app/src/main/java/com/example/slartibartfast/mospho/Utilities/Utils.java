package com.example.slartibartfast.mospho.Utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;

/**
 * Created by slartibartfast on 11/18/16.
 */
public class Utils {

    /**
     * Calculate the Red, Green and Blue pixel values of a given image.
     * Get the total value of each of R, G, B and divide each by the
     * total number of pixels.
     *
     * @param image is a Bitmap object.
     * @return ArrayList<R, G, B> with each of R,G,B being Integers.
     */
    public ArrayList<Integer> getAverageDominantColourFromBitmap(Bitmap image) {
        int width = image == null ? 0 : image.getWidth();
        int height = image == null ? 0 : image.getHeight();
        int numPixels = width * height;
        int red = 0;
        int green = 0;
        int blue = 0;

        int[] pixels = new int[numPixels];
        // Get all the pixels in the image and iterate over them
        // to find their R,G,B values.
        image.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int x = 0; x < numPixels; x++) {
            red += Color.red(pixels[x]);
            green += Color.green(pixels[x]);
            blue += Color.blue(pixels[x]);
        }

        ArrayList<Integer> average = new ArrayList<Integer>();
        average.add(red / numPixels);
        average.add(green / numPixels);
        average.add(blue / numPixels);
        // Return the RGB average of the image.
        return average;
    }

    /**
     * Check if there is any connectivity
     *
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }
}




