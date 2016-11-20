package com.example.slartibartfast.mospho.Utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
     */
    public static String getAverageDominantColourFromBitmap(Bitmap image) {

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

        red = red / numPixels;
        green = green / numPixels;
        blue = blue / numPixels;
        String hex = String.format("%02x%02x%02x", red, green, blue);
        return hex;
    }

    public static int getAverageIntDominantColourFromBitmap(Bitmap image) {

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

        red = red / numPixels;
        green = green / numPixels;
        blue = blue / numPixels;

        return getIntFromColor(red, green, blue);
    }

    //
    public static int getIntFromColor(int Red, int Green, int Blue) {
        Red = (Red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        Green = (Green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
        Blue = Blue & 0x000000FF; //Mask out anything not blue.

        return 0xFF000000 | Red | Green | Blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
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

    public static boolean isServerReachable(String serverURL, Context context) {

        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isConnected = false;
        NetworkInfo netInfo = connMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL urlServer = new URL(serverURL);
                HttpURLConnection urlConn = (HttpURLConnection) urlServer.openConnection();
                urlConn.setConnectTimeout(3000); //<- 3Seconds Timeout
                urlConn.connect();
                isConnected = true;
            } catch (MalformedURLException e1) {
                isConnected = false;
            } catch (IOException e) {
                isConnected = false;
            } catch (Exception e) {
                isConnected = false;
            }

        }
        return isConnected;
    }
}




